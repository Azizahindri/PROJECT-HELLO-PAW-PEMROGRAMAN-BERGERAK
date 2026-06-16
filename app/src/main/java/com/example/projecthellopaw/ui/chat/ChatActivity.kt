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
    private lateinit var chatAdapter: ChatAdapter

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

    companion object {
        private const val TAG = "ChatActivity"
        private const val SESSION_DURATION = 1 * 60 * 1000L
        private const val WARNING_TIME = 10 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val currentUid = auth.currentUser?.uid ?: ""
        if (currentUid != doctorId && ownerName.isEmpty()) {
            db.collection("users").document(currentUid).get()
                .addOnSuccessListener { doc ->
                    ownerName = doc.getString("name") ?: getString(R.string.default_owner_name)
                    setupToolbar()
                }
                .addOnFailureListener {
                    ownerName = getString(R.string.default_owner_name)
                }
        }

        setupToolbar()
        setupChat()
        setupInputArea()
        listenToMessages()
        setupEndButton()
        fetchGeminiApiKey()

        // ✅ CEK STATUS CHAT + SISA WAKTU
        checkChatStatusAndTime()
    }

    private fun checkChatStatusAndTime() {
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener { document ->
                val chatStatus = document.getString("chatStatus")
                hasReview = document.getBoolean("hasReview") ?: false

                // ✅ JIKA CHAT SUDAH SELESAI
                if (chatStatus == "completed") {
                    Log.d(TAG, "=== CHAT SUDAH SELESAI ===")
                    timer?.cancel()
                    binding.tvTimer.visibility = View.GONE
                    setChatReadOnly()
                    return@addOnSuccessListener
                }

                // ✅ JIKA CHAT MASIH AKTIF
                val createdAt = document.getTimestamp("createdAt")

                if (createdAt == null) {
                    db.collection("chat_rooms").document(chatRoomId)
                        .update("createdAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener {
                            sessionStartTime = System.currentTimeMillis()
                            startSessionTimer()
                        }
                        .addOnFailureListener {
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
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check session time", e)
                sessionStartTime = System.currentTimeMillis()
                startSessionTimer()
            }
    }

    private fun setChatReadOnly() {
        isChatReadOnly = true
        isConsultationCompleted = true

        // ✅ HENTIKAN TIMER
        timer?.cancel()

        // ✅ SEMBUNYIKAN TIMER
        binding.tvTimer.visibility = View.GONE

        // Nonaktifkan input
        binding.etMessage.isEnabled = false
        binding.btnSend.isEnabled = false
        binding.btnSend.alpha = 0.5f
        binding.etMessage.hint = "Konsultasi telah selesai"
        binding.btnAiSuggest.visibility = View.GONE

        // Sembunyikan tombol end consultation
        binding.btnEndConsultation.visibility = View.GONE

        // Tampilkan tombol review
        binding.btnViewReview.visibility = View.VISIBLE
        if (hasReview) {
            binding.btnViewReview.text = "Lihat Review"
            binding.btnViewReview.setOnClickListener {
                Toast.makeText(this, "Review sudah diberikan", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.btnViewReview.text = "Beri Review"
            binding.btnViewReview.setOnClickListener {
                val intent = Intent(this, ReviewActivity::class.java)
                intent.putExtra("CHAT_ROOM_ID", chatRoomId)
                intent.putExtra("DOCTOR_ID", doctorId)
                intent.putExtra("DOCTOR_NAME", doctorName)
                intent.putExtra("OWNER_ID", ownerId)
                intent.putExtra("PET_NAME", petName)
                intent.putExtra("DURATION", 0)
                startActivity(intent)
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

        db.collection("chat_rooms").document(chatRoomId)
            .update("createdAt", FieldValue.serverTimestamp())
            .addOnSuccessListener {
                startSessionTimer()
            }
            .addOnFailureListener {
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
            .addOnSuccessListener { document ->
                apiKey = document.getString("apiKey") ?: ""

                if (apiKey.isNotEmpty() && apiKey.length >= 20) {
                    geminiService = GeminiApiService(apiKey)
                    Log.d(TAG, "Gemini API Service initialized successfully")
                    // ✅ HAPUS TOAST DI SINI
                } else {
                    Toast.makeText(
                        this,
                        R.string.error_api_key_not_found,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch API key", e)
                Toast.makeText(
                    this,
                    R.string.error_fetch_config,
                    Toast.LENGTH_SHORT
                ).show()
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

        isConsultationCompleted = true
        isSessionEnding = true
        timer?.cancel()
        Log.d(TAG, "=== Timer dibatalkan ===")

        val endTime = System.currentTimeMillis()
        val durationInMinutes = ((endTime - sessionStartTime) / 1000 / 60).toInt()

        Log.d(TAG, "endTime: $endTime")
        Log.d(TAG, "durationInMinutes: $durationInMinutes")

        val updates = hashMapOf(
            "chatStatus" to "completed",
            "endedAt" to FieldValue.serverTimestamp(),
            "duration" to durationInMinutes.toLong()
        )

        Log.d(TAG, "=== UPDATING FIRESTORE ===")
        Log.d(TAG, "updates: $updates")

        db.collection("chat_rooms").document(chatRoomId)
            .update(updates as Map<String, Any>)
            .addOnSuccessListener {
                Log.d(TAG, "=== UPDATE SUCCESS ===")

                Toast.makeText(
                    this,
                    "Konsultasi selesai! Durasi: ${durationInMinutes} menit",
                    Toast.LENGTH_SHORT
                ).show()

                // ✅ SET CHAT READ-ONLY
                setChatReadOnly()

                // ✅ UPDATE hasReview
                db.collection("chat_rooms").document(chatRoomId)
                    .get()
                    .addOnSuccessListener { doc ->
                        hasReview = doc.getBoolean("hasReview") ?: false
                        if (hasReview) {
                            binding.btnViewReview.text = "Lihat Review"
                        } else {
                            binding.btnViewReview.text = "Beri Review"
                            binding.btnViewReview.setOnClickListener {
                                val intent = Intent(this, ReviewActivity::class.java)
                                intent.putExtra("CHAT_ROOM_ID", chatRoomId)
                                intent.putExtra("DOCTOR_ID", doctorId)
                                intent.putExtra("DOCTOR_NAME", doctorName)
                                intent.putExtra("OWNER_ID", ownerId)
                                intent.putExtra("PET_NAME", petName)
                                intent.putExtra("DURATION", durationInMinutes)
                                startActivity(intent)
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "=== UPDATE FAILED ===")
                Log.e(TAG, "Failed to end consultation: ${e.message}", e)

                Toast.makeText(
                    this,
                    "Gagal mengakhiri sesi: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                isConsultationCompleted = false
                isSessionEnding = false
                Log.d(TAG, "=== FLAG DI-RESET ===")
                Log.d(TAG, "isConsultationCompleted: $isConsultationCompleted")
                Log.d(TAG, "isSessionEnding: $isSessionEnding")
            }
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val currentUid = auth.currentUser?.uid ?: ""
        if (currentUid == doctorId) {
            binding.tvChatName.text = ownerName.ifEmpty { getString(R.string.default_owner_name) }
            binding.tvChatSubtitle.text = getString(R.string.chat_subtitle_doctor, petName)
        } else {
            binding.tvChatName.text = getString(R.string.chat_name_doctor, doctorName)
            binding.tvChatSubtitle.text = getString(R.string.chat_subtitle_patient)
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
        // ✅ CEK APAKAH READ-ONLY
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

        val message = hashMapOf(
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

        db.collection("chat_rooms").document(chatRoomId)
            .update(
                "lastMessage", text,
                "lastMessageTime", FieldValue.serverTimestamp()
            )
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update last message", e)
            }
    }

    private fun listenToMessages() {
        if (chatRoomId.isEmpty()) {
            Log.e(TAG, "ChatRoomId is empty")
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

                chatAdapter.updateMessages(messages)

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

        val rawMessages = chatAdapter.getMessages()
        val realChatMessages = rawMessages.filter { it.senderId != "system" }

        if (realChatMessages.isEmpty()) {
            val welcomeSuggestions = listOf(
                getString(R.string.suggestion_welcome_1),
                getString(R.string.suggestion_welcome_2),
                getString(R.string.suggestion_welcome_3)
            )
            displayBottomSheet(welcomeSuggestions)
            return
        }

        val lastMessages = realChatMessages.takeLast(5)
        val conversationHistory = StringBuilder()

        for (msg in lastMessages) {
            val role = if (msg.senderId == doctorId) {
                getString(R.string.role_doctor)
            } else {
                getString(R.string.role_owner)
            }
            conversationHistory.append("$role: ${msg.text}\n")
        }

        val finalPrompt = """
        Anda adalah asisten AI untuk dokter hewan. Berikut riwayat percakapan:
        
        $conversationHistory
        
        Berikan 3 saran balasan untuk dokter. Format: "saran1; saran2; saran3"
        Hanya output 3 kalimat, tanpa teks lain.
        """.trimIndent()

        lifecycleScope.launch {
            try {
                Toast.makeText(
                    this@ChatActivity,
                    R.string.ai_analyzing,
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
                        suggestionsList.add(getString(R.string.suggestion_fallback))
                    }

                    val finalSuggestions = suggestionsList.take(3)

                    withContext(Dispatchers.Main) {
                        displayBottomSheet(finalSuggestions)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showFallbackSuggestions(getString(R.string.error_ai_empty_response))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating AI suggestions: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showFallbackSuggestions(getString(R.string.error_ai_generic, e.localizedMessage ?: "Unknown error"))
                }
            }
        }
    }

    private fun showFallbackSuggestions(errorMessage: String = "") {
        if (errorMessage.isNotEmpty()) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            Log.e(TAG, "Fallback suggestions triggered: $errorMessage")
        }

        val fallback = listOf(
            getString(R.string.symptom_question_1),
            getString(R.string.symptom_question_2),
            getString(R.string.symptom_question_3)
        )
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