package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HistoryFragment : Fragment() {

    private lateinit var adapter: HistoryAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Kamu atau Programmer 2 perlu membuat HistoryAdapter jika belum ada.
    // Sementara kita buat deklarasi dasar untuk RecyclerView-nya.
    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Bind View (Pastikan ID ini ada di fragment_history.xml kalian) ──
        rvHistory = view.findViewById(R.id.rvHistory)
        tvEmptyState = view.findViewById(R.id.tvEmptyHistory) // Teks jika riwayat kosong
        progressBar = view.findViewById(R.id.progressBarHistory)

        setupRecyclerView()
        loadConsultationHistory()
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(requireContext())

        // 💡 TAMBAHKAN INI: Pasang adapter kosong di awal agar aplikasi tidak crash saat loading
        adapter = HistoryAdapter(emptyList()) { _ -> }
        rvHistory.adapter = adapter
    }

    private fun loadConsultationHistory() {
        val currentUserId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        // 🔍 QUERY FIRESTORE: Cari room chat milik user ini yang pembayarannya sukses
        db.collection("chat_rooms")
            .whereEqualTo("ownerId", currentUserId)
            .whereEqualTo("paymentStatus", "success") // Pastikan sudah bayar
            .orderBy("lastMessageTime", Query.Direction.DESCENDING) // Riwayat terbaru di atas
            .addSnapshotListener { snapshots, error ->
                progressBar.visibility = View.GONE

                if (error != null || snapshots == null) {
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Gagal memuat data riwayat."
                    return@addSnapshotListener
                }

                if (snapshots.isEmpty) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvHistory.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    rvHistory.visibility = View.VISIBLE

                    // ── 🛠️ SELESAI: Gabungkan data Firestore ke Adapter ──
                    val historyList = mutableListOf<HistoryItem>()
                    for (doc in snapshots.documents) {
                        val item = HistoryItem(
                            chatRoomId = doc.getString("chatRoomId") ?: "",
                            doctorId = doc.getString("doctorId") ?: "",
                            doctorName = doc.getString("doctorName") ?: "Dokter",
                            chatStatus = doc.getString("chatStatus") ?: "active",
                            lastMessage = doc.getString("lastMessage") ?: ""
                        )
                        historyList.add(item)
                    }

                    // Set adapter ke RecyclerView dengan aksi klik langsung ke ChatActivity
                    adapter = HistoryAdapter(historyList) { clickedItem ->
                        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                            putExtra("CHAT_ROOM_ID", clickedItem.chatRoomId)
                            putExtra("DOCTOR_ID", clickedItem.doctorId)
                            putExtra("DOCTOR_NAME", clickedItem.doctorName)
                        }
                        startActivity(intent)
                    }
                    rvHistory.adapter = adapter
                }
            }
    }
}