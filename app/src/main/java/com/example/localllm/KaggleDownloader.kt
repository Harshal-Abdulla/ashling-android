package com.example.localllm

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.zip.ZipInputStream

// KaggleDownloader handles:
//  1. Authenticating with the Kaggle API using username + API key
//  2. Downloading the model file with progress updates
//  3. Extracting the .bin from the downloaded archive (.zip, .tar, or .tar.gz)
//
// Think of it like a Python requests.get() + tarfile.extract() wrapped in one helper.

object KaggleDownloader {

    // Called from a coroutine, downloads and extracts the model, reporting progress along the way.
    // onProgress:  0–100 = download percentage,  -1 = "extracting now"
    suspend fun download(
        context: Context,
        model: ModelInfo,
        username: String,
        apiKey: String,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {

        val outputDir = context.getExternalFilesDir(null)
            ?: return@withContext Result.failure(Exception("Storage not available"))

        val outputFile = File(outputDir, model.fileName)
        val tempFile   = File(outputDir, "${model.fileName}.download.tmp")

        try {
            // Models with a directUrl come from Hugging Face and need no login,
            // so skip all the Kaggle credential work and just fetch the file.
            if (model.directUrl != null) {
                return@withContext downloadDirect(
                    model.directUrl, outputFile, tempFile, onProgress
                ) { isActive }
            }

            val credentials = Base64.getEncoder()
                .encodeToString("$username:$apiKey".toByteArray())

            // Try multiple URL formats, Kaggle's API path can vary by framework casing/version
            val base = "https://www.kaggle.com/api/v1/models"
            val path = model.kaggleApiPath
            val pathNoVersion = path.substringBeforeLast("/")
            val candidates = listOf(
                "$base/$path/download",
                "$base/$pathNoVersion/download",
                "$base/${path.replace("tflite", "tfLite")}/download",
                "$base/${pathNoVersion.replace("tflite", "tfLite")}/download"
            )

            var finalUrl: String? = null
            var lastCode = 0
            for (candidate in candidates) {
                val testConn = URL(candidate).openConnection() as HttpURLConnection
                testConn.setRequestProperty("Authorization", "Basic $credentials")
                testConn.instanceFollowRedirects = false
                testConn.connectTimeout = 15_000
                testConn.connect()
                lastCode = testConn.responseCode
                testConn.disconnect()
                if (lastCode in 200..399) {
                    finalUrl = resolveRedirects(candidate, credentials)
                    break
                }
            }
            if (finalUrl == null) {
                val reason = when (lastCode) {
                    401 -> "Wrong Kaggle username or API key."
                    403 -> "Access denied. Make sure you accepted the model license on kaggle.com."
                    404 -> "Model not found. The Kaggle path may have changed."
                    else -> "HTTP $lastCode"
                }
                return@withContext Result.failure(Exception(reason))
            }

            val connection = URL(finalUrl).openConnection() as HttpURLConnection
            // GCS signed URL doesn't need auth, but add it anyway, it's ignored safely
            connection.setRequestProperty("Authorization", "Basic $credentials")
            connection.connectTimeout = 15_000
            connection.readTimeout    = 60_000
            connection.connect()

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("Download failed: HTTP ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLengthLong

            // Stream straight to disk, never load 1.3 GB into memory
            connection.inputStream.buffered(64 * 1024).use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress((downloaded * 100 / totalBytes).toInt())
                        }
                    }
                }
            }
            connection.disconnect()

            // Signal that download is done and extraction is starting
            onProgress(-1)
            extractBin(tempFile, outputFile)
            tempFile.delete()

            Result.success(outputFile)

        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        }
    }

    // Follows HTTP redirects across domains (Kaggle → Google Cloud Storage)
    private fun resolveRedirects(startUrl: String, credentials: String): String {
        var url = startUrl
        repeat(5) {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Basic $credentials")
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15_000
            conn.connect()

            val code = conn.responseCode
            if (code in 300..399) {
                url = conn.getHeaderField("Location") ?: throw Exception("Redirect had no Location header")
                conn.disconnect()
            } else {
                conn.disconnect()
                return url
            }
        }
        throw Exception("Too many redirects")
    }

    // Detects the archive format by its magic bytes and extracts the .bin file
    private fun extractBin(archiveFile: File, outputFile: File) {
        val header = ByteArray(4)
        archiveFile.inputStream().use { it.read(header) }

        when {
            // ZIP: magic bytes are 'P' 'K'
            header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() ->
                extractFromZip(archiveFile, outputFile)

            // GZIP: magic bytes are 0x1F 0x8B
            header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte() ->
                extractFromTarGz(archiveFile, outputFile)

            // Otherwise assume plain TAR
            else ->
                extractFromTar(archiveFile, outputFile)
        }
    }

    private fun extractFromZip(archive: File, output: File) {
        ZipInputStream(BufferedInputStream(archive.inputStream())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".bin")) {
                    FileOutputStream(output).use { zis.copyTo(it) }
                    return
                }
                entry = zis.nextEntry
            }
        }
        throw Exception("No .bin file found inside the downloaded archive")
    }

    private fun extractFromTarGz(archive: File, output: File) {
        TarArchiveInputStream(
            GzipCompressorInputStream(BufferedInputStream(archive.inputStream()))
        ).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".bin")) {
                    FileOutputStream(output).use { tar.copyTo(it) }
                    return
                }
                entry = tar.nextTarEntry
            }
        }
        throw Exception("No .bin file found inside the downloaded archive")
    }

    private fun extractFromTar(archive: File, output: File) {
        TarArchiveInputStream(BufferedInputStream(archive.inputStream())).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".bin")) {
                    FileOutputStream(output).use { tar.copyTo(it) }
                    return
                }
                entry = tar.nextTarEntry
            }
        }
        throw Exception("No .bin file found inside the downloaded archive")
    }

    /**
     * Straight download, no authentication. Writes to a .tmp file first and
     * only renames it when the whole thing has arrived, so a download that
     * dies halfway through doesn't leave a broken file that looks complete.
     */
    private fun downloadDirect(
        url: String,
        outputFile: File,
        tempFile: File,
        onProgress: (Int) -> Unit,
        // Cancelling a coroutine doesn't interrupt a blocking read, so the loop
        // has to ask whether it should still be running. Without this, pressing
        // Cancel left the download going in the background and the file kept
        // growing.
        stillWanted: () -> Boolean
    ): Result<File> {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.connect()

            if (connection.responseCode !in 200..299) {
                return Result.failure(Exception("Download failed (HTTP ${connection.responseCode})"))
            }

            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var downloaded = 0L
                    var read = input.read(buffer)
                    while (read != -1) {
                        if (!stillWanted()) {
                            tempFile.delete()
                            return Result.failure(CancellationException("Download cancelled"))
                        }
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt())
                        read = input.read(buffer)
                    }
                }
            }

            if (outputFile.exists()) outputFile.delete()
            tempFile.renameTo(outputFile)
            Result.success(outputFile)
        } catch (e: Exception) {
            tempFile.delete()
            Result.failure(e)
        }
    }
}
