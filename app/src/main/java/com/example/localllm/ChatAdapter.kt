package com.example.localllm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// RecyclerView.Adapter = the "engine" that powers the scrolling chat list.
// Think of it like a Python generator that produces views on demand —
// it only creates as many chat bubbles as fit on screen, then REUSES them
// as you scroll (that's the "Recycler" part — efficient on memory).
//
// Our adapter handles TWO types of bubbles:
//   VIEW_TYPE_USER = blue bubble on the right
//   VIEW_TYPE_AI   = gray bubble on the left
class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        const val VIEW_TYPE_USER = 1
        const val VIEW_TYPE_AI = 2
    }

    // ViewHolder = a small object that holds references to the views in ONE chat bubble.
    // Without this, Android would call findViewById() on every scroll — very slow.
    // With it, we find views once and cache them here.
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    // Called for each item — tells the RecyclerView which layout to use
    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    } 

    // Called when a new bubble needs to be created (inflated from XML)
    // "Inflate" = read the XML layout and turn it into actual View objects in memory
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_user_message
        } else {
            R.layout.item_ai_message
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    // Called when a bubble is being shown — fill it with the actual message text
    // This is like binding data to a template in Python's Jinja2
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.tvMessage.text = messages[position].text
    }

    // Total number of messages — like len(messages) in Python
    override fun getItemCount() = messages.size
}
