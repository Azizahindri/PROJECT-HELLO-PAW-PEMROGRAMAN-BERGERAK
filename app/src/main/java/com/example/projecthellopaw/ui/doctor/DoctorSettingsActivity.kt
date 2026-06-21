package com.example.projecthellopaw.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.article.AddArticleActivity
import com.example.projecthellopaw.ui.auth.LoginActivity
import com.example.projecthellopaw.ui.user.ChangePasswordActivity
import com.google.firebase.auth.FirebaseAuth

class DoctorSettingsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_settings)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val ivBack = findViewById<ImageView>(R.id.ivBackDoctorSettings)

        val llDoctorProfile = findViewById<LinearLayout>(R.id.llDoctorProfile)
        val llPatientProfile = findViewById<LinearLayout>(R.id.llPatientProfile)
        val llDoctorArticle = findViewById<LinearLayout>(R.id.llDoctorArticle)
        val llDoctorRating = findViewById<LinearLayout>(R.id.llDoctorRating)
        val llDoctorChangePassword = findViewById<LinearLayout>(R.id.llDoctorChangePassword)
        val llDoctorLogout = findViewById<LinearLayout>(R.id.llDoctorLogout)

        ivBack.setOnClickListener {
            finish()
        }

        llDoctorProfile.setOnClickListener {
            startActivity(
                Intent(this, EditDoctorProfileActivity::class.java)
            )
        }

        llPatientProfile.setOnClickListener {
            startActivity(
                Intent(this, DoctorPatientListActivity::class.java)
            )
        }

        llDoctorArticle.setOnClickListener {
            showArticleMenu()
        }

        llDoctorRating.setOnClickListener {
            startActivity(
                Intent(this, DoctorRatingHistoryActivity::class.java)
            )
        }

        llDoctorChangePassword.setOnClickListener {
            startActivity(
                Intent(this, ChangePasswordActivity::class.java)
            )
        }

        llDoctorLogout.setOnClickListener {
            logoutDoctor()
        }
    }

    private fun showArticleMenu() {
        val options = arrayOf(
            "Tambah Artikel",
            "Lihat Artikel yang Diterapkan"
        )

        AlertDialog.Builder(this)
            .setTitle("Artikel")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        startActivity(
                            Intent(this, AddArticleActivity::class.java)
                        )
                    }

                    1 -> {
                        startActivity(
                            Intent(this, DoctorArticleListActivity::class.java)
                        )
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun logoutDoctor() {
        auth.signOut()

        Toast.makeText(
            this,
            "Logout Berhasil",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}