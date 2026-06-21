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
    private var isReadOnly = false

    companion object {
        private const val TAG = "ReviewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        chatRoomId = intent.getStringExtra("CHAT_ROOM_ID") ?: ""
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dokter"
        ownerId = intent.getStringExtra("OWNER_ID") ?: ""
        petName = intent.getStringExtra("PET_NAME") ?: "Anabul"
        duration = intent.getIntExtra("DURATION", 0)
        isReadOnly = intent.getBooleanExtra("IS_READ_ONLY", false)

        val tvDoctorName = findViewById<TextView>(R.id.tvDoctorName)
        val tvPetName = findViewById<TextView>(R.id.tvPetName)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvRatingLabel = findViewById<TextView>(R.id.tvRatingLabel)
        val rbRating = findViewById<RatingBar>(R.id.rbDoctorRating)
        val etComment = findViewById<EditText>(R.id.etReviewComment)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitReview)

        tvDoctorName.text = "drh. $doctorName"
        tvPetName.text = "Konsultasi untuk: $petName"
        tvDuration.text = "Durasi: $duration menit"

        Log.d(TAG, "=== REVIEW ACTIVITY ===")
        Log.d(TAG, "chatRoomId: $chatRoomId")
        Log.d(TAG, "doctorId: $doctorId")
        Log.d(TAG, "ownerId: $ownerId")
        Log.d(TAG, "isReadOnly: $isReadOnly")

        if (isReadOnly) {
            setReadOnlyMode(rbRating, etComment, btnSubmit)
            return
        }

        val currentUserId = auth.currentUser?.uid ?: ""

        /*
         * Review hanya boleh diberikan oleh pasien.
         * Kalau yang login adalah dokter, langsung mode lihat review.
         */
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            setReadOnlyMode(rbRating, etComment, btnSubmit)
            return
        }

        if (currentUserId == doctorId) {
            Toast.makeText(this, "Dokter tidak bisa memberi review", Toast.LENGTH_SHORT).show()
            setReadOnlyMode(rbRating, etComment, btnSubmit)
            return
        }

        if (ownerId.isNotEmpty() && currentUserId != ownerId) {
            Toast.makeText(this, "Review hanya dapat diberikan oleh pasien", Toast.LENGTH_SHORT).show()
            setReadOnlyMode(rbRating, etComment, btnSubmit)
            return
        }

        checkIfAlreadyReviewed(rbRating, etComment, btnSubmit)

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

        btnSubmit.setOnClickListener {
            val rating = rbRating.rating

            if (rating == 0f) {
                Toast.makeText(
                    this,
                    "Silakan pilih bintang terlebih dahulu",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val comment = etComment.text.toString().trim()

            if (comment.isEmpty()) {
                Toast.makeText(
                    this,
                    "Silakan tulis ulasan Anda",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            submitReview(rating, comment, btnSubmit)
        }
    }

    private fun setReadOnlyMode(
        rbRating: RatingBar,
        etComment: EditText,
        btnSubmit: Button
    ) {
        rbRating.isEnabled = false
        rbRating.setIsIndicator(true)

        etComment.isEnabled = false
        etComment.hint = "Review hanya dapat dilihat"

        btnSubmit.isEnabled = false
        btnSubmit.text = "Lihat Review"
        btnSubmit.alpha = 0.5f

        loadExistingReview()
    }

    private fun checkIfAlreadyReviewed(
        rbRating: RatingBar,
        etComment: EditText,
        btnSubmit: Button
    ) {
        if (chatRoomId.isEmpty()) {
            Toast.makeText(this, "Data konsultasi tidak ditemukan", Toast.LENGTH_SHORT).show()
            setReadOnlyMode(rbRating, etComment, btnSubmit)
            return
        }

        db.collection("chat_rooms")
            .document(chatRoomId)
            .get()
            .addOnSuccessListener { document ->
                hasReview = document.getBoolean("hasReview") ?: false

                if (hasReview) {
                    Toast.makeText(this, "Review sudah pernah diberikan", Toast.LENGTH_SHORT).show()

                    rbRating.isEnabled = false
                    rbRating.setIsIndicator(true)

                    etComment.isEnabled = false
                    etComment.hint = "Review sudah diberikan"

                    btnSubmit.isEnabled = false
                    btnSubmit.text = "Sudah Review"
                    btnSubmit.alpha = 0.5f

                    loadExistingReview()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal mengecek review", e)
                Toast.makeText(this, "Gagal mengecek status review", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadExistingReview() {
        if (chatRoomId.isEmpty()) return

        db.collection("reviews")
            .whereEqualTo("chatRoomId", chatRoomId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) return@addOnSuccessListener

                val doc = documents.documents[0]
                val rating = doc.getDouble("rating") ?: 0.0
                val comment = doc.getString("comment") ?: ""

                val rbRating = findViewById<RatingBar>(R.id.rbDoctorRating)
                val etComment = findViewById<EditText>(R.id.etReviewComment)
                val tvRatingLabel = findViewById<TextView>(R.id.tvRatingLabel)

                rbRating.rating = rating.toFloat()
                rbRating.isEnabled = false
                rbRating.setIsIndicator(true)

                etComment.setText(comment)
                etComment.isEnabled = false
                etComment.hint = "Review dari pasien"

                tvRatingLabel.text = when (rating) {
                    1.0 -> "Sangat Buruk"
                    2.0 -> "Buruk"
                    3.0 -> "Cukup"
                    4.0 -> "Baik"
                    5.0 -> "Sangat Baik"
                    else -> ""
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal memuat review", e)
            }
    }

    private fun submitReview(
        rating: Float,
        comment: String,
        btnSubmit: Button
    ) {
        val currentUserId = auth.currentUser?.uid ?: ""

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == doctorId) {
            Toast.makeText(this, "Dokter tidak bisa memberi review", Toast.LENGTH_SHORT).show()
            return
        }

        if (ownerId.isNotEmpty() && currentUserId != ownerId) {
            Toast.makeText(this, "Review hanya dapat diberikan oleh pasien", Toast.LENGTH_SHORT).show()
            return
        }

        if (hasReview) {
            Toast.makeText(this, "Review sudah pernah diberikan", Toast.LENGTH_SHORT).show()
            return
        }

        executeSubmitReview(rating, comment, btnSubmit)
    }

    private fun executeSubmitReview(
        rating: Float,
        comment: String,
        btnSubmit: Button
    ) {
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

        btnSubmit.isEnabled = false
        btnSubmit.text = "Mengirim..."

        db.collection("reviews")
            .add(reviewData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Review added: ${documentReference.id}")

                db.collection("chat_rooms")
                    .document(chatRoomId)
                    .update("hasReview", true)
                    .addOnSuccessListener {
                        updateDoctorRating()

                        Toast.makeText(
                            this,
                            "Terima kasih atas ulasannya!",
                            Toast.LENGTH_LONG
                        ).show()

                        hasReview = true
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Gagal update hasReview", e)

                        Toast.makeText(
                            this,
                            "Gagal mengirim ulasan",
                            Toast.LENGTH_SHORT
                        ).show()

                        btnSubmit.isEnabled = true
                        btnSubmit.text = "Kirim Ulasan"
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Gagal submit review", e)

                Toast.makeText(
                    this,
                    "Gagal mengirim ulasan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                btnSubmit.isEnabled = true
                btnSubmit.text = "Kirim Ulasan"
            }
    }

    private fun updateDoctorRating() {
        if (doctorId.isEmpty()) return

        db.collection("reviews")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) return@addOnSuccessListener

                var totalRating = 0.0
                var count = 0

                for (doc in documents) {
                    val rating = doc.getDouble("rating") ?: 0.0
                    totalRating += rating
                    count++
                }

                val averageRating = if (count > 0) {
                    totalRating / count
                } else {
                    0.0
                }

                db.collection("doctor_profiles")
                    .document(doctorId)
                    .update(
                        "averageRating", averageRating,
                        "totalReviews", count
                    )
                    .addOnSuccessListener {
                        Log.d(TAG, "Doctor rating updated to $averageRating")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update doctor rating", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get reviews for rating", e)
            }
    }
}