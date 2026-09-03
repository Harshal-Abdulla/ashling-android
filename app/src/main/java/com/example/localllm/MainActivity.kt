package com.example.localllm

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

    // True while a reply is streaming back. Stops a second request going out
    // on top of the first one.
    private var isGenerating = false

    // Saved chats. Kept in memory while the app is open and written to disk
    // whenever something changes, so nothing is lost if it gets killed.
    private lateinit var conversations: MutableList<Conversation>
    private lateinit var current: Conversation
    private lateinit var chatListAdapter: ChatListAdapter



    // activeModel = whichever model the user last selected in ModelLibraryActivity
    // SharedPreferences = tiny key-value store that survives app restarts
    // Think of it like a tiny dictionary saved to disk, like Python's shelve module
    private var activeModelFile: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()

        prefs = getSharedPreferences("localllm_prefs", MODE_PRIVATE)
        activeModelFile = prefs.getString("active_model", "") ?: ""

        showAccuracyWarningIfFirstLaunch()
        setupChat()
        setupChatList()
        loadModel()
    }

    /**
     * Keeps the top bar under the status bar and the input row above the
     * keyboard.
     *
     * Phones differ a lot here: notches, punch holes, gesture bars, and on
     * some phones a bottom bar that is there and on others one that isn't.
     * Hard-coded padding that looks right on one device is wrong on the next,
     * so the system tells us the sizes and we use those.
     *
     * The keyboard counts as an inset too, which is what stops the text box
     * ending up behind it while you are typing.
     */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.inputBar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            // Whichever is taller: the keyboard when it is open, the gesture
            // bar when it is not.
            view.updatePadding(bottom = maxOf(bars.bottom, ime.bottom))

            // Follow the messages down as the keyboard opens, otherwise you end
            // up typing at a view of the middle of the conversation.
            if (messages.isNotEmpty()) {
                binding.recyclerView.scrollToPosition(messages.lastIndex)
            }
            insets
        }
    }

    /**
     * Loads saved chats and wires up the side panel.
     *
     * The most recent chat is reopened on launch, so closing the app and coming
     * back lands you where you left off rather than on a blank screen.
     */
    private fun setupChatList() {
        conversations = ChatStore.load(this)
        current = conversations.firstOrNull() ?: ChatStore.newConversation().also {
            conversations.add(0, it)
        }

        messages.clear()
        messages.addAll(current.messages)
        adapter.notifyDataSetChanged()

        chatListAdapter = ChatListAdapter(
            conversations,
            onOpen = { openConversation(it) },
            onDelete = { deleteConversation(it) }
        )
        binding.rvChats.layoutManager = LinearLayoutManager(this)
        binding.rvChats.adapter = chatListAdapter

        binding.btnChats.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.drawerPanel)
        }
        binding.btnNewChat.setOnClickListener { startNewConversation() }
    }

    private fun openConversation(chat: Conversation) {
        saveCurrent()
        current = chat
        messages.clear()
        messages.addAll(chat.messages)
        adapter.notifyDataSetChanged()
        binding.recyclerView.scrollToPosition(messages.size - 1)
        binding.drawerLayout.closeDrawer(binding.drawerPanel)
    }

    private fun startNewConversation() {
        saveCurrent()
        // An unused empty chat is just clutter in the list, so reuse it rather
        // than stacking up "New chat" rows.
        val blank = conversations.firstOrNull { it.messages.isEmpty() }
        current = blank ?: ChatStore.newConversation().also { conversations.add(0, it) }
        messages.clear()
        adapter.notifyDataSetChanged()
        chatListAdapter.notifyDataSetChanged()
        binding.drawerLayout.closeDrawer(binding.drawerPanel)
        addWelcomeMessage()
    }

    private fun deleteConversation(chat: Conversation) {
        AlertDialog.Builder(this)
            .setTitle("Delete this chat?")
            .setMessage(chat.title)
            .setPositiveButton("Delete") { _, _ ->
                conversations.remove(chat)
                if (conversations.isEmpty()) conversations.add(ChatStore.newConversation())
                if (current == chat) {
                    current = conversations.first()
                    messages.clear()
                    messages.addAll(current.messages)
                    adapter.notifyDataSetChanged()
                }
                chatListAdapter.notifyDataSetChanged()
                ChatStore.save(this, conversations)
            }
            .setNegativeButton("Keep", null)
            .show()
    }

    /** Copies what's on screen into the current chat and writes it out. */
    private fun saveCurrent() {
        if (!::current.isInitialized) return
        current.messages.clear()
        current.messages.addAll(messages)
        current.updatedAt = System.currentTimeMillis()
        if (current.title == "New chat") current.title = ChatStore.titleFrom(messages)
        conversations.sortByDescending { it.updatedAt }
        ChatStore.save(this, conversations)
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
                // Save that we've shown the warning, never show again
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
            // No change animation. The reply row is rebound many times while
            // the answer streams in, and the default cross-fade made every
            // update flash.
            itemAnimator = null
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

        // Model library button, opens the model picker screen
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
            if (isGenerating) {
                Toast.makeText(this, "Still answering, hold on", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            binding.etMessage.setText("")
            sendMessage(text)
        }
    }

    private fun loadModel() {
        // If no model selected yet, send user to the model library
        if (activeModelFile.isEmpty()) {
            binding.tvStatus.text = "No model selected.\nTap Models to choose one."
            binding.tvStatus.visibility = View.VISIBLE
            return
        }

        val modelFile = File(getExternalFilesDir(null), activeModelFile)

        if (!modelFile.exists()) {
            binding.tvStatus.text = "Model file not found.\nTap Models to download it again."
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
                    // Only greet an empty chat. This used to wipe the messages
                    // every time the model finished loading, which threw away
                    // whatever conversation had just been restored.
                    if (messages.isEmpty()) addWelcomeMessage()
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
        isGenerating = true
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
                val model = llm
                if (model == null) {
                    withContext(Dispatchers.Main) { finishGenerating("Model was closed.") }
                    return@launch
                }

                // Synchronous, deliberately.
                //
                // generateResponseAsync streams token by token, which looks
                // nicer, but the done flag never arrived for several of these
                // models. The engine stayed busy, the send button stayed
                // disabled, and the next message came back with "Previous
                // invocation still processing". A timer to work around it only
                // moved the problem, because it re-enabled the button while the engine
                // was still locked.
                //
                // This call returns when the answer is finished, so there is no
                // state to get stuck in. The trade is that words appear all at
                // once instead of typing themselves out.
                val reply = model.generateResponse(prompt)

                // Store it already cleaned. The saved history is what gets fed
                // back into the next prompt, and it is also what you read if
                // you ever open the JSON file.
                val cleaned = ReplyFormatter.clean(reply)

                withContext(Dispatchers.Main) {
                    messages[aiIndex] = ChatMessage(cleaned, isUser = false)
                    adapter.notifyItemChanged(aiIndex)
                    keepAtBottomIfAlreadyThere()
                    finishGenerating(null)
                    // Replies are written into the list by index rather than
                    // through addMessage, so they need saving explicitly.
                    saveCurrent()
                    chatListAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    messages[aiIndex] = ChatMessage("Error: ${e.message}", isUser = false)
                    adapter.notifyItemChanged(aiIndex)
                    finishGenerating(null)
                }
            }
        }
    }

    // Puts the UI back to idle after a reply finishes or fails.
    // Was repeated in three places before, and one of them forgot to
    // re-enable the send button.
    private fun finishGenerating(error: String?) {
        if (!isGenerating) return
        isGenerating = false
        binding.progressBar.visibility = View.GONE
        binding.btnSend.isEnabled = true
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
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
    /**
     * Builds the prompt using the template the active model was trained with.
     *
     * This used to always write Gemma's format. Every other model then failed
     * to recognise the structure and simply continued the text instead of
     * answering: echoing the instruction back, inventing its own follow-up
     * questions, and rambling. That was the cause of the gibberish, not the
     * formatting of the reply afterwards.
     */
    private fun buildPrompt(): String {
        // Only recent turns. These have small context windows and a long chat
        // pushes the actual question out of view.
        val maxHistory = 10

        val history = messages
            .drop(if (messages.first().isUser.not()) 1 else 0)
            .dropLast(1)
            .takeLast(maxHistory)
            .map { if (it.isUser) it else ChatMessage(ReplyFormatter.clean(it.text), false) }

        val format = ModelLibrary.models
            .firstOrNull { it.fileName == activeModelFile }
            ?.promptFormat
            ?: PromptFormat.GEMMA

        return format.build(history, ReplyFormatter.SYSTEM_INSTRUCTION)
    }

    /**
     * Scrolls to the newest message, but only when you were already at the
     * bottom, because otherwise reading back through a long answer is impossible,
     * because the list yanks you to the end again.
     */
    private fun keepAtBottomIfAlreadyThere() {
        val lm = binding.recyclerView.layoutManager as? LinearLayoutManager ?: return
        val last = lm.findLastVisibleItemPosition()
        if (last >= messages.lastIndex - 2) {
            binding.recyclerView.scrollToPosition(messages.lastIndex)
        }
    }

    private fun addWelcomeMessage() {
        addMessage(
            ChatMessage(
                "Hi! I'm running locally on your device, no internet needed. Ask me anything!",
                isUser = false
            )
        )
    }

    private fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        saveCurrent()
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
            // The conversation stays. Changing which model answers is not a
            // reason to throw away what was already said.
            loadModel()
        }
    }

    override fun onPause() {
        super.onPause()
        saveCurrent()
        if (::chatListAdapter.isInitialized) chatListAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        llm?.close()
    }
}
