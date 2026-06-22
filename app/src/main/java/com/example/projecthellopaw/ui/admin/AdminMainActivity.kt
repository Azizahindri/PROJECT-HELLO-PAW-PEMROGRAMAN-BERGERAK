package com.example.projecthellopaw.ui.admin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityAdminMainBinding
import com.example.projecthellopaw.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "AdminMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserCounts()

        binding.btnViewUsers.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            intent.putExtra("role", "OWNER")
            startActivity(intent)
        }

        binding.btnViewDoctors.setOnClickListener {
            val intent = Intent(this, ManageUsersActivity::class.java)
            intent.putExtra("role", "DOCTOR")
            startActivity(intent)
        }

        binding.btnAdminLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadUserCounts() {
        db.collection("users")
            .whereEqualTo("role", "OWNER")
            .get()
            .addOnSuccessListener { documents ->
                val userCount = documents.size()
                binding.tvCountUsers.text = userCount.toString()
                Log.d(TAG, "Total Users: $userCount")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load user count: ${e.message}", e)
                binding.tvCountUsers.text = "0"
            }

        db.collection("users")
            .whereEqualTo("role", "DOCTOR")
            .get()
            .addOnSuccessListener { documents ->
                val doctorCount = documents.size()
                binding.tvCountDoctors.text = doctorCount.toString()
                Log.d(TAG, "Total Doctors: $doctorCount")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load doctor count: ${e.message}", e)
                binding.tvCountDoctors.text = "0"
            }
    }

    private fun logout() {
        try {
            auth.signOut()

            Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()

        } catch (e: Exception) {
            Log.e(TAG, "Error saat logout: ${e.message}", e)
            Toast.makeText(this, "Gagal logout: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserCounts()
    }
}