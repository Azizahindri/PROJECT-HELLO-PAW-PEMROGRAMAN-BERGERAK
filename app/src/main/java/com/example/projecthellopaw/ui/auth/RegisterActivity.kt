package com.example.projecthellopaw.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnRegister.setOnClickListener {
            val name = binding.etNama.text.toString().trim()
            val email = binding.etRegEmail.text.toString().trim()
            val password = binding.etRegPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Semua kolom wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = "OWNER"

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""

                    val userMap = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "role" to role
                    )

                    db.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Pendaftaran Berhasil!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal simpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Pendaftaran Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}