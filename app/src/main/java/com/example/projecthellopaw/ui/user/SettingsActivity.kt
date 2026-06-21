package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val ivBack = findViewById<ImageView>(R.id.ivBackSettings)
        ivBack.setOnClickListener {
            finish()
        }

        setupMenuClick(R.id.llAccount, "Akun", ProfileActivity::class.java)
        setupMenuClickActivity(R.id.llPetProfile, PetListActivity::class.java)
        setupMenuClick(R.id.llHealthHistory, "Riwayat Kesehatan")
        setupMenuClick(R.id.llTransactionHistory, "Riwayat Transaksi")
        setupMenuClick(R.id.llInviteFriend, "Ajak Teman Pakai HelloPaw")
        setupMenuClick(R.id.llHelpCenter, "Pusat Bantuan")
        setupMenuClick(R.id.llSecurity, "Ganti Password", ChangePasswordActivity::class.java)
        setupMenuClick(R.id.llPrivacy, "Kebijakan Privasi")
        setupMenuClick(R.id.llDownload, "Download")
        setupMenuClick(R.id.llRating, "Rating")
        setupMenuClick(R.id.llPaymentMethod, "Metode Pembayaran")

        val llLogout = findViewById<LinearLayout>(R.id.llLogout)
        llLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logout Berhasil", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupMenuClick(menuId: Int, menuName: String, targetClass: Class<*>? = null) {
        val menu = findViewById<LinearLayout>(menuId)
        menu.setOnClickListener {
            if (targetClass != null) {
                val intent = Intent(this, targetClass)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Buka: $menuName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMenuClickActivity(menuId: Int, targetClass: Class<*>) {
        val menu = findViewById<LinearLayout>(menuId)
        menu.setOnClickListener {
            startActivity(Intent(this, targetClass))
        }
    }
}