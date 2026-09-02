package com.example.localllm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** The list of saved chats in the side panel. */
class ChatListAdapter(
    private val conversations: List<Conversation>,
    private val onOpen: (Conversation) -> Unit,
    private val onDelete: (Conversation) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvChatTitle)
        val delete: Button = view.findViewById(R.id.btnDeleteChat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_row, parent, false)
        return Holder(view)
    }

    override fun getItemCount() = conversations.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val chat = conversations[position]
        holder.title.text = chat.title
        holder.title.setOnClickListener { onOpen(chat) }
        holder.delete.setOnClickListener { onDelete(chat) }
    }
}
