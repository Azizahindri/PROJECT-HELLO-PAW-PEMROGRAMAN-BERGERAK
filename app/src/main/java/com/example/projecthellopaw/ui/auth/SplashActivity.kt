package com.example.projecthellopaw.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.databinding.ActivitySplashBinding
import com.example.projecthellopaw.ui.admin.AdminMainActivity
import com.example.projecthellopaw.ui.doctor.DoctorMainActivity
import com.example.projecthellopaw.ui.user.UserMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val delayMillis = 3000L

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, delayMillis)
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")

                        when (role) {
                            "ADMIN" -> {
                                Toast.makeText(this, "Selamat datang kembali, Admin!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, AdminMainActivity::class.java))
                                finish()
                            }
                            "OWNER" -> {
                                Toast.makeText(this, "Selamat datang kembali, Pemilik!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, UserMainActivity::class.java))
                                finish()
                            }
                            "DOCTOR" -> {
                                Toast.makeText(this, "Selamat datang kembali, Dokter!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, DoctorMainActivity::class.java))
                                finish()
                            }
                            else -> {
                                Toast.makeText(this, "Role tidak dikenali!", Toast.LENGTH_SHORT).show()
                                navigateToLogin()
                            }
                        }
                    } else {
                        navigateToLogin()
                    }
                }
                .addOnFailureListener {
                    navigateToLogin()
                }
        } else {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}