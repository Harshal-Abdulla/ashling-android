package com.example.localllm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * One saved conversation.
 *
 * The title is the first thing the user said, shortened. That is usually enough
 * to recognise a chat in a list without asking anyone to name it.
 */
data class Conversation(
    val id: String,
    var title: String,
    val messages: MutableList<ChatMessage> = mutableListOf(),
    var updatedAt: Long = System.currentTimeMillis()
)

/**
 * Saves conversations to a JSON file in the app's own storage.
 *
 * No database. There will be tens of these, not thousands, and the whole file
 * is small enough to read and write in one go. Room would be three more
 * dependencies and a migration story for something a text file does fine.
 *
 * Everything here is on-device. The app has no internet permission for chat,
 * and the history never leaves the phone.
 */
object ChatStore {

    private const val FILE_NAME = "conversations.json"
    private const val TITLE_MAX = 40

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    fun load(context: Context): MutableList<Conversation> {
        val f = file(context)
        if (!f.exists()) return mutableListOf()

        return try {
            val root = JSONArray(f.readText())
            val out = mutableListOf<Conversation>()
            for (i in 0 until root.length()) {
                val obj = root.getJSONObject(i)
                val msgs = mutableListOf<ChatMessage>()
                val arr = obj.optJSONArray("messages") ?: JSONArray()
                for (j in 0 until arr.length()) {
                    val m = arr.getJSONObject(j)
                    msgs.add(ChatMessage(m.optString("text"), m.optBoolean("isUser")))
                }
                out.add(
                    Conversation(
                        id = obj.optString("id"),
                        title = obj.optString("title", "Chat"),
                        messages = msgs,
                        updatedAt = obj.optLong("updatedAt")
                    )
                )
            }
            // Newest first, which is the order people expect a chat list in.
            out.sortByDescending { it.updatedAt }
            out
        } catch (e: Exception) {
            // A corrupted file should not stop the app opening. Losing the
            // history is bad; refusing to start is worse.
            mutableListOf()
        }
    }

    fun save(context: Context, conversations: List<Conversation>) {
        try {
            val root = JSONArray()
            conversations.forEach { c ->
                val msgs = JSONArray()
                c.messages.forEach { m ->
                    msgs.put(JSONObject().put("text", m.text).put("isUser", m.isUser))
                }
                root.put(
                    JSONObject()
                        .put("id", c.id)
                        .put("title", c.title)
                        .put("updatedAt", c.updatedAt)
                        .put("messages", msgs)
                )
            }
            file(context).writeText(root.toString())
        } catch (e: Exception) {
            // Nothing useful to do here. The chat on screen still works.
        }
    }

    /** First thing the user said, trimmed to something that fits in the list. */
    fun titleFrom(messages: List<ChatMessage>): String {
        val first = messages.firstOrNull { it.isUser }?.text?.trim().orEmpty()
        if (first.isEmpty()) return "New chat"
        return if (first.length <= TITLE_MAX) first else first.take(TITLE_MAX).trimEnd() + "…"
    }

    fun newConversation(): Conversation =
        Conversation(id = System.currentTimeMillis().toString(), title = "New chat")
}
