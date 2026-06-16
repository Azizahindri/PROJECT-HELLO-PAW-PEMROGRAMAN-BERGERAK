package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Ambil data dari intent
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dokter"
        consultationFee = intent.getIntExtra("FEE", 50000)
        petName = intent.getStringExtra("PET_NAME") ?: "Anabul"

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarPayment)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pembayaran"

        // Update UI
        val tvServiceName = findViewById<TextView>(R.id.tvServiceName)
        tvServiceName.text = getString(R.string.consultation_online)

        val tvDoctorNamePayment = findViewById<TextView>(R.id.tvDoctorNamePayment)
        tvDoctorNamePayment.text = getString(R.string.doctor_name_prefix, doctorName)

        // ❌ HAPUS ATAU COMMENT INI KARENA TIDAK ADA DI LAYOUT
        // val tvPetNamePayment = findViewById<TextView>(R.id.tvPetNamePayment)
        // tvPetNamePayment.text = "Hewan: $petName"

        val tvTotalAmount = findViewById<TextView>(R.id.tvTotalAmount)
        tvTotalAmount.text = String.format(Locale.getDefault(), "Rp %s", String.format("%,d", consultationFee).replace(',', '.'))

        // Ambil nama user
        loadCurrentUserInfo()

        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

        btnConfirmPayment.setOnClickListener {
            verifyAndCreateChatRoom()
        }
    }

    private fun loadCurrentUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: getString(R.string.default_owner_name)
            }
    }

    private fun verifyAndCreateChatRoom() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)
        val currentUserId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        btnConfirmPayment.isEnabled = false
        btnConfirmPayment.text = getString(R.string.verifying)

        // Buat CHAT ROOM ID BARU
        val chatRoomId = db.collection("chat_rooms").document().id

        val chatRoomData = hashMapOf(
            "chatRoomId" to chatRoomId,
            "ownerId" to currentUserId,
            "ownerName" to currentUserName,
            "doctorId" to doctorId,
            "doctorName" to doctorName,
            "petName" to petName,
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
                onPaymentVerified(chatRoomId)
            }
            .addOnFailureListener { e ->
                onPaymentFailed(e.message)
            }
    }

    private fun onPaymentVerified(chatRoomId: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

        progressBar.visibility = View.GONE
        btnConfirmPayment.text = getString(R.string.payment_confirmed)
        btnConfirmPayment.backgroundTintList = android.content.res.ColorStateList.valueOf(
            "#1B5E20".toColorInt()
        )

        Toast.makeText(this, R.string.payment_success, Toast.LENGTH_LONG).show()

        // Kirim result ke DoctorDetailActivity
        val resultIntent = Intent()
        resultIntent.putExtra("PAYMENT_SUCCESS", true)
        setResult(RESULT_OK, resultIntent)

        // Langsung ke ChatActivity
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("CHAT_ROOM_ID", chatRoomId)
            putExtra("DOCTOR_ID", doctorId)
            putExtra("DOCTOR_NAME", doctorName)
            putExtra("OWNER_ID", auth.currentUser?.uid ?: "")
            putExtra("PET_NAME", petName)
        }
        startActivity(intent)
        finish()
    }

    private fun onPaymentFailed(errorMessage: String?) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

        progressBar.visibility = View.GONE
        btnConfirmPayment.isEnabled = true
        btnConfirmPayment.text = getString(R.string.pay_button)

        Toast.makeText(this, getString(R.string.payment_failed, errorMessage ?: "Coba lagi"), Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}