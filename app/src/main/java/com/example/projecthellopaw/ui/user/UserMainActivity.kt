package com.example.projecthellopaw.ui.user

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.projecthellopaw.R

class UserMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)

        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Tampilkan fragment pertama (Daftar Dokter) saat activity pertama dibuka
        if (savedInstanceState == null) {
            loadFragment(DoctorListFragment())
        }

        // Listener BottomNavigationView: ganti fragment sesuai tab yang diklik
        bottomNavView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_doctors -> {
                    loadFragment(DoctorListFragment())
                    true
                }
                R.id.nav_my_pets -> {
                    // PetListFragment milik Programmer 1 — sambungkan di sini
                    // loadFragment(PetListFragment())
                    // Sementara pakai placeholder:
                    loadFragment(PetListFragment())
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi helper untuk mengganti fragment di dalam fragmentContainer
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}