package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.chat.ChatActivity // ◄── TAMBAH IMPORT
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class PaymentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Variabel global untuk menyimpan data yang dikirim dari halaman detail
    private var doctorId = ""
    private var doctorName = ""
    private var consultationFee = 50000
    private var currentUserName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // 🔄 FIX KUNCI: Menyamakan key intent dengan DoctorDetailActivity.kt
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dokter"
        consultationFee = intent.getIntExtra("FEE", 50000)

        // Toolbar back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarPayment)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pembayaran"

        // Update teks nama dokter di ringkasan
        // 1. Update nama layanan (Biarkan statis atau isi jenis layanan)
        val tvServiceName = findViewById<TextView>(R.id.tvServiceName)
        tvServiceName.text = "Konsultasi Online"

// 2. 🔄 UPDATE: Isi nama dokter ke tempat yang benar di XML
        val tvDoctorNamePayment = findViewById<TextView>(R.id.tvDoctorNamePayment)
        if (tvDoctorNamePayment != null) {
            tvDoctorNamePayment.text = "drh. $doctorName"
        }

// 3. 🔄 FIX ERROR: Ganti tvTotalPayment menjadi tvTotalAmount sesuai XML
        val tvTotalAmount = findViewById<TextView>(R.id.tvTotalAmount)
        if (tvTotalAmount != null) {
            // Format menjadi Rupiah yang rapi
            tvTotalAmount.text = "Rp ${String.format("%,d", consultationFee).replace(',', '.')}"
        }

        // Ambil nama user aktif terlebih dahulu untuk modal chat_rooms nanti
        loadCurrentUserInfo()

        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

        // Klik tombol "SAYA SUDAH BAYAR"
        btnConfirmPayment.setOnClickListener {
            verifyAndCreateChatRoom()
        }
    }

    private fun loadCurrentUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                currentUserName = doc.getString("name") ?: "Pemilik Hewan"
            }
    }

    // 🚀 TUGAS PROGRAMMER 4: Membuat data room chat asli di Cloud Firestore
    private fun verifyAndCreateChatRoom() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)
        val currentUserId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE
        btnConfirmPayment.isEnabled = false
        btnConfirmPayment.text = "Memverifikasi..."

        // Membuat ID unik untuk room chat (Gabungan UID User dan UID Dokter)
        val chatRoomId = "${currentUserId}_${doctorId}"

        // Struktur data yang WAJIB sama dengan bacaan di AppointmentFragment milik Dokter
        val chatRoomData = hashMapOf(
                "chatRoomId" to chatRoomId,
        "ownerId" to currentUserId,
        "ownerName" to currentUserName,
        "doctorId" to doctorId,
        "doctorName" to doctorName,
        "petName" to "Anabul",
        "petType" to "Kucing/Anjing",
        "paymentStatus" to "success",
        "chatStatus" to "active",

        // --- TAMBAHKAN DUA BARIS INI ---
        "startTime" to System.currentTimeMillis(), // Waktu mulai sesi (dalam milidetik)
        "durationMinutes" to 2,                   // Durasi sesi 30 menit
        // -------------------------------

        "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        // Simpan ke Firestore
        db.collection("chat_rooms").document(chatRoomId)
            .set(chatRoomData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                onPaymentVerified(chatRoomId)
            }
            .addOnFailureListener {
                onPaymentFailed()
            }
    }

    private fun onPaymentVerified(chatRoomId: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

        progressBar.visibility = View.GONE
        btnConfirmPayment.text = "✓ Pembayaran Dikonfirmasi"
        btnConfirmPayment.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#1B5E20")
        )

        Toast.makeText(this, "Pembayaran berhasil! Konsultasi dimulai.", Toast.LENGTH_LONG).show()

        // 🔄 SELESAI: Langsung arahkan User masuk ke halaman ChatActivity
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("CHAT_ROOM_ID", chatRoomId)
            putExtra("DOCTOR_ID", doctorId)
            putExtra("DOCTOR_NAME", doctorName)
        }
        startActivity(intent)
        finish() // Tutup halaman payment agar jika di-back tidak kembali ke form bayar
    }

    private fun onPaymentFailed() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

        progressBar.visibility = View.GONE
        btnConfirmPayment.isEnabled = true
        btnConfirmPayment.text = "SAYA SUDAH BAYAR"

        Toast.makeText(this, "Verifikasi gagal. Coba lagi.", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}