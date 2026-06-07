package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ReviewActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)

        val doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        val rbRating = findViewById<RatingBar>(R.id.rbDoctorRating)
        val etComment = findViewById<EditText>(R.id.etReviewComment)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitReview)

        btnSubmit.setOnClickListener {
            if (rbRating.rating == 0f) {
                Toast.makeText(this, "Silakan pilih bintang terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reviewData = hashMapOf(
                "doctorId" to doctorId,
                "userId" to (auth.currentUser?.uid ?: ""),
                "rating" to rbRating.rating.toDouble(),
                "comment" to etComment.text.toString(),
                "timestamp" to FieldValue.serverTimestamp()
            )

            db.collection("reviews").add(reviewData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Terima kasih atas ulasannya!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal mengirim ulasan", Toast.LENGTH_SHORT).show()
                }
        }
    }
}