package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
    private val historyList = mutableListOf<HistoryItem>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val TAG = "HistoryFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHistory = view.findViewById(R.id.rvHistory)
        tvEmptyState = view.findViewById(R.id.tvEmptyHistory)
        progressBar = view.findViewById(R.id.progressBarHistory)

        setupRecyclerView()
        loadConsultationHistory()
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(historyList) { clickedItem ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("CHAT_ROOM_ID", clickedItem.chatRoomId)
                putExtra("DOCTOR_ID", clickedItem.doctorId)
                putExtra("DOCTOR_NAME", clickedItem.doctorName)
                putExtra("OWNER_ID", clickedItem.ownerId)
                putExtra("PET_NAME", clickedItem.petName)
            }
            startActivity(intent)
        }
        rvHistory.adapter = adapter
    }

    private fun loadConsultationHistory() {
        val currentUserId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        Log.d(TAG, "Loading history for user: $currentUserId")

        // ✅ PERBAIKAN: Hapus whereIn dan orderBy dulu untuk menghindari error index
        db.collection("chat_rooms")
            .whereEqualTo("ownerId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE
                Log.d(TAG, "Total documents: ${documents.size()}")

                if (documents.isEmpty) {
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Belum ada riwayat konsultasi."
                    rvHistory.visibility = View.GONE
                    return@addOnSuccessListener
                }

                // ✅ FILTER MANUAL UNTUK PAYMENT STATUS
                val filteredDocs = documents.filter { doc ->
                    val paymentStatus = doc.getString("paymentStatus") ?: ""
                    paymentStatus.equals("success", ignoreCase = true)
                }

                Log.d(TAG, "Filtered documents: ${filteredDocs.size}")

                if (filteredDocs.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Belum ada riwayat konsultasi."
                    rvHistory.visibility = View.GONE
                    return@addOnSuccessListener
                }

                tvEmptyState.visibility = View.GONE
                rvHistory.visibility = View.VISIBLE

                historyList.clear()

                // ✅ SORTIR MANUAL berdasarkan createdAt (terbaru di atas)
                val sortedDocs = filteredDocs.sortedByDescending {
                    it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                for (doc in sortedDocs) {
                    val duration = doc.getLong("duration")?.toInt() ?: 0

                    // ✅ PERBAIKAN: Jika durasi tidak wajar (> 24 jam), set ke 0
                    val finalDuration = if (duration > 1440) 0 else duration

                    val item = HistoryItem(
                        chatRoomId = doc.getString("chatRoomId") ?: doc.id,
                        doctorId = doc.getString("doctorId") ?: "",
                        doctorName = doc.getString("doctorName") ?: "Dokter",
                        ownerId = doc.getString("ownerId") ?: "",
                        petName = doc.getString("petName") ?: "Anabul",
                        chatStatus = doc.getString("chatStatus") ?: "completed",
                        lastMessage = doc.getString("lastMessage") ?: "Belum ada pesan.",
                        hasReview = doc.getBoolean("hasReview") ?: false,
                        duration = finalDuration
                    )
                    Log.d(TAG, "Item: ${item.doctorName}, duration: ${item.duration}")
                    historyList.add(item)
                }

                adapter.updateData(historyList)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Failed to load history: ${e.message}", e)

                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = "Gagal memuat data riwayat: ${e.message}"
                Toast.makeText(requireContext(), "Gagal memuat riwayat: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data saat fragment di-resume
        if (::rvHistory.isInitialized) {
            loadConsultationHistory()
        }
    }
}