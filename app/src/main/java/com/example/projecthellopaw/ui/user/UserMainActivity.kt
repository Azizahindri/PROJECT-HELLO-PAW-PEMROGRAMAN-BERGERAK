package com.example.projecthellopaw.ui.user

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.databinding.ActivityUserMainBinding

class UserMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // KODE FRAGMENT DIHAPUS SEMUA AGAR TIDAK ERROR SAAT DI-RUN
        // Nanti kalau Programmer 1 sudah buat Fragment-nya, baru pasang lagi di sini
    }
}