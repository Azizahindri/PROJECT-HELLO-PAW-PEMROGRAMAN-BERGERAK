package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.projecthellopaw.R

class PaymentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DOCTOR_ID = "extra_doctor_id"
        const val EXTRA_DOCTOR_NAME = "extra_doctor_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        // Ambil data dari DoctorDetailActivity
        val doctorId = intent.getStringExtra(EXTRA_DOCTOR_ID) ?: ""
        val doctorName = intent.getStringExtra(EXTRA_DOCTOR_NAME) ?: "Dokter"

        // Toolbar back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarPayment)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pembayaran"

        // Update teks nama dokter di ringkasan
        val tvServiceName = findViewById<TextView>(R.id.tvServiceName)
        tvServiceName.text = "Konsultasi dengan $doctorName"

        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)

        // Klik tombol "SAYA SUDAH BAYAR"
        btnConfirmPayment.setOnClickListener {
            // 1. Tampilkan animasi loading
            progressBar.visibility = View.VISIBLE
            btnConfirmPayment.isEnabled = false
            btnConfirmPayment.text = "Memverifikasi..."

            // TODO (Programmer 4): Ubah status konsultasi di Firebase di sini
            // Contoh: db.collection("consultations").document(consultationId)
            //             .update("status", "paid")
            //             .addOnSuccessListener { onPaymentVerified() }
            //             .addOnFailureListener { onPaymentFailed() }

            // Sementara: simulasi delay 2 detik lalu selesai
            // (Programmer 4 hapus handler ini dan ganti dengan Firebase call)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                onPaymentVerified()
            }, 2000)
        }
    }

    // Dipanggil setelah pembayaran berhasil diverifikasi
    private fun onPaymentVerified() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBarPayment)
        val btnConfirmPayment = findViewById<MaterialButton>(R.id.btnConfirmPayment)

        progressBar.visibility = View.GONE
        btnConfirmPayment.text = "✓ Pembayaran Dikonfirmasi"
        btnConfirmPayment.backgroundTintList = android.content.res.ColorStateList.valueOf(
            android.graphics.Color.parseColor("#1B5E20") // hijau gelap
        )

        Toast.makeText(this, "Pembayaran berhasil! Konsultasi dimulai.", Toast.LENGTH_LONG).show()

        // TODO (Programmer 4): Pindah ke halaman chat konsultasi
        // startActivity(Intent(this, ChatActivity::class.java))
        // finish()
    }

    // Dipanggil jika pembayaran gagal
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
