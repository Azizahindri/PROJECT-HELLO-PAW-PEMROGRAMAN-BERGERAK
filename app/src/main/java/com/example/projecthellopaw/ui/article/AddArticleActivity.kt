package com.example.projecthellopaw.ui.article

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class AddArticleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_article)

        val db = FirebaseFirestore.getInstance()
        val etTitle = findViewById<TextInputEditText>(R.id.etTitle)
        val etDescription = findViewById<TextInputEditText>(R.id.etDescription)
        val etThumbnail = findViewById<TextInputEditText>(R.id.etThumbnail)
        val etUrl = findViewById<TextInputEditText>(R.id.etUrl)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val thumbnail = etThumbnail.text.toString().trim()
            val url = etUrl.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "Kolom Wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val articleData = hashMapOf(
                "title" to title,
                "description" to description,
                "thumbnail" to thumbnail,
                "url" to url
            )

            db.collection("articles").add(articleData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Artikel berhasil dirilis!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}