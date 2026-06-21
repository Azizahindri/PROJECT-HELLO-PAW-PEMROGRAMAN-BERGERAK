package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class DoctorDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var doctorId = ""
    private var doctorName = ""
    private var fee = 0
    private var petName = ""
    private var doctorRating = 0f
    private var totalReviews = 0

    companion object {
        private const val TAG = "DoctorDetailActivity"
    }

    private val paymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val paymentSuccess = result.data?.getBooleanExtra("PAYMENT_SUCCESS", false) ?: false
            if (paymentSuccess) {
                Log.d(TAG, "Payment success, consultation started!")
                Toast.makeText(this, getString(R.string.consultation_started), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_detail)

        Log.d(TAG, "=== DOCTOR DETAIL ACTIVITY STARTED ===")

        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: ""
        val specialization = intent.getStringExtra("DOCTOR_SPECIALIZATION") ?: ""
        fee = intent.getIntExtra("DOCTOR_FEE", 50000)
        doctorRating = intent.getFloatExtra("DOCTOR_RATING", 0f)
        val bio = intent.getStringExtra("DOCTOR_BIO") ?: ""
        val isOnline = intent.getBooleanExtra("DOCTOR_STATUS", false)

        Log.d(TAG, "doctorId: $doctorId")
        Log.d(TAG, "doctorName: $doctorName")

        val tvName = findViewById<TextView>(R.id.tvDetailDoctorName)
        val tvSpec = findViewById<TextView>(R.id.tvDetailSpecialization)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBarDetail)
        val tvRatingCount = findViewById<TextView>(R.id.tvRatingCount)
        val tvFee = findViewById<TextView>(R.id.tvTarifAmount)
        val tvBio = findViewById<TextView>(R.id.tvBio)
        val tvPraktik = findViewById<TextView>(R.id.tvPraktikTime)
        val btnConsult = findViewById<MaterialButton>(R.id.btnStartConsultation)
        val btnViewReviews = findViewById<MaterialButton>(R.id.btnViewReviews)
        val tvTotalReviews = findViewById<TextView>(R.id.tvTotalReviews)
        val tvExperience = findViewById<TextView>(R.id.tvExperience)

        tvName.text = "drh. $doctorName"
        tvSpec.text = specialization
        ratingBar.rating = 0f
        tvRatingCount.text = "Belum ada ulasan"
        tvFee.text = "Rp ${String.format("%,d", fee).replace(',', '.')}"
        tvBio.text = if (bio.isNotEmpty()) bio else "Informasi bio belum tersedia."

        val currentUserId = auth.currentUser?.uid ?: ""
        db.collection("users").document(currentUserId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val doc = task.result
                    if (doc != null && doc.exists()) {
                        val role = doc.getString("role") ?: "OWNER"
                        if (role == "DOCTOR") {
                            btnViewReviews.visibility = View.VISIBLE
                            btnViewReviews.setOnClickListener {
                                val intent = Intent(this, ReviewListActivity::class.java)
                                intent.putExtra("DOCTOR_ID", doctorId)
                                intent.putExtra("DOCTOR_NAME", doctorName)
                                startActivity(intent)
                            }
                        }
                    }
                }
            }

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

        loadDoctorReviews(tvRatingCount, ratingBar, tvTotalReviews, tvExperience)
    }

    private fun loadDoctorReviews(
        tvRatingCount: TextView,
        ratingBar: RatingBar,
        tvTotalReviews: TextView,
        tvExperience: TextView
    ) {
        db.collection("reviews")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Failed to load reviews", task.exception)
                    tvRatingCount.text = "Gagal memuat rating"
                    return@addOnCompleteListener
                }

                val documents = task.result
                totalReviews = documents?.size() ?: 0
                var totalRating = 0.0
                if (documents != null) {
                    for (doc in documents) {
                        totalRating += doc.getDouble("rating") ?: 0.0
                    }
                }
                val averageRating = if (totalReviews > 0) totalRating / totalReviews else 0.0

                if (totalReviews > 0) {
                    tvRatingCount.text = String.format("%.1f", averageRating)
                    ratingBar.rating = averageRating.toFloat()
                } else {
                    tvRatingCount.text = "Belum ada ulasan"
                    ratingBar.rating = 0f
                }

                tvTotalReviews.text = totalReviews.toString()
            }
    }

    private fun checkAndSelectPet(userId: String) {
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Failed to get pet data", task.exception)
                    Toast.makeText(this, "Gagal mengambil data hewan", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }

                val documents = task.result
                if (documents == null || documents.isEmpty) {
                    Toast.makeText(this, "Silakan tambahkan hewan peliharaan terlebih dahulu", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, AddPetActivity::class.java)
                    startActivity(intent)
                } else if (documents.size() > 1) {
                    showPetSelectionDialog(documents)
                } else {
                    val petName = documents.documents[0].getString("name") ?: "Hewan"
                    goToPayment(petName)
                }
            }
    }

    private fun showPetSelectionDialog(documents: com.google.firebase.firestore.QuerySnapshot) {
        val petNames = documents.map { it.getString("name") ?: "Hewan" }

        AlertDialog.Builder(this)
            .setTitle("Pilih Hewan Peliharaan")
            .setItems(petNames.toTypedArray()) { _, which ->
                goToPayment(petNames[which])
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun goToPayment(petName: String) {
        this.petName = petName
        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra("DOCTOR_ID", doctorId)
            putExtra("DOCTOR_NAME", doctorName)
            putExtra("FEE", fee)
            putExtra("USER_ID", auth.currentUser?.uid ?: "")
            putExtra("PET_NAME", petName)
        }
        paymentLauncher.launch(intent)
    }
}