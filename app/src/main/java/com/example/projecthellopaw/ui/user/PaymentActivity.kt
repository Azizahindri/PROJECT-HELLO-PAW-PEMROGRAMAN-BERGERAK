package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.chat.ChatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var doctorId = ""
    private var doctorName = ""
    private var consultationFee = 50000
    private var currentUserName = ""
    private var petName = "Anabul"
    private var isProcessing = false
    private var existingChatId: String? = null

    companion object {
        private const val TAG = "PaymentActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_payment)

            doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
            doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dokter"
            consultationFee = intent.getIntExtra("FEE", 50000)
            petName = intent.getStringExtra("PET_NAME") ?: "Anabul"

            Log.d(TAG, "doctorId: $doctorId")
            Log.d(TAG, "doctorName: $doctorName")
            Log.d(TAG, "consultationFee: $consultationFee")
            Log.d(TAG, "petName: $petName")

            if (doctorId.isEmpty()) {
                Toast.makeText(this, "Data dokter tidak valid", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            try {
                val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarPayment)
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Ringkasan Pesanan"
            } catch (e: Exception) {
                Log.e(TAG, "Toolbar error: ${e.message}", e)
            }

            try {
                val tvServiceName = findViewById<TextView>(R.id.tvServiceName)
                tvServiceName.text = "Konsultasi Online"

                val tvDoctorNamePayment = findViewById<TextView>(R.id.tvDoctorNamePayment)
                tvDoctorNamePayment.text = doctorName

                val tvPetNamePayment = findViewById<TextView>(R.id.tvPetNamePayment)
                tvPetNamePayment.text = petName

                val tvTotalAmount = findViewById<TextView>(R.id.tvTotalAmount)
                tvTotalAmount.text = String.format(Locale.getDefault(), "Rp %s",
                    String.format("%,d", consultationFee).replace(',', '.'))
            } catch (e: Exception) {
                Log.e(TAG, "Error setting text views: ${e.message}", e)
            }

            loadCurrentUserInfo()

            checkExistingActiveChat()

            try {
                val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)
                btnConfirmPayment.setOnClickListener {
                    if (!isProcessing) {
                        if (existingChatId != null) {
                            goToChat(existingChatId!!)
                        } else {
                            verifyAndCreateChatRoom()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Button error: ${e.message}", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "onCreate error: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkExistingActiveChat() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrEmpty()) {
            Log.w(TAG, "User not logged in")
            return
        }

        db.collection("chat_rooms")
            .whereEqualTo("ownerId", currentUserId)
            .whereEqualTo("doctorId", doctorId)
            .whereIn("chatStatus", listOf("active", "waiting", "pending"))
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    existingChatId = documents.documents.first().id
                    Log.d(TAG, "Existing chat found: $existingChatId")

                    try {
                        val cardWarning = findViewById<CardView>(R.id.cardWarning)
                        val tvWarning = findViewById<TextView>(R.id.tvWarningActiveChat)
                        val btnConfirm = findViewById<MaterialButton>(R.id.btnConfirmPayment)
                        val cardQris = findViewById<CardView>(R.id.cardQris)

                        cardWarning.visibility = View.VISIBLE
                        tvWarning.text = "Anda sudah memiliki konsultasi aktif dengan $doctorName"

                        cardQris.visibility = View.GONE

                        btnConfirm.text = "💬 Lanjutkan Konsultasi"
                        btnConfirm.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#FF9800")
                        )

                        Toast.makeText(
                            this,
                            "Anda sudah memiliki konsultasi aktif dengan dokter ini",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating UI for existing chat: ${e.message}", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check existing chat: ${e.message}", e)
            }
    }

    private fun loadCurrentUserInfo() {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            currentUserName = "Pemilik"
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "Pemilik"
                Log.d(TAG, "Current user name: $currentUserName")
            }
            .addOnFailureListener {
                currentUserName = "Pemilik"
                Log.e(TAG, "Failed to load user info")
            }
    }

    private fun verifyAndCreateChatRoom() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrEmpty()) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("chat_rooms")
            .whereEqualTo("ownerId", currentUserId)
            .whereEqualTo("doctorId", doctorId)
            .whereIn("chatStatus", listOf("active", "waiting", "pending"))
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val chatRoomId = documents.documents.first().id
                    Toast.makeText(
                        this,
                        "Konsultasi sudah aktif",
                        Toast.LENGTH_SHORT
                    ).show()
                    goToChat(chatRoomId)
                    return@addOnSuccessListener
                }

                isProcessing = true
                progressBar.visibility = View.VISIBLE
                btnConfirmPayment.isEnabled = false
                btnConfirmPayment.text = "Memproses..."

                val chatRoomId = db.collection("chat_rooms").document().id

                val chatRoomData = hashMapOf(
                    "chatRoomId" to chatRoomId,
                    "ownerId" to currentUserId,
                    "ownerName" to currentUserName.ifEmpty { "Pemilik" },
                    "doctorId" to doctorId,
                    "doctorName" to doctorName,
                    "petName" to petName.ifEmpty { "Anabul" },
                    "petType" to "Hewan",
                    "paymentStatus" to "SUCCESS",
                    "chatStatus" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastMessage" to "",
                    "hasReview" to false,
                    "duration" to 0
                )

                db.collection("chat_rooms").document(chatRoomId)
                    .set(chatRoomData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Chat room created: $chatRoomId")
                        onPaymentVerified(chatRoomId)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to create chat room: ${e.message}", e)
                        onPaymentFailed(e.message)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to verify chat: ${e.message}", e)
                onPaymentFailed(e.message)
            }
    }

    private fun onPaymentVerified(chatRoomId: String) {
        try {
            val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
            val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

            isProcessing = false
            progressBar.visibility = View.GONE
            btnConfirmPayment.isEnabled = true
            btnConfirmPayment.text = "✅ Pembayaran Berhasil"
            btnConfirmPayment.backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#1B5E20")
            )

            Toast.makeText(this, "Pembayaran berhasil! Memulai konsultasi...", Toast.LENGTH_LONG).show()

            val resultIntent = Intent()
            resultIntent.putExtra("PAYMENT_SUCCESS", true)
            setResult(RESULT_OK, resultIntent)

            goToChat(chatRoomId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPaymentVerified: ${e.message}", e)
        }
    }

    private fun onPaymentFailed(errorMessage: String?) {
        try {
            val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
            val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

            isProcessing = false
            progressBar.visibility = View.GONE
            btnConfirmPayment.isEnabled = true
            btnConfirmPayment.text = "✅ SAYA SUDAH BAYAR"

            Toast.makeText(
                this,
                "Pembayaran gagal: ${errorMessage ?: "Coba lagi"}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPaymentFailed: ${e.message}", e)
        }
    }

    private fun goToChat(chatRoomId: String) {
        try {
            val currentUserId = auth.currentUser?.uid
            if (currentUserId.isNullOrEmpty()) {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("CHAT_ROOM_ID", chatRoomId)
                putExtra("DOCTOR_ID", doctorId)
                putExtra("DOCTOR_NAME", doctorName)
                putExtra("OWNER_ID", currentUserId)
                putExtra("PET_NAME", petName)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error going to chat: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}