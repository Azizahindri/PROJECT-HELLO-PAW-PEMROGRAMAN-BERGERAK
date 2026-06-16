package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.chat.ChatActivity

data class HistoryItem(
    val chatRoomId: String,
    val doctorId: String,
    val doctorName: String,
    val ownerId: String = "",
    val petName: String = "",
    val chatStatus: String,
    val lastMessage: String,
    val hasReview: Boolean = false,
    val duration: Int = 0
)

class HistoryAdapter(
    private var historyList: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_row, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.bind(item, onItemClick)
    }

    override fun getItemCount(): Int = historyList.size

    fun updateData(newList: List<HistoryItem>) {
        historyList = newList
        notifyDataSetChanged()
    }

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDocName: TextView = itemView.findViewById(R.id.tvHistoryDocName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvHistoryStatus)
        private val tvPetName: TextView = itemView.findViewById(R.id.tvHistoryPetName)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvHistoryLastMessage)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvHistoryDuration)
        private val btnRate: Button = itemView.findViewById(R.id.btnRate)

        fun bind(item: HistoryItem, onItemClick: (HistoryItem) -> Unit) {
            tvDocName.text = "drh. ${item.doctorName}"
            tvPetName.text = "Hewan: ${item.petName.ifEmpty { "Anabul" }}"
            tvMessage.text = item.lastMessage.ifEmpty { "Belum ada pesan." }

            if (item.duration > 0) {
                tvDuration.visibility = View.VISIBLE
                tvDuration.text = "Durasi: ${item.duration} menit"
            } else {
                tvDuration.visibility = View.GONE
            }

            if (item.chatStatus == "active") {
                tvStatus.text = "🟢 Sedang Berlangsung"
                tvStatus.setTextColor(Color.parseColor("#1B5E20"))
                btnRate.visibility = View.GONE

                itemView.setOnClickListener {
                    val intent = Intent(itemView.context, ChatActivity::class.java).apply {
                        putExtra("CHAT_ROOM_ID", item.chatRoomId)
                        putExtra("DOCTOR_ID", item.doctorId)
                        putExtra("DOCTOR_NAME", item.doctorName)
                        putExtra("OWNER_ID", item.ownerId)
                        putExtra("PET_NAME", item.petName)
                    }
                    itemView.context.startActivity(intent)
                }
            } else {
                tvStatus.text = "⚫ Selesai"
                tvStatus.setTextColor(Color.parseColor("#757575"))

                if (item.hasReview) {
                    btnRate.text = "📋 Lihat Review"
                    btnRate.visibility = View.VISIBLE
                    btnRate.setOnClickListener {
                        Toast.makeText(itemView.context, "Review sudah diberikan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    btnRate.text = "⭐ Beri Review"
                    btnRate.visibility = View.VISIBLE
                    btnRate.setOnClickListener {
                        val intent = Intent(itemView.context, ReviewActivity::class.java).apply {
                            putExtra("CHAT_ROOM_ID", item.chatRoomId)
                            putExtra("DOCTOR_ID", item.doctorId)
                            putExtra("DOCTOR_NAME", item.doctorName)
                            putExtra("OWNER_ID", item.ownerId)
                            putExtra("PET_NAME", item.petName)
                            putExtra("DURATION", item.duration)
                        }
                        itemView.context.startActivity(intent)
                    }
                }

                itemView.setOnClickListener {
                    val intent = Intent(itemView.context, ChatActivity::class.java).apply {
                        putExtra("CHAT_ROOM_ID", item.chatRoomId)
                        putExtra("DOCTOR_ID", item.doctorId)
                        putExtra("DOCTOR_NAME", item.doctorName)
                        putExtra("OWNER_ID", item.ownerId)
                        putExtra("PET_NAME", item.petName)
                    }
                    itemView.context.startActivity(intent)
                }
            }
        }
    }
}