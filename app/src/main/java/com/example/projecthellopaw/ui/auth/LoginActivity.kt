package com.example.projecthellopaw.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.databinding.ActivityLoginBinding
import com.example.projecthellopaw.ui.admin.AdminMainActivity // Pastikan package ini sesuai projectmu
import com.example.projecthellopaw.ui.doctor.DoctorMainActivity
import com.example.projecthellopaw.ui.user.UserMainActivity // Pastikan package ini sesuai projectmu
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

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
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

                                when (role) {
                                    "ADMIN" -> {
                                        Toast.makeText(this, "Login Berhasil sebagai Admin!", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, AdminMainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    "OWNER" -> {
                                        Toast.makeText(this, "Login Berhasil sebagai Pemilik!", Toast.LENGTH_SHORT).show()
                                        // FIX: Menambahkan perpindahan ke UserMainActivity
                                        val intent = Intent(this, UserMainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    "DOCTOR" -> {
                                        Toast.makeText(this, "Selamat datang, Dokter!", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, DoctorMainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    else -> {
                                        Toast.makeText(this, "Role tidak dikenali!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Data user tidak ditemukan di database!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    // Jika salah password / email belum terdaftar, larinya ke sini
                    Toast.makeText(this, "Login Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }
}