package com.example.localllm

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.localllm.databinding.ActivityModelLibraryBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class ModelLibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelLibraryBinding
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var adapter: ModelAdapter

    // Tracks the currently downloading model filename (only one at a time)
    // Several downloads can run at once, so each one is tracked by file name
    // rather than there being a single "the download" that blocks the others.
    private val downloadJobs = mutableMapOf<String, Job>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("localllm_prefs", MODE_PRIVATE)

        // Same inset handling as the chat screen. Without it this top bar sits
        // underneath the status bar, which not only looks wrong but eats the
        // taps, so the Back button was drawn but not reachable.
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.rvModels) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = bars.bottom)
            insets
        }

        // There was no way back to the chat except the system gesture, and
        // that closed the app rather than returning, because this activity was
        // launched without a parent. finish() drops back to the chat.
        binding.btnBack.setOnClickListener { finish() }

        binding.btnKaggleAccount.setOnClickListener {
            showCredentialsDialog(onSaved = {
                Toast.makeText(this, "Credentials saved!", Toast.LENGTH_SHORT).show()
            })
        }

        setupModelList()
    }

    private fun setupModelList() {
        val activeModel = prefs.getString("active_model", "") ?: ""

        adapter = ModelAdapter(
            models = ModelLibrary.models,
            activeFileName = activeModel,
            externalFilesDir = getExternalFilesDir(null),
            onUseClicked = { model -> switchToModel(model) },
            onDownloadClicked = { model -> startDownload(model) },
            onLicenseClicked = { model ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(model.licenseUrl)))
            }
        )

        binding.rvModels.layoutManager = LinearLayoutManager(this)
        binding.rvModels.adapter = adapter
    }

    // ── Model switching ────────────────────────────────────────────────────────

    private fun switchToModel(model: ModelInfo) {
        prefs.edit().putString("active_model", model.fileName).apply()
        Toast.makeText(this, "Switched to ${model.displayName}", Toast.LENGTH_SHORT).show()
        // Update adapter so the active indicator moves to the new model
        adapter.activeFileName = model.fileName
        adapter.notifyDataSetChanged()
        // Go back to chat, onResume in MainActivity will reload the model
        finish()
    }

    // ── Download flow ──────────────────────────────────────────────────────────

    private fun startDownload(model: ModelInfo) {
        // Already downloading this one, nothing to do. Other models can
        // still be started while this runs.
        if (downloadJobs.containsKey(model.fileName)) return

        // Only the Gemma models live on Kaggle and need a login. The rest come
        // from Hugging Face and download straight away.
        val username = prefs.getString("kaggle_username", "") ?: ""
        val apiKey   = prefs.getString("kaggle_api_key",  "") ?: ""

        if (model.needsKaggle && (username.isEmpty() || apiKey.isEmpty())) {
            showCredentialsDialog(onSaved = { startDownload(model) })
            return
        }

        adapter.downloadProgress[model.fileName] = 0
        notifyModelChanged(model.fileName)

        downloadJobs[model.fileName] = lifecycleScope.launch {
            val result = KaggleDownloader.download(
                context  = this@ModelLibraryActivity,
                model    = model,
                username = username,
                apiKey   = apiKey,
                onProgress = { progress ->
                    // The downloader reports every 64KB, which for a 1GB file
                    // is thousands of callbacks. Redrawing the row that often
                    // is what made the Cancel button flicker, so only redraw
                    // when the whole percentage actually changes.
                    if (adapter.downloadProgress[model.fileName] != progress) {
                        runOnUiThread {
                            adapter.downloadProgress[model.fileName] = progress
                            notifyModelChanged(model.fileName)
                        }
                    }
                }
            )

            // Download finished (success or failure)
            downloadJobs.remove(model.fileName)
            adapter.downloadProgress.remove(model.fileName)
            adapter.notifyDataSetChanged()

            result.onSuccess {
                Toast.makeText(
                    this@ModelLibraryActivity,
                    "${model.displayName} downloaded!",
                    Toast.LENGTH_SHORT
                ).show()
                // Auto-switch to the newly downloaded model
                switchToModel(model)
            }
            result.onFailure { error ->
                AlertDialog.Builder(this@ModelLibraryActivity)
                    .setTitle("Download failed")
                    .setMessage(error.message ?: "Unknown error")
                    .setPositiveButton("Retry") { _, _ -> startDownload(model) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    /** Cancels one download and leaves any others running. */
    private fun cancelDownload(fileName: String) {
        downloadJobs.remove(fileName)?.cancel()
        adapter.downloadProgress.remove(fileName)
        notifyModelChanged(fileName)
    }

    /** Redraws one row instead of the whole list. */
    private fun notifyModelChanged(fileName: String) {
        val index = ModelLibrary.models.indexOfFirst { it.fileName == fileName }
        if (index != -1) adapter.notifyItemChanged(index)
    }

    // ── Kaggle credentials dialog ──────────────────────────────────────────────

    private fun showCredentialsDialog(onSaved: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_kaggle_credentials, null)
        val etUsername = dialogView.findViewById<EditText>(R.id.etKaggleUsername)
        val etApiKey   = dialogView.findViewById<EditText>(R.id.etKaggleApiKey)

        // Pre-fill if already saved
        etUsername.setText(prefs.getString("kaggle_username", ""))
        etApiKey.setText(prefs.getString("kaggle_api_key", ""))

        AlertDialog.Builder(this)
            .setTitle("Kaggle Account")
            .setMessage("Models are hosted on Kaggle. Enter your credentials to download directly.\n\nGet your API key at kaggle.com → Settings → API → Create New Token")
            .setView(dialogView)
            .setPositiveButton("Save & Download") { _, _ ->
                val username = etUsername.text.toString().trim()
                val apiKey   = etApiKey.text.toString().trim()
                if (username.isEmpty() || apiKey.isEmpty()) {
                    Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prefs.edit()
                    .putString("kaggle_username", username)
                    .putString("kaggle_api_key", apiKey)
                    .apply()
                onSaved()
            }
            .setNeutralButton("Open Kaggle Settings") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.kaggle.com/settings/account")))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Adds a "Kaggle Account" option to the top-right menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Kaggle Account")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            showCredentialsDialog(onSaved = {
                Toast.makeText(this, "Credentials updated", Toast.LENGTH_SHORT).show()
            })
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // ── Adapter ────────────────────────────────────────────────────────────────

    inner class ModelAdapter(
        private val models: List<ModelInfo>,
        var activeFileName: String,
        private val externalFilesDir: File?,
        private val onUseClicked: (ModelInfo) -> Unit,
        private val onDownloadClicked: (ModelInfo) -> Unit,
        private val onLicenseClicked: (ModelInfo) -> Unit
    ) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

        // progress value per fileName:  0–100 = download %,  -1 = extracting
        val downloadProgress = mutableMapOf<String, Int>()

        inner class ModelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName:             TextView    = view.findViewById(R.id.tvModelName)
            val tvDescription:      TextView    = view.findViewById(R.id.tvModelDescription)
            val tvSize:             TextView    = view.findViewById(R.id.tvModelSize)
            val tvRam:              TextView    = view.findViewById(R.id.tvModelRam)
            val tvDeviceTag:        TextView    = view.findViewById(R.id.tvDeviceTag)
            val tvLicense:          TextView    = view.findViewById(R.id.tvModelLicense)
            val tvStatus:           TextView    = view.findViewById(R.id.tvModelStatus)
            val tvDownloadProgress: TextView    = view.findViewById(R.id.tvDownloadProgress)
            val progressBar:        ProgressBar = view.findViewById(R.id.progressBarDownload)
            val btnUse:             Button      = view.findViewById(R.id.btnUseModel)
            val btnDownload:        Button      = view.findViewById(R.id.btnDownloadModel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_model, parent, false)
            return ModelViewHolder(view)
        }

        override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
            val model        = models[position]
            val isDownloaded = File(externalFilesDir, model.fileName).exists()
            val isActive     = model.fileName == activeFileName
            val isDownloading = adapter.downloadProgress.containsKey(model.fileName)
            val progress     = downloadProgress[model.fileName]

            // Static info
            holder.tvName.text        = model.displayName
            holder.tvDescription.text = model.description
            holder.tvSize.text        = "Storage: ${model.sizeLabel}"
            holder.tvRam.text         = "RAM: ${model.ramLabel}"
            holder.tvLicense.text     = "License: ${model.license}"

            // Device tag colour
            holder.tvDeviceTag.text = model.deviceTag
            holder.tvDeviceTag.setBackgroundColor(
                when (model.deviceTagColor) {
                    "green"  -> Color.parseColor("#2E7D32")
                    "orange" -> Color.parseColor("#E65100")
                    else     -> Color.parseColor("#B71C1C")
                }
            )

            // ── Download progress UI ──
            if (isDownloading && progress != null) {
                holder.tvDownloadProgress.visibility = View.VISIBLE
                holder.progressBar.visibility        = View.VISIBLE

                if (progress == -1) {
                    holder.tvDownloadProgress.text = "Extracting model..."
                    holder.progressBar.isIndeterminate = true
                } else {
                    holder.tvDownloadProgress.text = "Downloading $progress%"
                    holder.progressBar.isIndeterminate = false
                    holder.progressBar.progress = progress
                }

                holder.tvStatus.visibility = View.GONE
                holder.btnUse.isEnabled    = false
                holder.btnUse.text         = "Downloading..."
                holder.btnDownload.text    = "Cancel"
                holder.btnDownload.setOnClickListener { cancelDownload(model.fileName) }
                return
            } else {
                holder.tvDownloadProgress.visibility   = View.GONE
                holder.progressBar.visibility          = View.GONE
                holder.progressBar.isIndeterminate     = false
            }

            // ── Normal state UI ──
            when {
                isActive -> {
                    holder.tvStatus.text = "✓ Active"
                    holder.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                    holder.tvStatus.visibility = View.VISIBLE
                    holder.btnUse.isEnabled    = false
                    holder.btnUse.text         = "In Use"
                }
                isDownloaded -> {
                    holder.tvStatus.text = "Downloaded"
                    holder.tvStatus.setTextColor(Color.parseColor("#1976D2"))
                    holder.tvStatus.visibility = View.VISIBLE
                    holder.btnUse.isEnabled    = true
                    holder.btnUse.text         = "Switch to This Model"
                }
                else -> {
                    holder.tvStatus.visibility = View.GONE
                    holder.btnUse.isEnabled    = false
                    holder.btnUse.text         = "Not Downloaded"
                }
            }

            // Disable download button if a different model is already downloading
            // Downloads no longer block each other, so nothing is greyed out
            // just because something else is running.
            val anotherDownloading = false
            holder.btnDownload.isEnabled = !anotherDownloading
            holder.btnDownload.text = if (isDownloaded) "Re-download" else "Download"

            holder.btnUse.setOnClickListener      { onUseClicked(model) }
            holder.btnDownload.setOnClickListener  { onDownloadClicked(model) }
            holder.tvLicense.setOnClickListener    { onLicenseClicked(model) }
        }

        override fun getItemCount() = models.size
    }
}
