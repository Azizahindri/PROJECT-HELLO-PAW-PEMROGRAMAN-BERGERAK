package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.chat.ChatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var doctorId = ""
    private var doctorName = ""
    private var fee = 0
    private var petName = ""

    companion object {
        private const val REQUEST_PAYMENT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_detail)

        // Ambil data dari Intent
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: ""
        val specialization = intent.getStringExtra("DOCTOR_SPECIALIZATION") ?: ""
        fee = intent.getIntExtra("DOCTOR_FEE", 50000)
        val rating = intent.getFloatExtra("DOCTOR_RATING", 0f)
        val bio = intent.getStringExtra("DOCTOR_BIO") ?: ""
        val isOnline = intent.getBooleanExtra("DOCTOR_STATUS", false)

        // Bind View
        val tvName = findViewById<TextView>(R.id.tvDetailDoctorName)
        val tvSpec = findViewById<TextView>(R.id.tvDetailSpecialization)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBarDetail)
        val tvFee = findViewById<TextView>(R.id.tvTarifAmount)
        val tvBio = findViewById<TextView>(R.id.tvBio)
        val tvPraktik = findViewById<TextView>(R.id.tvPraktikTime)
        val btnConsult = findViewById<MaterialButton>(R.id.btnStartConsultation)

        // Isi Data
        tvName.text = "drh. $doctorName"
        tvSpec.text = specialization
        ratingBar.rating = rating
        tvFee.text = "Rp ${String.format("%,d", fee).replace(',', '.')}"
        tvBio.text = if (bio.isNotEmpty()) bio else "Informasi bio belum tersedia."
        tvPraktik.text = "Senin – Jumat: 08.00 – 20.00 WIB\nSabtu: 08.00 – 14.00 WIB"

        if (isOnline) {
            btnConsult.text = "Mulai Konsultasi"
            btnConsult.alpha = 1f
        } else {
            btnConsult.text = "Dokter Sedang Offline"
            btnConsult.alpha = 0.5f
        }

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

            checkAndSelectPet(currentUser.uid)
        }
    }

    private fun checkAndSelectPet(userId: String) {
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Silakan tambahkan hewan peliharaan terlebih dahulu", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AddPetActivity::class.java)
                    startActivity(intent)
                    return@addOnSuccessListener
                }

                if (documents.size() > 1) {
                    showPetSelectionDialog(documents)
                } else {
                    val petName = documents.documents[0].getString("name") ?: "Hewan"
                    goToPayment(petName)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data hewan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPetSelectionDialog(documents: com.google.firebase.firestore.QuerySnapshot) {
        val petNames = documents.map { it.getString("name") ?: "Hewan" }

        AlertDialog.Builder(this)
            .setTitle("Pilih Hewan Peliharaan")
            .setItems(petNames.toTypedArray()) { _, which ->
                val selectedPetName = petNames[which]
                goToPayment(selectedPetName)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun goToPayment(petName: String) {
        this.petName = petName

        val intent = Intent(this, PaymentActivity::class.java)
        intent.putExtra("DOCTOR_ID", doctorId)
        intent.putExtra("DOCTOR_NAME", doctorName)
        intent.putExtra("FEE", fee)
        intent.putExtra("USER_ID", auth.currentUser?.uid ?: "")
        intent.putExtra("PET_NAME", petName)
        startActivityForResult(intent, REQUEST_PAYMENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PAYMENT && resultCode == RESULT_OK) {
            val paymentSuccess = data?.getBooleanExtra("PAYMENT_SUCCESS", false) ?: false
            if (paymentSuccess) {
                Toast.makeText(this, "Konsultasi dimulai!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}