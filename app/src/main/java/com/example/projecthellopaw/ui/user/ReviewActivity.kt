package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ReviewActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatRoomId = ""
    private var doctorId = ""
    private var doctorName = ""
    private var ownerId = ""
    private var petName = ""
    private var duration = 0
    private var hasReview = false

    companion object {
        private const val TAG = "ReviewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        // Ambil data dari intent
        chatRoomId = intent.getStringExtra("CHAT_ROOM_ID") ?: ""
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dokter"
        ownerId = intent.getStringExtra("OWNER_ID") ?: ""
        petName = intent.getStringExtra("PET_NAME") ?: "Anabul"
        duration = intent.getIntExtra("DURATION", 0)

        Log.d(TAG, "=== DATA DITERIMA ===")
        Log.d(TAG, "chatRoomId: $chatRoomId")
        Log.d(TAG, "doctorName: $doctorName")
        Log.d(TAG, "duration: $duration")

        // Inisialisasi view
        val tvDoctorName = findViewById<TextView>(R.id.tvDoctorName)
        val tvPetName = findViewById<TextView>(R.id.tvPetName)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvRatingLabel = findViewById<TextView>(R.id.tvRatingLabel)
        val rbRating = findViewById<RatingBar>(R.id.rbDoctorRating)
        val etComment = findViewById<EditText>(R.id.etReviewComment)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitReview)

        // Set data
        tvDoctorName.text = "drh. $doctorName"
        tvPetName.text = "Konsultasi untuk: $petName"
        tvDuration.text = "Durasi: ${duration} menit"

        // ✅ CEK APAKAH SUDAH PERNAH REVIEW
        checkIfAlreadyReviewed(rbRating, etComment, btnSubmit)

        // Rating change listener
        rbRating.setOnRatingBarChangeListener { _, rating, _ ->
            tvRatingLabel.text = when (rating) {
                0f -> ""
                1f -> "Sangat Buruk"
                2f -> "Buruk"
                3f -> "Cukup"
                4f -> "Baik"
                5f -> "Sangat Baik"
                else -> ""
            }
        }

        // Submit button
        btnSubmit.setOnClickListener {
            val rating = rbRating.rating
            if (rating == 0f) {
                Toast.makeText(this, "Silakan pilih bintang terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val comment = etComment.text.toString().trim()
            if (comment.isEmpty()) {
                Toast.makeText(this, "Silakan tulis ulasan Anda", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            submitReview(rating, comment, btnSubmit)
        }
    }

    private fun checkIfAlreadyReviewed(
        rbRating: RatingBar,
        etComment: EditText,
        btnSubmit: Button
    ) {
        db.collection("chat_rooms").document(chatRoomId)
            .get()
            .addOnSuccessListener { document ->
                hasReview = document.getBoolean("hasReview") ?: false

                if (hasReview) {
                    Log.d(TAG, "=== SUDAH PERNAH REVIEW ===")
                    Toast.makeText(this, "Anda sudah memberi review", Toast.LENGTH_SHORT).show()

                    // Nonaktifkan semua form
                    rbRating.isEnabled = false
                    rbRating.rating = 0f
                    etComment.isEnabled = false
                    etComment.hint = "Anda sudah memberikan review"
                    btnSubmit.isEnabled = false
                    btnSubmit.text = "Sudah Review"
                    btnSubmit.alpha = 0.5f

                    // Tampilkan review yang sudah diberikan (opsional)
                    loadExistingReview()
                } else {
                    Log.d(TAG, "=== BELUM PERNAH REVIEW ===")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check review status", e)
            }
    }

    private fun loadExistingReview() {
        // Ambil review yang sudah ada untuk ditampilkan
        db.collection("reviews")
            .whereEqualTo("chatRoomId", chatRoomId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val doc = documents.documents[0]
                    val rating = doc.getDouble("rating") ?: 0.0
                    val comment = doc.getString("comment") ?: ""

                    val rbRating = findViewById<RatingBar>(R.id.rbDoctorRating)
                    val etComment = findViewById<EditText>(R.id.etReviewComment)
                    val tvRatingLabel = findViewById<TextView>(R.id.tvRatingLabel)

                    rbRating.rating = rating.toFloat()
                    etComment.setText(comment)
                    etComment.hint = "Review Anda"

                    tvRatingLabel.text = when (rating) {
                        1.0 -> "Sangat Buruk"
                        2.0 -> "Buruk"
                        3.0 -> "Cukup"
                        4.0 -> "Baik"
                        5.0 -> "Sangat Baik"
                        else -> ""
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load existing review", e)
            }
    }

    private fun submitReview(rating: Float, comment: String, btnSubmit: Button) {
        // ✅ CEK LAGI APAKAH SUDAH PERNAH REVIEW
        if (hasReview) {
            Toast.makeText(this, "Anda sudah memberi review", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserId = auth.currentUser?.uid ?: ""

        val reviewData = hashMapOf(
            "chatRoomId" to chatRoomId,
            "doctorId" to doctorId,
            "doctorName" to doctorName,
            "ownerId" to ownerId,
            "userId" to currentUserId,
            "rating" to rating.toDouble(),
            "comment" to comment,
            "petName" to petName,
            "duration" to duration,
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Disable button agar tidak double click
        btnSubmit.isEnabled = false
        btnSubmit.text = "Mengirim..."

        Log.d(TAG, "=== SUBMITTING REVIEW ===")
        Log.d(TAG, "reviewData: $reviewData")

        db.collection("reviews").add(reviewData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "=== REVIEW ADDED: ${documentReference.id} ===")

                // ✅ UPDATE hasReview = true
                db.collection("chat_rooms").document(chatRoomId)
                    .update("hasReview", true)
                    .addOnSuccessListener {
                        Log.d(TAG, "=== HAS REVIEW UPDATED ===")
                        Toast.makeText(this, "Terima kasih atas ulasannya!", Toast.LENGTH_LONG).show()
                        hasReview = true
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update hasReview", e)
                        Toast.makeText(this, "Gagal mengirim ulasan", Toast.LENGTH_SHORT).show()
                        btnSubmit.isEnabled = true
                        btnSubmit.text = "Kirim Ulasan"
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to submit review", e)
                Toast.makeText(this, "Gagal mengirim ulasan: ${e.message}", Toast.LENGTH_SHORT).show()
                btnSubmit.isEnabled = true
                btnSubmit.text = "Kirim Ulasan"
            }
    }
}