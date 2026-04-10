package com.example.localllm

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.localllm.databinding.ActivityMainBinding
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ChatAdapter
    private lateinit var prefs: SharedPreferences

    private val messages = mutableListOf<ChatMessage>()
    private var llm: LlmInference? = null

    // activeModel = whichever model the user last selected in ModelLibraryActivity
    // SharedPreferences = tiny key-value store that survives app restarts
    // Think of it like a tiny dictionary saved to disk — like Python's shelve module
    private var activeModelFile: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("localllm_prefs", MODE_PRIVATE)
        activeModelFile = prefs.getString("active_model", "") ?: ""

        showAccuracyWarningIfFirstLaunch()
        setupChat()
        loadModel()
    }

    // SharedPreferences stores a flag "warning_shown" = true after first launch
    // So this dialog only appears once ever
    private fun showAccuracyWarningIfFirstLaunch() {
        val warningShown = prefs.getBoolean("warning_shown", false)
        if (warningShown) return

        AlertDialog.Builder(this)
            .setTitle("⚠️ ${getString(R.string.accuracy_warning_title)}")
            .setMessage(getString(R.string.accuracy_warning_message))
            .setPositiveButton("I understand") { dialog, _ ->
                // Save that we've shown the warning — never show again
                prefs.edit().putBoolean("warning_shown", true).apply()
                dialog.dismiss()
            }
            .setCancelable(false) // user MUST tap "I understand" to continue
            .show()
    }

    private fun setupChat() {
        adapter = ChatAdapter(messages)

        binding.recyclerView.apply {
            adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(this@MainActivity).also {
                it.stackFromEnd = true
            }
        }

        // Clear conversation button
        binding.btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear conversation?")
                .setMessage("This will erase all messages in this chat.")
                .setPositiveButton("Clear") { _, _ ->
                    messages.clear()
                    adapter.notifyDataSetChanged()
                    addMessage(ChatMessage("Conversation cleared. Ask me anything!", isUser = false))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Model library button — opens the model picker screen
        binding.btnModels.setOnClickListener {
            startActivity(Intent(this, ModelLibraryActivity::class.java))
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            if (llm == null) {
                Toast.makeText(this, "Model still loading...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.etMessage.setText("")
            sendMessage(text)
        }
    }

    private fun loadModel() {
        // If no model selected yet, send user to the model library
        if (activeModelFile.isEmpty()) {
            binding.tvStatus.text = "No model selected.\nTap the Models button to choose one."
            binding.tvStatus.visibility = View.VISIBLE
            return
        }

        val modelFile = File(getExternalFilesDir(null), activeModelFile)

        if (!modelFile.exists()) {
            binding.tvStatus.text = "Model file not found.\nTap Models to download one."
            binding.tvStatus.visibility = View.VISIBLE
            return
        }

        binding.tvStatus.text = "Loading model..."
        binding.tvStatus.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(512)
                    .build()

                val model = LlmInference.createFromOptions(this@MainActivity, options)

                withContext(Dispatchers.Main) {
                    llm = model
                    binding.tvStatus.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                    // Clear old messages and show fresh welcome
                    messages.clear()
                    adapter.notifyDataSetChanged()
                    addMessage(ChatMessage(
                        "Hi! I'm running locally on your device — no internet needed. Ask me anything!",
                        isUser = false
                    ))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "Error loading model: ${e.message}"
                    binding.tvStatus.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun sendMessage(userText: String) {
        addMessage(ChatMessage(userText, isUser = true))

        messages.add(ChatMessage("", isUser = false))
        val aiIndex = messages.lastIndex
        adapter.notifyItemInserted(aiIndex)

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSend.isEnabled = false

        // Build prompt BEFORE launching the coroutine
        // This captures the current state of messages on the main thread
        val prompt = buildPrompt()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                var fullResponse = ""

                llm!!.generateResponseAsync(prompt) { partialToken, isDone ->
                    fullResponse += partialToken

                    lifecycleScope.launch(Dispatchers.Main) {
                        messages[aiIndex] = ChatMessage(fullResponse, isUser = false)
                        adapter.notifyItemChanged(aiIndex)
                        binding.recyclerView.scrollToPosition(messages.lastIndex)

                        if (isDone) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSend.isEnabled = true
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages[aiIndex] = ChatMessage("Error: ${e.message}", isUser = false)
                    adapter.notifyItemChanged(aiIndex)
                    binding.progressBar.visibility = View.GONE
                    binding.btnSend.isEnabled = true
                }
            }
        }
    }

    // Builds the full conversation history as a Gemma-format prompt
    // This is what gives the AI "memory" of the conversation
    //
    // Example output:
    // <start_of_turn>user
    // What is 1+1?<end_of_turn>
    // <start_of_turn>model
    // 2<end_of_turn>
    // <start_of_turn>user
    // What did I just ask?<end_of_turn>
    // <start_of_turn>model
    //
    private fun buildPrompt(): String {
        val sb = StringBuilder()

        // System instruction — prepended to the first user message.
        // This tells Gemma to use the conversation context rather than
        // falling back to its default "I have no memory" trained response.
        val systemInstruction = "You are a helpful, friendly personal AI assistant. " +
            "You have access to the full conversation history shown below. " +
            "Use it to answer questions accurately. If the user told you their name, remember it."

        // Skip the first message if it's the AI welcome message (not a real model response)
        // Then skip the last message which is the blank AI placeholder we just added
        val history = messages
            .drop(if (messages.first().isUser.not()) 1 else 0)
            .dropLast(1)

        history.forEachIndexed { index, message ->
            if (message.isUser) {
                // Prepend system instruction to the very first user message only
                val content = if (index == 0) "$systemInstruction\n\n${message.text}"
                              else message.text
                sb.append("<start_of_turn>user\n$content<end_of_turn>\n")
            } else if (message.text.isNotEmpty()) {
                sb.append("<start_of_turn>model\n${message.text}<end_of_turn>\n")
            }
        }

        // End with model turn — this is where Gemma continues writing
        sb.append("<start_of_turn>model\n")
        return sb.toString()
    }

    private fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        adapter.notifyItemInserted(messages.lastIndex)
        binding.recyclerView.scrollToPosition(messages.lastIndex)
    }

    // onResume = called every time you come BACK to this screen
    // (e.g. after returning from ModelLibraryActivity)
    // We reload the model if the user changed their selection
    override fun onResume() {
        super.onResume()
        val newModel = prefs.getString("active_model", "") ?: ""
        if (newModel != activeModelFile) {
            activeModelFile = newModel
            llm?.close()
            llm = null
            messages.clear()
            adapter.notifyDataSetChanged()
            loadModel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llm?.close()
    }
}
