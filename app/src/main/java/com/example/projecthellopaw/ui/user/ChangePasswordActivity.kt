package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnChangePassword: Button
    private lateinit var tvResetPassword: TextView
    private lateinit var ivBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()

        // Inisialisasi View
        ivBack = findViewById(R.id.ivBackChangePassword)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        tvResetPassword = findViewById(R.id.tvResetPassword)

        // Tombol Back
        ivBack.setOnClickListener { finish() }

        // Tombol Ganti Password
        btnChangePassword.setOnClickListener {
            changePassword()
        }

        // Reset Password via Email
        tvResetPassword.setOnClickListener {
            sendResetPasswordEmail()
        }
    }

    private fun changePassword() {
        val currentPassword = etCurrentPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validasi
        if (currentPassword.isEmpty()) {
            etCurrentPassword.error = "Masukkan password saat ini"
            etCurrentPassword.requestFocus()
            return
        }

        if (newPassword.isEmpty()) {
            etNewPassword.error = "Masukkan password baru"
            etNewPassword.requestFocus()
            return
        }

        if (newPassword.length < 6) {
            etNewPassword.error = "Password minimal 6 karakter"
            etNewPassword.requestFocus()
            return
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.error = "Konfirmasi password baru"
            etConfirmPassword.requestFocus()
            return
        }

        if (newPassword != confirmPassword) {
            etConfirmPassword.error = "Password tidak sama"
            etConfirmPassword.requestFocus()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Re-autentikasi + Update Password
        reauthenticateAndChangePassword(user, currentPassword, newPassword)
    }

    private fun reauthenticateAndChangePassword(
        user: com.google.firebase.auth.FirebaseUser,
        currentPassword: String,
        newPassword: String
    ) {
        btnChangePassword.isEnabled = false
        btnChangePassword.text = "Memproses..."

        // Buat credential dari email dan password saat ini
        val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)

        // Re-autentikasi (verifikasi password lama)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sukses, ganti password
                    updatePassword(user, newPassword)
                } else {
                    // Gagal (password lama salah)
                    btnChangePassword.isEnabled = true
                    btnChangePassword.text = "Ganti Password"
                    Toast.makeText(
                        this,
                        "❌ Password saat ini salah!",
                        Toast.LENGTH_SHORT
                    ).show()
                    etCurrentPassword.error = "Password salah"
                    etCurrentPassword.requestFocus()
                }
            }
    }

    private fun updatePassword(
        user: com.google.firebase.auth.FirebaseUser,
        newPassword: String
    ) {
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                btnChangePassword.isEnabled = true
                btnChangePassword.text = "Ganti Password"

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "✅ Password berhasil diubah!",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "❌ Gagal mengubah password: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun sendResetPasswordEmail() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val email = user.email
        if (email.isNullOrEmpty()) {
            Toast.makeText(this, "Email tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "📧 Email reset password telah dikirim ke $email",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "❌ Gagal mengirim email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}