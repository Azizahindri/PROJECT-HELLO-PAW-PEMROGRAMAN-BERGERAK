package com.example.projecthellopaw.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityAdminMainBinding

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set data statistik (contoh)
        binding.tvCountUsers.text = "188"
        binding.tvCountDoctors.text = "65"

        // Klik Lihat Pengguna -> buka ManageUsersActivity
        binding.btnViewUsers.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            intent.putExtra("role", "USER") // Untuk filter user biasa
            startActivity(intent)
        }

        // Klik Lihat Dokter -> buka ManageUsersActivity
        binding.btnViewDoctors.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            intent.putExtra("role", "DOCTOR") // Untuk filter dokter
            startActivity(intent)
        }

        // Logout
        binding.btnAdminLogout.setOnClickListener {
            finish()
        }
    }
}