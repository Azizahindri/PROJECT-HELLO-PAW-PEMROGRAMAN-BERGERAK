package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R

data class HistoryItem(
    val chatRoomId: String,
    val doctorId: String,
    val doctorName: String,
    val chatStatus: String, // "active" atau "completed"
    val lastMessage: String
)

class HistoryAdapter(
    private val historyList: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        // MENGGUNAKAN LAYOUT BARU YANG KAMU BUAT
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_row, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]
        holder.bind(item, onItemClick)
    }

    override fun getItemCount(): Int = historyList.size

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // MENGHUBUNGKAN DENGAN ID YANG ADA DI item_history_row.xml
        private val tvDocName: TextView = itemView.findViewById(R.id.tvHistoryDocName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvHistoryStatus)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvHistoryLastMessage)

        // Di dalam class HistoryViewHolder (setelah deklarasi private val lain):
        private val btnRate: Button = itemView.findViewById(R.id.btnRate)

        // Di dalam fungsi bind():
        fun bind(item: HistoryItem, onItemClick: (HistoryItem) -> Unit) {
            tvDocName.text = "drh. ${item.doctorName}"
            tvMessage.text = item.lastMessage.ifEmpty { "Belum ada pesan." }

            if (item.chatStatus == "active") {
                tvStatus.text = "🟢 Sedang Berlangsung"
                tvStatus.setTextColor(Color.parseColor("#1B5E20"))
                btnRate.visibility = View.GONE // Sembunyikan tombol saat aktif
            } else {
                tvStatus.text = "⚫ Selesai"
                tvStatus.setTextColor(Color.parseColor("#757575"))
                btnRate.visibility = View.VISIBLE // Munculkan tombol saat selesai

                // Logika klik tombol rating
                btnRate.setOnClickListener {
                    val intent = Intent(itemView.context, ReviewActivity::class.java).apply {
                        putExtra("DOCTOR_ID", item.doctorId)
                    }
                    itemView.context.startActivity(intent)
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}