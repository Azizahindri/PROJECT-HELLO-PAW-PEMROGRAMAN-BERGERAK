package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorDetailActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_detail)

        // ── Ambil data dari Intent ──────────────────────────────────────────────
        val doctorId       = intent.getStringExtra("DOCTOR_ID")           ?: ""
        val doctorName     = intent.getStringExtra("DOCTOR_NAME")         ?: ""
        val specialization = intent.getStringExtra("DOCTOR_SPECIALIZATION") ?: ""
        val fee            = intent.getIntExtra("DOCTOR_FEE", 50000)
        val rating         = intent.getFloatExtra("DOCTOR_RATING", 0f)
        val experience     = intent.getIntExtra("DOCTOR_EXPERIENCE", 0)
        val bio            = intent.getStringExtra("DOCTOR_BIO")          ?: ""
        val isOnline       = intent.getBooleanExtra("DOCTOR_STATUS", false)

        // ── Bind View (ID sesuai activity_doctor_detail.xml) ───────────────────
        //    tvDetailDoctorName   → nama dokter di header
        //    tvDetailSpecialization → spesialisasi di header
        //    ratingBarDetail      → rating di header
        //    tvTarifAmount        → tarif di card quick info
        //    tvBio                → bio di card biodata
        //    tvPraktikTime        → jam praktik (statis / bisa dikosongkan)
        //    btnStartConsultation → tombol mulai konsultasi
        val tvName        = findViewById<TextView>(R.id.tvDetailDoctorName)
        val tvSpec        = findViewById<TextView>(R.id.tvDetailSpecialization)
        val ratingBar     = findViewById<RatingBar>(R.id.ratingBarDetail)
        val tvFee         = findViewById<TextView>(R.id.tvTarifAmount)
        val tvBio         = findViewById<TextView>(R.id.tvBio)
        val tvPraktik     = findViewById<TextView>(R.id.tvPraktikTime)
        val btnConsult    = findViewById<MaterialButton>(R.id.btnStartConsultation)

        // ── Isi Data ───────────────────────────────────────────────────────────
        tvName.text    = "drh. $doctorName"
        tvSpec.text    = specialization
        ratingBar.rating = rating
        tvFee.text     = "Rp ${String.format("%,d", fee).replace(',', '.')}"
        tvBio.text     = if (bio.isNotEmpty()) bio
        else "Informasi bio belum tersedia."
        tvPraktik.text = "Senin – Jumat: 08.00 – 20.00 WIB\nSabtu: 08.00 – 14.00 WIB"

        // ── Warna tombol & teks sesuai status online/offline ──────────────────
        if (isOnline) {
            btnConsult.text = "Mulai Konsultasi"
            btnConsult.alpha = 1f
        } else {
            btnConsult.text = "Dokter Sedang Offline"
            btnConsult.alpha = 0.5f
        }

        // ── Tombol Mulai Konsultasi ────────────────────────────────────────────
        btnConsult.setOnClickListener {
            if (!isOnline) {
                Toast.makeText(this, "Dokter sedang offline, coba lagi nanti", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra("DOCTOR_ID",   doctorId)
            intent.putExtra("DOCTOR_NAME", doctorName)
            intent.putExtra("FEE",         fee)
            intent.putExtra("USER_ID",     currentUser.uid)
            startActivity(intent)
        }
    }
}