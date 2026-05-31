package com.example.projecthellopaw.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.databinding.ActivityAdminMainBinding
import com.example.projecthellopaw.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Ambil data total Pengguna & Dokter dari Firestore secara otomatis
        fetchDashboardCounts()

        // Klik Card Pengguna -> Buka daftar Pengguna (OWNER)
        binding.cardUsers.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            intent.putExtra("ROLE_TYPE", "OWNER")
            startActivity(intent)
        }

        // Klik Card Dokter -> Buka daftar Dokter (DOCTOR)
        binding.cardDoctors.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            intent.putExtra("ROLE_TYPE", "DOCTOR")
            startActivity(intent)
        }

        // Tombol Logout Admin
        binding.btnAdminLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Admin Logout Berhasil", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun fetchDashboardCounts() {
        // Hitung data OWNER secara dinamis dari Firestore
        db.collection("users")
            .whereEqualTo("role", "OWNER")
            .get()
            .addOnSuccessListener { snapshots ->
                binding.tvCountUsers.text = snapshots.size().toString()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat count pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Hitung data DOCTOR secara dinamis dari Firestore
        db.collection("users")
            .whereEqualTo("role", "DOCTOR")
            .get()
            .addOnSuccessListener { snapshots ->
                binding.tvCountDoctors.text = snapshots.size().toString()
            }
    }
}