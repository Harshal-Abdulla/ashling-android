package com.example.localllm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.localllm.databinding.ActivityModelLibraryBinding
import java.io.File

class ModelLibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModelLibraryBinding
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Same edge-to-edge fix as MainActivity: without it the first model in
        // the list sits under the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.modelRoot)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        prefs = getSharedPreferences("localllm_prefs", MODE_PRIVATE)

        supportActionBar?.title = "Model Library"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupModelList()
    }

    private fun setupModelList() {
        val activeModel = prefs.getString("active_model", "") ?: ""

        binding.rvModels.layoutManager = LinearLayoutManager(this)
        binding.rvModels.adapter = ModelAdapter(
            models = ModelLibrary.models,
            activeFileName = activeModel,
            externalFilesDir = getExternalFilesDir(null),
            onUseClicked = { model ->
                // Save selected model to SharedPreferences
                prefs.edit().putString("active_model", model.fileName).apply()
                finish() // go back to chat screen — onResume will reload the model
            },
            onDownloadClicked = { model ->
                // Open Kaggle in browser with instructions
                showDownloadInstructions(model)
            },
            onLicenseClicked = { model ->
                // Open license URL in browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(model.licenseUrl))
                startActivity(intent)
            }
        )
    }

    private fun showDownloadInstructions(model: ModelInfo) {
        val adbCommand = "adb push ~/Downloads/${model.fileName} " +
                "/sdcard/Android/data/com.example.localllm/files/${model.fileName}"

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Download ${model.displayName}")
            .setMessage(
                "1. Go to kaggle.com/models/google/gemma\n" +
                "2. Select 'LiteRT' tab\n" +
                "3. Choose variation: ${model.kaggleVariation}\n" +
                "4. Download and extract the .tar.gz file\n\n" +
                "5. Run this command on your Mac:\n\n" +
                adbCommand
            )
            .setPositiveButton("Open Kaggle") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.kaggle.com/models/google/gemma/frameworks/tfLite"))
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // Adapter for the model list — same concept as ChatAdapter
    class ModelAdapter(
        private val models: List<ModelInfo>,
        private val activeFileName: String,
        private val externalFilesDir: File?,
        private val onUseClicked: (ModelInfo) -> Unit,
        private val onDownloadClicked: (ModelInfo) -> Unit,
        private val onLicenseClicked: (ModelInfo) -> Unit
    ) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

        class ModelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvModelName)
            val tvDescription: TextView = view.findViewById(R.id.tvModelDescription)
            val tvSize: TextView = view.findViewById(R.id.tvModelSize)
            val tvLicense: TextView = view.findViewById(R.id.tvModelLicense)
            val tvStatus: TextView = view.findViewById(R.id.tvModelStatus)
            val btnUse: Button = view.findViewById(R.id.btnUseModel)
            val btnDownload: Button = view.findViewById(R.id.btnDownloadModel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_model, parent, false)
            return ModelViewHolder(view)
        }

        override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
            val model = models[position]
            val isDownloaded = File(externalFilesDir, model.fileName).exists()
            val isActive = model.fileName == activeFileName

            holder.tvName.text = model.displayName
            holder.tvDescription.text = model.description
            holder.tvSize.text = "Size: ${model.sizeLabel}"
            holder.tvLicense.text = "License: ${model.license}"

            when {
                isActive -> {
                    holder.tvStatus.text = "✓ Active"
                    holder.tvStatus.visibility = View.VISIBLE
                    holder.btnUse.isEnabled = false
                    holder.btnUse.text = "In Use"
                }
                isDownloaded -> {
                    holder.tvStatus.text = "Downloaded"
                    holder.tvStatus.visibility = View.VISIBLE
                    holder.btnUse.isEnabled = true
                    holder.btnUse.text = "Use This Model"
                }
                else -> {
                    holder.tvStatus.visibility = View.GONE
                    holder.btnUse.isEnabled = false
                    holder.btnUse.text = "Not Downloaded"
                }
            }

            holder.btnUse.setOnClickListener { onUseClicked(model) }
            holder.btnDownload.setOnClickListener { onDownloadClicked(model) }
            holder.tvLicense.setOnClickListener { onLicenseClicked(model) }
        }

        override fun getItemCount() = models.size
    }
}
