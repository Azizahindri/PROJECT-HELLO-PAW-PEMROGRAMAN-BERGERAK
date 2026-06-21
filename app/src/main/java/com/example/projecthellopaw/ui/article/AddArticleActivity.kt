package com.example.projecthellopaw.ui.article

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityAddArticleBinding
import com.example.projecthellopaw.ui.doctor.DoctorSettingsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class AddArticleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddArticleBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddArticleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadDoctorProfile()
        setupListeners()
    }

    private fun loadDoctorProfile() {
        val uid = auth.currentUser?.uid ?: return

        val tvName = findViewById<TextView>(R.id.tv_doctor_name)
        val tvEmail = findViewById<TextView>(R.id.tv_doctor_email)
        val ivPhoto = findViewById<CircleImageView>(R.id.iv_doctor_photo)
        val ivSettings = findViewById<ImageView>(R.id.iv_doctor_settings)

        tvEmail.text = auth.currentUser?.email ?: ""

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc.getString("name") ?: "Dokter Hewan"
                val photoUrl = userDoc.getString("photoUrl") ?: ""

                tvName.text = name

                if (photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_doctor_placeholder)
                        .circleCrop()
                        .into(ivPhoto)
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_doctor_placeholder)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            }

        ivSettings.setOnClickListener {
            startActivity(Intent(this, DoctorSettingsActivity::class.java))
        }
    }

    private fun setupListeners() {
        binding.btnSubmit.setOnClickListener {
            submitArticle()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun submitArticle() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val thumbnail = binding.etThumbnail.text.toString().trim()
        val url = binding.etUrl.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilTitle.error = "Judul tidak boleh kosong"
            return
        }
        binding.tilTitle.error = null

        if (description.isEmpty()) {
            binding.tilDescription.error = "Deskripsi tidak boleh kosong"
            return
        }
        binding.tilDescription.error = null

        if (thumbnail.isEmpty()) {
            binding.tilThumbnail.error = "URL thumbnail tidak boleh kosong"
            return
        }
        binding.tilThumbnail.error = null

        val articleData = hashMapOf(
            "title" to title,
            "description" to description,
            "thumbnail" to thumbnail,
            "url" to url,
            "doctorId" to auth.currentUser?.uid,
            "createdAt" to System.currentTimeMillis()
        )

        binding.btnSubmit.isEnabled = false
        binding.btnSubmit.text = "Menyimpan..."

        db.collection("articles")
            .add(articleData)
            .addOnSuccessListener {
                Toast.makeText(this, "Artikel berhasil diterbitkan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmit.isEnabled = true
                binding.btnSubmit.text = "📤 Terbitkan Artikel"
            }
    }
}