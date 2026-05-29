package com.example.projecthellopaw.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityChatBinding
import java.util.Date

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Date? = null,
    val isAiGenerated: Boolean = false
)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatRoomId = intent.getStringExtra("CHAT_ROOM_ID") ?: ""
        ownerId = intent.getStringExtra("OWNER_ID") ?: ""
        ownerName = intent.getStringExtra("OWNER_NAME") ?: getString(R.string.default_owner_name)
        petName = intent.getStringExtra("PET_NAME") ?: ""
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: auth.currentUser?.uid ?: ""

        setupToolbar()
        setupChat()
        setupInputArea()
        listenToMessages()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.tvChatName.text = ownerName
        if (petName.isNotEmpty()) {
            val consultationText = getString(R.string.consultation)
            binding.tvChatSubtitle.text = "$consultationText · $petName"
        }
        binding.ivMenuDots.setOnClickListener { /* show options menu */ }
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
        val senderName = if (uid == doctorId) "Dokter" else ownerName

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
            .addOnFailureListener { e ->
                // Handle error jika perlu
            }

        db.collection("chat_rooms").document(chatRoomId)
            .update(
                "lastMessage", text,
                "lastMessageTime", com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            .addOnFailureListener { e ->
                // Handle error jika perlu
            }
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
                if (error != null || snapshots == null) {
                    return@addSnapshotListener
                }

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

                // Scroll ke pesan terakhir
                if (messages.isNotEmpty()) {
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun getAiSuggestions(): List<String> {
        val lastMessages = chatAdapter.getMessages().takeLast(3)
            .filter { it.senderId != auth.currentUser?.uid && it.senderId != "system" }
            .joinToString(" ") { it.text.lowercase() }

        return when {
            lastMessages.contains("demam") || lastMessages.contains("panas") -> listOf(
                getString(R.string.ai_fever_1),
                getString(R.string.ai_fever_2),
                getString(R.string.ai_fever_3)
            )
            lastMessages.contains("muntah") || lastMessages.contains("mual") -> listOf(
                getString(R.string.ai_vomit_1),
                getString(R.string.ai_vomit_2),
                getString(R.string.ai_vomit_3)
            )
            lastMessages.contains("diare") || lastMessages.contains("mencret") -> listOf(
                getString(R.string.ai_diarrhea_1),
                getString(R.string.ai_diarrhea_2),
                getString(R.string.ai_diarrhea_3)
            )
            lastMessages.contains("luka") || lastMessages.contains("berdarah") -> listOf(
                getString(R.string.ai_wound_1),
                getString(R.string.ai_wound_2),
                getString(R.string.ai_wound_3)
            )
            lastMessages.contains("tidak mau makan") || lastMessages.contains("nafsu makan") -> listOf(
                getString(R.string.ai_eating_1),
                getString(R.string.ai_eating_2),
                getString(R.string.ai_eating_3)
            )
            else -> listOf(
                getString(R.string.ai_suggestion_1),
                getString(R.string.ai_suggestion_2),
                getString(R.string.ai_suggestion_3)
            )
        }
    }

    private fun showAiSuggestions() {
        val suggestions = getAiSuggestions()
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_ai_suggest, null)
        dialog.setContentView(view)

        val tvSuggest1 = view.findViewById<TextView>(R.id.tv_suggest_1)
        val tvSuggest2 = view.findViewById<TextView>(R.id.tv_suggest_2)
        val tvSuggest3 = view.findViewById<TextView>(R.id.tv_suggest_3)

        tvSuggest1.text = suggestions.getOrNull(0) ?: getString(R.string.ai_suggestion_1)
        tvSuggest2.text = suggestions.getOrNull(1) ?: getString(R.string.ai_suggestion_2)
        tvSuggest3.text = suggestions.getOrNull(2) ?: getString(R.string.ai_suggestion_3)

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