package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.projecthellopaw.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserMainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)

        // ==========================================
        // HEADER PROFILE
        // ==========================================
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val ivSettings = findViewById<ImageView>(R.id.ivSettings)

        // Load data user ke header
        loadUserData(tvGreeting, tvEmail)

        // Tombol Settings -> buka SettingsActivity
        ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // ==========================================
        // BOTTOM NAVIGATION
        // ==========================================
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Fragment awal
        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_doctor -> {
                    loadFragment(DoctorListFragment())
                    true
                }
                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun loadUserData(tvGreeting: TextView, tvEmail: TextView) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("username") ?: "User"
                    val username = document.getString("username") ?: name
                    val email = document.getString("email") ?: "email@kosong.com"

                    tvGreeting.text = "Hello $username!"
                    tvEmail.text = email
                }
            }
            .addOnFailureListener {
                tvGreeting.text = "Hello User!"
                tvEmail.text = "email@kosong.com"
            }
    }

    // ==========================================
    // FUNGSI UNTUK REFRESH DATA (DIPANGGIL DARI FRAGMENT)
    // ==========================================
    fun refreshUserData() {
        val tvGreeting = findViewById<TextView>(R.id.tvGreeting)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        loadUserData(tvGreeting, tvEmail)
    }
}