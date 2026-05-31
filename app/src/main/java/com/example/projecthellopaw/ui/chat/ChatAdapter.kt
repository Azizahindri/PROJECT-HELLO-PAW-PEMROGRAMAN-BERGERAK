package com.example.projecthellopaw.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.projecthellopaw.data.model.Message // ◄── Tambahkan baris ini!

class ChatAdapter(
    private val messages: MutableList<Message>,
    private val currentUserId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_SYSTEM = 0
        const val TYPE_LEFT = 1
        const val TYPE_RIGHT = 2
    }

    inner class SystemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tv_system_message)
    }

    inner class LeftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSenderName: TextView = itemView.findViewById(R.id.tv_sender_name)
        val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
    }

    inner class RightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        val tvAiBadge: TextView = itemView.findViewById(R.id.tv_ai_badge)
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return when {
            msg.senderId == "system" -> TYPE_SYSTEM
            msg.senderId == currentUserId -> TYPE_RIGHT
            else -> TYPE_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SYSTEM -> SystemViewHolder(
                inflater.inflate(R.layout.item_chat_system, parent, false)
            )
            TYPE_RIGHT -> RightViewHolder(
                inflater.inflate(R.layout.item_chat_right, parent, false)
            )
            else -> LeftViewHolder(
                inflater.inflate(R.layout.item_chat_left, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        val timeStr = msg.timestamp?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        } ?: ""

        when (holder) {
            is SystemViewHolder -> holder.tvMessage.text = msg.text

            is LeftViewHolder -> {
                holder.tvMessage.text = msg.text
                holder.tvTimestamp.text = timeStr

                // --- LOGIKA IMPROVISASI NAMA PENGIRIM ---
                // Cek apakah pesan sebelumnya dikirim oleh orang yang sama
                val isPreviousSameSender = position > 0 && messages[position - 1].senderId == msg.senderId

                if (msg.senderName.isNotEmpty() && !isPreviousSameSender) { // ◄── Selesai!
                    // Hanya tampilkan nama jika ini adalah awal chat dari si pengirim
                    holder.tvSenderName.visibility = View.VISIBLE
                    holder.tvSenderName.text = msg.senderName
                } else {
                    // Sembunyikan nama jika dia mengirim pesan beruntun (seperti WhatsApp/Telegram)
                    holder.tvSenderName.visibility = View.GONE
                }
                // ----------------------------------------
            }

            is RightViewHolder -> {
                holder.tvMessage.text = msg.text
                holder.tvTimestamp.text = timeStr
                // Sesuai logika Programmer 4, jika isAiGenerated = true, badge AI langsung menyala!
                holder.tvAiBadge.visibility = if (msg.isAiGenerated) View.VISIBLE else View.GONE
            }
        }
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Message>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getMessages(): List<Message> = messages.toList()

    private class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos].messageId == newList[newPos].messageId
        override fun areContentsTheSame(oldPos: Int, newPos: Int) =
            oldList[oldPos] == newList[newPos]
    }
}
