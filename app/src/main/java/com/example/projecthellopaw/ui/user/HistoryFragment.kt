package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
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

class HistoryFragment : Fragment() {

    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<HistoryItem>()
    private val filteredList = mutableListOf<HistoryItem>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var etSearch: EditText
    private lateinit var ivClear: ImageView

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
        etSearch = view.findViewById(R.id.etSearchHistory)
        ivClear = view.findViewById(R.id.ivClearSearchHistory)

        setupRecyclerView()
        loadConsultationHistory()

        // Search Listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    ivClear.visibility = View.GONE
                    filterHistory("")
                } else {
                    ivClear.visibility = View.VISIBLE
                    filterHistory(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        ivClear.setOnClickListener {
            etSearch.setText("")
            ivClear.visibility = View.GONE
            filterHistory("")
        }
    }

    private fun setupRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        adapter = HistoryAdapter(filteredList) { clickedItem ->
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

        db.collection("chat_rooms")
            .whereEqualTo("ownerId", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                if (documents.isEmpty) {
                    showEmptyState("Belum ada riwayat konsultasi.")
                    return@addOnSuccessListener
                }

                val filteredDocs = documents.filter { doc ->
                    val paymentStatus = doc.getString("paymentStatus") ?: ""
                    paymentStatus.equals("success", ignoreCase = true)
                }

                if (filteredDocs.isEmpty()) {
                    showEmptyState("Belum ada riwayat konsultasi.")
                    return@addOnSuccessListener
                }

                historyList.clear()
                val sortedDocs = filteredDocs.sortedByDescending {
                    it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                for (doc in sortedDocs) {
                    val duration = doc.getLong("duration")?.toInt() ?: 0
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
                    historyList.add(item)
                }

                filteredList.clear()
                filteredList.addAll(historyList)
                updateUIState()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Log.e("HistoryFragment", "Failed to load history", e)
                showEmptyState("Gagal memuat data riwayat: ${e.message}")
            }
    }

    private fun filterHistory(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(historyList)
        } else {
            val lowerQuery = query.lowercase()
            val filtered = historyList.filter { item ->
                item.doctorName.lowercase().contains(lowerQuery) ||
                        item.petName.lowercase().contains(lowerQuery)
            }
            filteredList.addAll(filtered)
        }
        updateUIState()
    }

    private fun updateUIState() {
        if (filteredList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = if (historyList.isEmpty()) "Belum ada riwayat konsultasi." else "Tidak ditemukan"
            rvHistory.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvHistory.visibility = View.VISIBLE
        }
        adapter.updateData(filteredList)
    }

    private fun showEmptyState(message: String) {
        tvEmptyState.visibility = View.VISIBLE
        tvEmptyState.text = message
        rvHistory.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (::rvHistory.isInitialized) {
            loadConsultationHistory()
        }
    }
}