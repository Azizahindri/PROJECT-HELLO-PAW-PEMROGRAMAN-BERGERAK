package com.example.projecthellopaw.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.databinding.ActivitySplashBinding
import com.example.projecthellopaw.ui.doctor.DoctorMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

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

        // Menunda perpindahan halaman selama 3 detik (3000 milidetik)
        val delayMillis = 3000L

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, delayMillis)
    }

    /**
     * Fungsi pintar untuk mengecek apakah user sudah login atau belum
     */
    private fun checkUserSession() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // 1. Jika USER SUDAH LOGIN, langsung cek role-nya di Firestore
            val uid = currentUser.uid
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")

                        // Alirkan ke halaman utama masing-masing tanpa harus login lagi
                        if (role == "OWNER") {
                            Toast.makeText(this, "Selamat datang kembali, Pemilik!", Toast.LENGTH_SHORT).show()
                            // TODO: Ganti dengan Intent ke MainActivity milik Owner/User kalian nanti
                            // startActivity(Intent(this, OwnerMainActivity::class.java))
                            finish()
                        } else if (role == "DOCTOR") {
                            Toast.makeText(this, "Selamat datang, Dokter!", Toast.LENGTH_SHORT).show()

                            // Kita arahkan ke DoctorMainActivity asli yang ada di folder proyekmu
                            val intent = Intent(this, DoctorMainActivity::class.java)
                            startActivity(intent)

                            finish()
                        }
                    } else {
                        // Jika data di firestore tidak ada, lempar ke Login
                        navigateToLogin()
                    }
                }
                .addOnFailureListener {
                    // Jika internet bermasalah/gagal ambil data, amankan ke Login
                    navigateToLogin()
                }
        } else {
            // 2. Jika USER BELUM LOGIN, arahkan ke LoginActivity
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}