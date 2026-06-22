package com.example.projecthellopaw.ui.chat

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecthellopaw.R
import com.example.projecthellopaw.data.model.Message
import com.example.projecthellopaw.databinding.ActivityChatBinding
import com.example.projecthellopaw.ui.user.ReviewActivity
import com.example.projecthellopaw.utils.GeminiApiService
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var messagesListener: ListenerRegistration? = null
    private var chatAdapter: ChatAdapter? = null

    private var chatRoomId = ""
    private var ownerId = ""
    private var ownerName = ""
    private var petName = ""
    private var doctorId = ""
    private var doctorName = ""

    private var geminiService: GeminiApiService? = null
    private var apiKey: String = ""

    private var sessionStartTime: Long = 0
    private var timer: CountDownTimer? = null
    private var isSessionEnding = false
    private var isConsultationCompleted = false
    private var isChatReadOnly = false
    private var hasReview = false
    private var userRole: String = "OWNER"

    private var consultationDuration = 0
    private var isMessagesListenerActive = false

    companion object {
        private const val TAG = "ChatActivity"
        private const val SESSION_DURATION = 1 * 60 * 1000L
        private const val WARNING_TIME = 10 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityChatBinding.inflate(layoutInflater)
            setContentView(binding.root)

            chatRoomId = intent.getStringExtra("CHAT_ROOM_ID") ?: ""
            ownerId = intent.getStringExtra("OWNER_ID") ?: ""
            ownerName = intent.getStringExtra("OWNER_NAME") ?: ""
            petName = intent.getStringExtra("PET_NAME") ?: getString(R.string.default_pet_name)
            doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
            doctorName = intent.getStringExtra("DOCTOR_NAME") ?: getString(R.string.default_doctor_name)

            Log.d(TAG, "=== CHAT STARTED ===")
            Log.d(TAG, "chatRoomId: $chatRoomId")

            if (chatRoomId.isEmpty()) {
                Toast.makeText(this, "ID Chat tidak valid", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            val currentUid = auth.currentUser?.uid ?: ""
            if (currentUid != doctorId && ownerName.isEmpty()) {
                db.collection("users").document(currentUid).get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val doc = task.result
                            if (doc != null && doc.exists()) {
                                ownerName = doc.getString("name") ?: getString(R.string.default_owner_name)
                                setupToolbar()
                            }
                        } else {
                            ownerName = getString(R.string.default_owner_name)
                        }
                    }
            }

            checkChatStatusBeforeLoad()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkChatStatusBeforeLoad() {
        db.collection("chat_rooms")
            .document(chatRoomId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val chatStatus = doc.getString("chatStatus") ?: "active"
                    hasReview = doc.getBoolean("hasReview") ?: false
                    consultationDuration = doc.getLong("duration")?.toInt() ?: 0

                    Log.d(TAG, "Chat Status: $chatStatus")

                    when (chatStatus) {
                        "completed", "finished" -> {
                            isConsultationCompleted = true
                            isChatReadOnly = true

                            setupToolbar()
                            setupChat()
                            setChatReadOnly()
                            listenToMessages()
                            getUserRole()

                            Toast.makeText(
                                this,
                                "Konsultasi ini sudah selesai",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        "active", "waiting", "pending" -> {
                            isConsultationCompleted = false
                            isChatReadOnly = false

                            setupToolbar()
                            setupChat()
                            setupInputArea()
                            listenToMessages()
                            setupEndButton()
                            fetchGeminiApiKey()
                            getUserRole()
                            checkChatStatusAndTime()
                        }
                        else -> {
                            Toast.makeText(this, "Status chat tidak dikenali", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                } else {
                    Toast.makeText(this, "Chat room tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error: ${e.message}", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun getUserRole() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doc = task.result
                    if (doc != null && doc.exists()) {
                        userRole = doc.getString("role") ?: "OWNER"
                        Log.d(TAG, "User role: $userRole")
                    }
                }
            }
    }

    private fun checkChatStatusAndTime() {
        db.collection("chat_rooms").document(chatRoomId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    sessionStartTime = System.currentTimeMillis()
                    startSessionTimer()
                    return@addOnSuccessListener
                }

                val chatStatus = document.getString("chatStatus")
                hasReview = document.getBoolean("hasReview") ?: false
                consultationDuration = document.getLong("duration")?.toInt() ?: 0

                if (chatStatus == "completed") {
                    Log.d(TAG, "=== CHAT SUDAH SELESAI ===")
                    timer?.cancel()
                    binding.tvTimer.visibility = View.GONE

                    binding.tvChatSubtitle.text = "✅ Konsultasi Selesai"
                    binding.tvChatSubtitle.setTextColor(android.graphics.Color.parseColor("#A5D6A7"))
                    binding.tvChatSubtitle.visibility = View.VISIBLE

                    setChatReadOnly()
                    return@addOnSuccessListener
                }

                val createdAt = document.getTimestamp("createdAt")

                if (createdAt == null) {
                    val updates = hashMapOf<String, Any>(
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("chat_rooms").document(chatRoomId)
                        .update(updates)
                        .addOnCompleteListener {
                            sessionStartTime = System.currentTimeMillis()
                            startSessionTimer()
                        }
                } else {
                    val elapsedTime = System.currentTimeMillis() - createdAt.toDate().time
                    val remainingTime = SESSION_DURATION - elapsedTime

                    Log.d(TAG, "createdAt: ${createdAt.toDate()}")
                    Log.d(TAG, "elapsedTime: ${elapsedTime / 1000} detik")
                    Log.d(TAG, "remainingTime: ${remainingTime / 1000} detik")

                    if (remainingTime > 0) {
                        sessionStartTime = createdAt.toDate().time
                        startSessionTimerWithRemaining(remainingTime)
                    } else {
                        Toast.makeText(this, "Sesi konsultasi telah berakhir", Toast.LENGTH_SHORT).show()
                        isSessionEnding = true
                        completeConsultation()
                    }
                }
            }
    }

    private fun setChatReadOnly() {
        isChatReadOnly = true
        isConsultationCompleted = true

        timer?.cancel()
        binding.tvTimer.visibility = View.GONE

        binding.tvChatSubtitle.text = "✅ Konsultasi Selesai"
        binding.tvChatSubtitle.setTextColor(android.graphics.Color.parseColor("#A5D6A7"))
        binding.tvChatSubtitle.visibility = View.VISIBLE

        binding.inputArea.visibility = View.GONE

        binding.btnEndConsultation.visibility = View.GONE

        binding.tvChatStatus.visibility = View.VISIBLE
        binding.tvChatStatus.text = "✅ Konsultasi telah berakhir"
        binding.tvChatStatus.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
        binding.tvChatStatus.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))

        binding.btnBackToHome.visibility = View.VISIBLE
        binding.btnBackToHome.setOnClickListener {
            finish()
        }

        binding.btnViewReview.visibility = View.VISIBLE

        setupReviewButtonByRole()
    }

    private fun setupReviewButtonByRole() {
        val currentUid = auth.currentUser?.uid ?: ""

        val isDoctor = currentUid == doctorId
        val isOwner = currentUid == ownerId

        if (!isConsultationCompleted) {
            binding.btnViewReview.visibility = View.GONE
            return
        }

        when {
            isDoctor && hasReview -> {
                binding.btnViewReview.visibility = View.VISIBLE
                binding.btnViewReview.text = "⭐ Lihat Review"

                binding.btnViewReview.setOnClickListener {
                    val intent = Intent(this, ReviewActivity::class.java)
                    intent.putExtra("CHAT_ROOM_ID", chatRoomId)
                    intent.putExtra("DOCTOR_ID", doctorId)
                    intent.putExtra("DOCTOR_NAME", doctorName)
                    intent.putExtra("OWNER_ID", ownerId)
                    intent.putExtra("PET_NAME", petName)
                    intent.putExtra("DURATION", consultationDuration)
                    intent.putExtra("IS_READ_ONLY", true)
                    startActivity(intent)
                }
            }

            isDoctor && !hasReview -> {
                binding.btnViewReview.visibility = View.GONE
            }

            isOwner && hasReview -> {
                binding.btnViewReview.visibility = View.VISIBLE
                binding.btnViewReview.text = "⭐ Lihat Review"

                binding.btnViewReview.setOnClickListener {
                    val intent = Intent(this, ReviewActivity::class.java)
                    intent.putExtra("CHAT_ROOM_ID", chatRoomId)
                    intent.putExtra("DOCTOR_ID", doctorId)
                    intent.putExtra("DOCTOR_NAME", doctorName)
                    intent.putExtra("OWNER_ID", ownerId)
                    intent.putExtra("PET_NAME", petName)
                    intent.putExtra("DURATION", consultationDuration)
                    intent.putExtra("IS_READ_ONLY", true)
                    startActivity(intent)
                }
            }

            isOwner && !hasReview -> {
                binding.btnViewReview.visibility = View.VISIBLE
                binding.btnViewReview.text = "⭐ Beri Review"

                binding.btnViewReview.setOnClickListener {
                    val intent = Intent(this, ReviewActivity::class.java)
                    intent.putExtra("CHAT_ROOM_ID", chatRoomId)
                    intent.putExtra("DOCTOR_ID", doctorId)
                    intent.putExtra("DOCTOR_NAME", doctorName)
                    intent.putExtra("OWNER_ID", ownerId)
                    intent.putExtra("PET_NAME", petName)
                    intent.putExtra("DURATION", consultationDuration)
                    intent.putExtra("IS_READ_ONLY", false)
                    startActivity(intent)
                }
            }

            else -> {
                binding.btnViewReview.visibility = View.GONE
            }
        }
    }

    private fun startSessionTimer() {
        Log.d(TAG, "=== TIMER STARTED ===")
        startSessionTimerWithRemaining(SESSION_DURATION)
    }

    private fun startSessionTimerWithRemaining(remainingTime: Long) {
        Log.d(TAG, "=== TIMER STARTED WITH REMAINING: ${remainingTime / 1000} detik ===")

        timer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                if (millisUntilFinished <= WARNING_TIME && !isSessionEnding) {
                    showWarningDialog()
                }
            }

            override fun onFinish() {
                Log.d(TAG, "=== TIMER FINISHED ===")
                autoEndSession()
            }
        }.start()
    }

    private fun showWarningDialog() {
        if (isConsultationCompleted || isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("⏰ Sesi Akan Berakhir")
            .setMessage("Sesi konsultasi akan berakhir dalam 10 detik. Pastikan Anda sudah menyelesaikan konsultasi.")
            .setPositiveButton("Selesaikan Sekarang") { _, _ ->
                completeConsultation()
            }
            .setNegativeButton("Perpanjang Sesi") { _, _ ->
                resetSessionTimer()
                isSessionEnding = false
                Toast.makeText(this, "Sesi diperpanjang 1 menit", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun resetSessionTimer() {
        timer?.cancel()
        sessionStartTime = System.currentTimeMillis()
        isSessionEnding = false

        val updates = hashMapOf<String, Any>(
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("chat_rooms").document(chatRoomId)
            .update(updates)
            .addOnCompleteListener {
                startSessionTimer()
            }
    }

    private fun autoEndSession() {
        Log.d(TAG, "=== autoEndSession() DIPANGGIL ===")
        Log.d(TAG, "chatRoomId: $chatRoomId")
        Log.d(TAG, "isConsultationCompleted: $isConsultationCompleted")

        if (chatRoomId.isNotEmpty() && !isConsultationCompleted) {
            Log.d(TAG, "=== AUTO END SESSION EKSEKUSI ===")
            isSessionEnding = true
            sendMessage("⏰ Sesi konsultasi telah berakhir otomatis (batas waktu 1 menit)", isAi = false)

            Handler(Looper.getMainLooper()).postDelayed({
                Log.d(TAG, "=== DELAY SELESAI, PANGGIL completeConsultation() ===")
                completeConsultation()
            }, 1000)
        } else {
            Log.d(TAG, "=== autoEndSession() TIDAK JALAN ===")
            Log.d(TAG, "chatRoomId kosong? ${chatRoomId.isEmpty()}")
            Log.d(TAG, "isConsultationCompleted: $isConsultationCompleted")
        }
    }

    private fun fetchGeminiApiKey() {
        db.collection("app_config").document("gemini").get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Failed to fetch API key", task.exception)
                    Toast.makeText(
                        this,
                        R.string.error_fetch_config,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnCompleteListener
                }

                val document = task.result
                if (document == null || !document.exists()) {
                    Toast.makeText(
                        this,
                        R.string.error_api_key_not_found,
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnCompleteListener
                }

                apiKey = document.getString("apiKey") ?: ""

                if (apiKey.isNotEmpty() && apiKey.length >= 20) {
                    geminiService = GeminiApiService(apiKey)
                    Log.d(TAG, "Gemini API Service initialized successfully")
                } else {
                    Toast.makeText(
                        this,
                        R.string.error_api_key_not_found,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setupEndButton() {
        val currentUid = auth.currentUser?.uid ?: ""
        val btnEnd = binding.btnEndConsultation

        if (currentUid == doctorId) {
            btnEnd.visibility = View.VISIBLE
            btnEnd.setOnClickListener {
                if (!isConsultationCompleted) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_end_title)
                        .setMessage(R.string.dialog_end_message)
                        .setPositiveButton(R.string.dialog_yes) { _, _ -> completeConsultation() }
                        .setNegativeButton(R.string.dialog_no, null)
                        .show()
                }
            }
        } else {
            btnEnd.visibility = View.GONE
        }
    }

    private fun completeConsultation() {
        Log.d(TAG, "=== completeConsultation() DIPANGGIL ===")
        Log.d(TAG, "isConsultationCompleted: $isConsultationCompleted")
        Log.d(TAG, "isSessionEnding: $isSessionEnding")
        Log.d(TAG, "chatRoomId: $chatRoomId")
        Log.d(TAG, "sessionStartTime: $sessionStartTime")

        if (isConsultationCompleted) {
            Log.d(TAG, "=== Konsultasi sudah selesai, RETURN ===")
            return
        }

        if (sessionStartTime == 0L) {
            Log.d(TAG, "=== sessionStartTime 0, ambil dari Firestore ===")

            db.collection("chat_rooms")
                .document(chatRoomId)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val doc = task.result

                        if (doc != null) {
                            val createdAt = doc.getTimestamp("createdAt")

                            if (createdAt != null) {
                                sessionStartTime = createdAt.toDate().time
                                Log.d(TAG, "sessionStartTime dari Firestore: $sessionStartTime")
                            } else {
                                sessionStartTime = System.currentTimeMillis()
                                Log.d(TAG, "sessionStartTime fallback: $sessionStartTime")
                            }
                        } else {
                            sessionStartTime = System.currentTimeMillis()
                            Log.d(TAG, "sessionStartTime fallback: $sessionStartTime")
                        }
                    } else {
                        sessionStartTime = System.currentTimeMillis()
                        Log.d(TAG, "sessionStartTime fallback: $sessionStartTime")
                    }

                    completeConsultation()
                }

            return
        }

        isConsultationCompleted = true
        isSessionEnding = true
        timer?.cancel()

        Log.d(TAG, "=== Timer dibatalkan ===")

        val endTime = System.currentTimeMillis()
        val durationInMillis = endTime - sessionStartTime
        val durationInMinutes = (durationInMillis / 1000 / 60).toInt()

        val finalDuration = if (durationInMinutes < 1) {
            val seconds = (durationInMillis / 1000).toInt()
            if (seconds < 60) 1 else seconds / 60
        } else {
            durationInMinutes
        }

        consultationDuration = finalDuration

        Log.d(TAG, "endTime: $endTime")
        Log.d(TAG, "sessionStartTime: $sessionStartTime")
        Log.d(TAG, "durationInMillis: $durationInMillis")
        Log.d(TAG, "durationInMinutes: $durationInMinutes")
        Log.d(TAG, "finalDuration: $finalDuration")

        val updates = hashMapOf<String, Any>(
            "chatStatus" to "completed",
            "endedAt" to FieldValue.serverTimestamp(),
            "duration" to finalDuration.toLong()
        )

        Log.d(TAG, "=== UPDATING FIRESTORE ===")
        Log.d(TAG, "updates: $updates")

        db.collection("chat_rooms")
            .document(chatRoomId)
            .update(updates)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "=== UPDATE FAILED ===")
                    Log.e(
                        TAG,
                        "Failed to end consultation: ${task.exception?.message}",
                        task.exception
                    )

                    Toast.makeText(
                        this,
                        "Gagal mengakhiri sesi: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    isConsultationCompleted = false
                    isSessionEnding = false
                    return@addOnCompleteListener
                }

                Log.d(TAG, "=== UPDATE SUCCESS ===")

                Toast.makeText(
                    this,
                    "Konsultasi selesai! Durasi: $finalDuration menit",
                    Toast.LENGTH_SHORT
                ).show()

                setChatReadOnly()
            }
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val currentUid = auth.currentUser?.uid ?: ""

        if (currentUid == doctorId) {
            binding.tvChatName.text = if (ownerName.isNotEmpty()) ownerName else "Pemilik Hewan"
        } else {
            binding.tvChatName.text = if (doctorName.isNotEmpty()) doctorName else "Dokter Hewan"
        }

        if (!isConsultationCompleted) {
            binding.tvChatSubtitle.text = "Konsultasi Berlangsung"
            binding.tvChatSubtitle.setTextColor(android.graphics.Color.parseColor("#D4E8F2"))
            binding.tvChatSubtitle.visibility = View.VISIBLE
        }
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
                sendMessage(text, isAi = false)
                binding.etMessage.setText("")
            }
        }

        binding.btnAiSuggest.setOnClickListener {
            showAiSuggestions()
        }
    }

    private fun sendMessage(text: String, isAi: Boolean = false) {
        if (isChatReadOnly) {
            Toast.makeText(this, "Konsultasi sudah selesai, tidak dapat mengirim pesan", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return
        val senderName = if (uid == doctorId) {
            getString(R.string.chat_name_doctor, doctorName)
        } else {
            ownerName
        }

        val message = hashMapOf<String, Any>(
            "senderId" to uid,
            "senderName" to senderName,
            "text" to text,
            "timestamp" to FieldValue.serverTimestamp(),
            "isAiGenerated" to isAi
        )

        db.collection("chat_rooms").document(chatRoomId)
            .collection("messages")
            .add(message)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send message", e)
                Toast.makeText(
                    this,
                    R.string.error_send_message,
                    Toast.LENGTH_SHORT
                ).show()
            }

        val updateMap = hashMapOf<String, Any>(
            "lastMessage" to text,
            "lastMessageTime" to FieldValue.serverTimestamp()
        )

        db.collection("chat_rooms").document(chatRoomId)
            .update(updateMap)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update last message", e)
            }
    }

    private fun listenToMessages() {
        if (chatRoomId.isEmpty()) {
            Log.e(TAG, "ChatRoomId is empty")
            return
        }

        if (chatAdapter == null) {
            Log.e(TAG, "ChatAdapter is null, skipping listenToMessages")
            return
        }

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
                if (error != null) {
                    Log.e(TAG, "Listen failed", error)
                    Toast.makeText(
                        this,
                        getString(R.string.error_load_messages, error.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    Log.w(TAG, "Snapshot is null")
                    return@addSnapshotListener
                }

                val adapter = chatAdapter
                if (adapter == null) {
                    Log.e(TAG, "ChatAdapter is null, cannot update messages")
                    return@addSnapshotListener
                }

                val messages = mutableListOf<Message>()
                messages.add(welcomeMsg)
                messages.addAll(snapshots.documents.mapNotNull { doc ->
                    try {
                        Message(
                            messageId = doc.id,
                            senderId = doc.getString("senderId") ?: "",
                            senderName = doc.getString("senderName") ?: "",
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getDate("timestamp") ?: Date(),
                            isAiGenerated = doc.getBoolean("isAiGenerated") ?: false
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                        null
                    }
                })

                adapter.updateMessages(messages)

                if (messages.isNotEmpty()) {
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun showAiSuggestions() {
        val service = geminiService
        if (service == null || apiKey.isEmpty()) {
            showFallbackSuggestions(getString(R.string.error_ai_not_ready))
            return
        }

        val adapter = chatAdapter
        if (adapter == null) {
            showFallbackSuggestions("Chat tidak tersedia")
            return
        }

        val rawMessages = adapter.getMessages()
        val realChatMessages = rawMessages.filter { it.senderId != "system" }

        if (realChatMessages.isEmpty()) {
            val welcomeSuggestions = if (userRole == "DOCTOR") {
                listOf(
                    "Halo! Ada yang bisa saya bantu dengan anabul Anda?",
                    "Selamat siang, bagaimana kondisi hewan peliharaan Anda hari ini?",
                    "Halo, silakan ceritakan keluhan gejalanya ya."
                )
            } else {
                listOf(
                    "Halo dok, saya mau konsultasi tentang hewan saya",
                    "Selamat siang dok, hewan saya sedang sakit",
                    "Halo dok, saya butuh bantuan untuk anabul saya"
                )
            }
            displayBottomSheet(welcomeSuggestions)
            return
        }

        val lastMessages = realChatMessages.takeLast(5)
        val conversationHistory = StringBuilder()

        for (msg in lastMessages) {
            val role = if (msg.senderId == doctorId) "Dokter" else "Pemilik Hewan"
            conversationHistory.append("$role: ${msg.text}\n")
        }

        val isFollowUp = detectFollowUp(realChatMessages)
        val intentType = detectIntent(realChatMessages.last().text)

        val finalPrompt = buildPrompt(
            conversationHistory = conversationHistory.toString(),
            userRole = userRole,
            isFollowUp = isFollowUp,
            intentType = intentType,
            petName = petName
        )

        lifecycleScope.launch {
            try {
                Toast.makeText(
                    this@ChatActivity,
                    "AI sedang menganalisis...",
                    Toast.LENGTH_SHORT
                ).show()

                val responseText = withContext(Dispatchers.IO) {
                    service.generateContent(finalPrompt)
                }

                Log.d(TAG, "AI Response: $responseText")

                if (!responseText.isNullOrBlank()) {
                    val suggestionsList = responseText
                        .split(Regex("[;\\n]"))
                        .map { it.trim() }
                        .filter {
                            it.isNotEmpty() &&
                                    !it.startsWith("-") &&
                                    !it.matches(Regex("^\\d+\\..*")) &&
                                    it.length > 5
                        }
                        .toMutableList()

                    while (suggestionsList.size < 3) {
                        suggestionsList.add(
                            if (userRole == "DOCTOR") {
                                "Apakah ada keluhan lain yang dirasakan?"
                            } else {
                                "Dok, apa yang harus saya lakukan selanjutnya?"
                            }
                        )
                    }

                    val finalSuggestions = suggestionsList.take(3)

                    withContext(Dispatchers.Main) {
                        displayBottomSheet(finalSuggestions)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showFallbackSuggestions("Respon AI kosong, silakan coba lagi")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating AI suggestions: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showFallbackSuggestions("Gagal memuat AI: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun detectFollowUp(messages: List<Message>): Boolean {
        if (messages.size < 3) return false

        val lastSender = messages.last().senderId
        if (lastSender == doctorId) return false

        val lastThreeMessages = messages.takeLast(3)
        val hasQuestion = lastThreeMessages.any { it.text.contains("?") }
        val hasShortAnswer = lastThreeMessages.last().text.split(" ").size < 5

        return hasQuestion && hasShortAnswer
    }

    private fun detectIntent(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("makan") || lowerText.contains("minum") -> "FOOD_DRINK"
            lowerText.contains("demam") || lowerText.contains("panas") -> "FEVER"
            lowerText.contains("muntah") || lowerText.contains("diare") -> "VOMIT_DIARRHEA"
            lowerText.contains("luka") || lowerText.contains("berdarah") -> "WOUND"
            lowerText.contains("gatal") || lowerText.contains("kudis") -> "ITCHING"
            lowerText.contains("batuk") || lowerText.contains("bersin") -> "COUGH_SNEEZE"
            lowerText.contains("lemas") || lowerText.contains("malas") -> "LETHARGY"
            lowerText.contains("obat") || lowerText.contains("vitamin") -> "MEDICATION"
            lowerText.contains("vaksin") -> "VACCINE"
            else -> "GENERAL"
        }
    }

    private fun buildPrompt(
        conversationHistory: String,
        userRole: String,
        isFollowUp: Boolean,
        intentType: String,
        petName: String
    ): String {
        val followUpInstruction = if (isFollowUp) {
            "Ini adalah percakapan lanjutan. User telah memberikan informasi tambahan. " +
                    "Berikan saran yang lebih spesifik dan mendalam berdasarkan jawaban sebelumnya."
        } else {
            "Ini adalah percakapan awal. Berikan saran umum yang baik untuk memulai."
        }

        val intentInstruction = when (intentType) {
            "FOOD_DRINK" -> "Fokus pada masalah makan dan minum hewan."
            "FEVER" -> "Fokus pada demam dan suhu tubuh hewan."
            "VOMIT_DIARRHEA" -> "Fokus pada masalah pencernaan."
            "WOUND" -> "Fokus pada luka dan perawatan."
            "ITCHING" -> "Fokus pada masalah kulit dan gatal."
            "COUGH_SNEEZE" -> "Fokus pada masalah pernapasan."
            "LETHARGY" -> "Fokus pada kondisi lemas dan kurang energi."
            "MEDICATION" -> "Fokus pada obat dan vitamin."
            "VACCINE" -> "Fokus pada vaksinasi."
            else -> "Berikan saran umum yang sesuai."
        }

        return if (userRole == "DOCTOR") {
            """
            Anda adalah asisten AI untuk dokter hewan. Hewan yang dikonsultasikan adalah $petName.
            
            Berikut riwayat percakapan:
            $conversationHistory
            
            $followUpInstruction
            $intentInstruction
            
            Berikan 3 saran balasan untuk DIPAKAI OLEH DOKTER saat merespons pasien.
            Saran harus:
            1. Profesional dan medis
            2. Ramah dan empati
            3. Memberikan solusi atau saran medis
            4. Spesifik sesuai dengan kondisi yang dibicarakan
            
            Format: "saran1; saran2; saran3"
            Hanya output 3 kalimat, tanpa teks lain.
            """.trimIndent()
        } else {
            """
            Anda adalah asisten AI untuk pemilik hewan. Hewan yang dikonsultasikan adalah $petName.
            
            Berikut riwayat percakapan:
            $conversationHistory
            
            $followUpInstruction
            $intentInstruction
            
            Berikan 3 saran pertanyaan untuk DIAJUKAN OLEH PASIEN ke dokter.
            Saran harus:
            1. Pertanyaan tentang kondisi hewan
            2. Pertanyaan tentang perawatan
            3. Pertanyaan tentang gejala
            4. Spesifik sesuai dengan kondisi yang dibicarakan
            
            Format: "pertanyaan1; pertanyaan2; pertanyaan3"
            Hanya output 3 kalimat, tanpa teks lain.
            """.trimIndent()
        }
    }

    private fun showFallbackSuggestions(errorMessage: String = "") {
        if (errorMessage.isNotEmpty()) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            Log.e(TAG, "Fallback suggestions triggered: $errorMessage")
        }

        val fallback = if (userRole == "DOCTOR") {
            listOf(
                "Bisa ceritakan lebih detail tentang gejala yang dialami?",
                "Sudah berapa lama anabul mengalami keluhan ini?",
                "Apakah nafsu makan dan minum anabul masih baik?"
            )
        } else {
            listOf(
                "Dok, bagaimana kondisi anabul saya?",
                "Apa yang harus saya lakukan selanjutnya dok?",
                "Apakah perlu dibawa ke klinik dok?"
            )
        }
        displayBottomSheet(fallback)
    }

    private fun displayBottomSheet(suggestions: List<String>) {
        try {
            val dialog = BottomSheetDialog(this)
            val view = LayoutInflater.from(this).inflate(
                R.layout.bottom_sheet_ai_suggest,
                null,
                false
            )
            dialog.setContentView(view)

            val tvSuggest1 = view.findViewById<TextView>(R.id.tv_suggest_1)
            val tvSuggest2 = view.findViewById<TextView>(R.id.tv_suggest_2)
            val tvSuggest3 = view.findViewById<TextView>(R.id.tv_suggest_3)

            tvSuggest1.text = suggestions.getOrNull(0)
                ?: getString(R.string.default_suggestion_1)
            tvSuggest2.text = suggestions.getOrNull(1)
                ?: getString(R.string.default_suggestion_2)
            tvSuggest3.text = suggestions.getOrNull(2)
                ?: getString(R.string.default_suggestion_3)

            val clickListener = View.OnClickListener { v ->
                val text = (v as TextView).text.toString()
                if (text.isNotEmpty()) {
                    sendMessage(text, isAi = true)
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        this,
                        R.string.error_suggestion_invalid,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            tvSuggest1.setOnClickListener(clickListener)
            tvSuggest2.setOnClickListener(clickListener)
            tvSuggest3.setOnClickListener(clickListener)

            dialog.setOnDismissListener {
                Log.d(TAG, "Bottom sheet dismissed")
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying bottom sheet", e)
            Toast.makeText(
                this,
                R.string.error_display_suggestions,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.remove()
        messagesListener = null
        timer?.cancel()
        Log.d(TAG, "ChatActivity destroyed, listeners removed")
    }

    override fun onResume() {
        super.onResume()
        if (messagesListener == null && chatRoomId.isNotEmpty()) {
            listenToMessages()
        }
    }
}