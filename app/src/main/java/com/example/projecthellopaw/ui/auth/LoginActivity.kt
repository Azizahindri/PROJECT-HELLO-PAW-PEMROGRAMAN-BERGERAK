package com.example.projecthellopaw.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.databinding.ActivityLoginBinding
import com.example.projecthellopaw.ui.doctor.DoctorMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Tombol login disesuaikan dengan ID: btnLogin
        binding.btnLogin.setOnClickListener {
            // ID diubah dari etLoginEmail menjadi etEmail sesuai XML Prog 2
            val email = binding.etEmail.text.toString().trim()
            // ID diubah dari etLoginPassword menjadi etPassword sesuai XML Prog 2
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan Password wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lakukan Login via Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""

                    // Ambil data role dari Firestore berdasarkan UID
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val role = document.getString("role")

                                // Arahkan halaman sesuai ROLE
                                if (role == "OWNER") {
                                    Toast.makeText(
                                        this,
                                        "Login sebagai Pemilik",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Todo: Intent ke MainActivity milik User / Owner
                                    finish()
                                } else if (role == "DOCTOR") {
                                        Toast.makeText(this, "Selamat datang, Dokter!", Toast.LENGTH_SHORT).show()

                                        // Kita arahkan ke DoctorMainActivity asli yang ada di folder proyekmu
                                        val intent = Intent(this, DoctorMainActivity::class.java)
                                        startActivity(intent)

                                        finish()
                                    }
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Login Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // ID diubah dari tvToRegister menjadi tvGoToRegister sesuai XML Prog 2
        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}