package com.example.projecthellopaw.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecthellopaw.R
import com.example.projecthellopaw.data.model.Message
import com.example.projecthellopaw.databinding.ActivityChatBinding
import com.example.projecthellopaw.ui.chat.ChatAdapter // Sesuaikan path ini jika beda
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import java.util.Date

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var messagesListener: ListenerRegistration? = null
    private lateinit var chatAdapter: ChatAdapter

    private var chatRoomId = ""
    private var ownerId = ""
    private var ownerName = ""
    private var petName = ""
    private var doctorId = ""
    private var doctorName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatRoomId = intent.getStringExtra("CHAT_ROOM_ID") ?: ""
        ownerId = intent.getStringExtra("OWNER_ID") ?: ""
        ownerName = intent.getStringExtra("OWNER_NAME") ?: ""
        petName = intent.getStringExtra("PET_NAME") ?: "Anabul"
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dokter"

        val currentUid = auth.currentUser?.uid ?: ""
        if (currentUid != doctorId && ownerName.isEmpty()) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    ownerName = doc.getString("name") ?: "Pemilik Hewan"
                }
        }

        setupToolbar()
        setupChat()
        setupInputArea()
        listenToMessages()
        setupEndButton() // Inisialisasi tombol selesaikan sesi
    }

    private fun setupEndButton() {
        val currentUid = auth.currentUser?.uid ?: ""
        val btnEnd = binding.btnEndConsultation

        // Tombol hanya tampil untuk dokter
        if (currentUid == doctorId) {
            btnEnd.visibility = View.VISIBLE
            btnEnd.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Selesaikan Konsultasi")
                    .setMessage("Yakin ingin mengakhiri sesi? Pasien akan diminta memberi rating.")
                    .setPositiveButton("Ya") { _, _ -> completeConsultation() }
                    .setNegativeButton("Tidak", null)
                    .show()
            }
        } else {
            btnEnd.visibility = View.GONE
        }
    }

    private fun completeConsultation() {
        db.collection("chat_rooms").document(chatRoomId)
            .update("chatStatus", "completed")
            .addOnSuccessListener {
                Toast.makeText(this, "Konsultasi selesai!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengakhiri sesi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 🔄 PERBAIKAN: Sesuaikan judul nama di atas toolbar chat
        val currentUid = auth.currentUser?.uid ?: ""
        if (currentUid == doctorId) {
            // Jika dokter yang buka, tampilkan nama Owner
            binding.tvChatName.text = ownerName.ifEmpty { "Pemilik Hewan" }
            binding.tvChatSubtitle.text = "Konsultasi · $petName"
        } else {
            // Jika Owner/User yang buka, tampilkan nama Dokter di atas toolbar
            binding.tvChatName.text = "drh. $doctorName"
            binding.tvChatSubtitle.text = "Konsultasi Sedang Berlangsung"
        }

        binding.ivMenuDots.setOnClickListener { /* show options menu jika perlu */ }
    }

    private fun setupChat() {
        val currentUserId = auth.currentUser?.uid ?: ""
        chatAdapter = ChatAdapter(mutableListOf(), currentUserId)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = chatAdapter
    }

    private fun setupInputArea() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        binding.btnAiSuggest.setOnClickListener {
            showAiSuggestions()
        }
    }

    private fun sendMessage(text: String, isAi: Boolean = false) {
        val uid = auth.currentUser?.uid ?: return

        // 🔄 PERBAIKAN: Menentukan nama pengirim secara akurat di Firestore
        val senderName = if (uid == doctorId) "drh. $doctorName" else ownerName

        val message = hashMapOf(
            "senderId" to uid,
            "senderName" to senderName,
            "text" to text,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "isAiGenerated" to isAi
        )

        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(message)

        db.collection("chat_rooms").document(chatRoomId)
            .update(
                "lastMessage", text,
                "lastMessageTime", com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
    }

    private fun listenToMessages() {
        if (chatRoomId.isEmpty()) return

        val welcomeMessageText = getString(R.string.welcome_message)
        val welcomeMsg = Message(
            messageId = "welcome",
            senderId = "system",
            senderName = "System",
            text = welcomeMessageText,
            timestamp = Date(),
            isAiGenerated = false
        )

        messagesListener = db.collection("chat_rooms")
            .document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val messages = mutableListOf(welcomeMsg)
                messages.addAll(snapshots.documents.mapNotNull { doc ->
                    Message(
                        messageId = doc.id,
                        senderId = doc.getString("senderId") ?: "",
                        senderName = doc.getString("senderName") ?: "",
                        text = doc.getString("text") ?: "",
                        timestamp = doc.getDate("timestamp"),
                        isAiGenerated = doc.getBoolean("isAiGenerated") ?: false
                    )
                })

                chatAdapter.updateMessages(messages)

                if (messages.isNotEmpty()) {
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun showAiSuggestions() {
        val rawMessages = chatAdapter.getMessages()
        if (rawMessages.isEmpty()) {
            Toast.makeText(this, "Belum ada percakapan untuk dianalisis AI", Toast.LENGTH_SHORT).show()
            return
        }

        val conversationHistory = ArrayList<TextMessage>()
        val lastFive = rawMessages.takeLast(5)

        for (msg in lastFive) {
            if (msg.senderId == "system") continue

            if (msg.senderId == auth.currentUser?.uid) {
                conversationHistory.add(TextMessage.createForLocalUser(
                    msg.text, System.currentTimeMillis()
                ))
            } else {
                conversationHistory.add(TextMessage.createForRemoteUser(
                    msg.text, System.currentTimeMillis(), msg.senderId
                ))
            }
        }

        val smartReplyClient = SmartReply.getClient()
        smartReplyClient.suggestReplies(conversationHistory)
            .addOnSuccessListener { result ->
                if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    val suggestionsList = mutableListOf<String>()
                    for (suggestion in result.suggestions) {
                        suggestionsList.add(suggestion.text)
                    }

                    while (suggestionsList.size < 3) {
                        suggestionsList.add("Ada keluhan lain, Dok?")
                    }
                    displayBottomSheet(suggestionsList)

                } else {
                    val fallbackSuggestions = listOf("Bisa ceritakan gejalanya?", "Sejak kapan terjadi?", "Apakah sudah diberi obat?")
                    displayBottomSheet(fallbackSuggestions)
                }
            }
            .addOnFailureListener {
                val safeSuggestions = listOf("Halo, ada yang bisa dibantu?", "Mohon tunggu sebentar ya.", "Bagaimana kondisi hewan sekarang?")
                displayBottomSheet(safeSuggestions)
            }
    }

    private fun displayBottomSheet(suggestions: List<String>) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_ai_suggest, null)
        dialog.setContentView(view)

        val tvSuggest1 = view.findViewById<TextView>(R.id.tv_suggest_1)
        val tvSuggest2 = view.findViewById<TextView>(R.id.tv_suggest_2)
        val tvSuggest3 = view.findViewById<TextView>(R.id.tv_suggest_3)

        tvSuggest1.text = suggestions.getOrNull(0)
        tvSuggest2.text = suggestions.getOrNull(1)
        tvSuggest3.text = suggestions.getOrNull(2)

        val clickListener = View.OnClickListener { v ->
            val text = (v as TextView).text.toString()
            sendMessage(text, isAi = true)
            dialog.dismiss()
        }

        tvSuggest1.setOnClickListener(clickListener)
        tvSuggest2.setOnClickListener(clickListener)
        tvSuggest3.setOnClickListener(clickListener)

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
    }
}