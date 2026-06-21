package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var etFullName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etBirthDate: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var rbMale: RadioButton
    private lateinit var rbFemale: RadioButton
    private lateinit var etPhone: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etBirthDate = findViewById(R.id.etBirthDate)
        rgGender = findViewById(R.id.rgGender)
        rbMale = findViewById(R.id.rbMale)
        rbFemale = findViewById(R.id.rbFemale)
        etPhone = findViewById(R.id.etPhone)
        etEmail = findViewById(R.id.etEmail)
        etAddress = findViewById(R.id.etAddress)
        btnSave = findViewById(R.id.btnSave)

        val ivBack = findViewById<android.widget.ImageView>(R.id.ivBackProfile)
        ivBack.setOnClickListener { finish() }

        loadUserData()

        btnSave.setOnClickListener {
            saveUserData()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    etFullName.setText(document.getString("name") ?: "")
                    etUsername.setText(document.getString("username") ?: "")
                    etBirthDate.setText(document.getString("birthDate") ?: "")
                    etPhone.setText(document.getString("phoneNumber") ?: "")
                    etEmail.setText(document.getString("email") ?: "")
                    etAddress.setText(document.getString("address") ?: "")

                    val gender = document.getString("gender") ?: ""
                    if (gender.equals("Laki-laki", ignoreCase = true)) {
                        rbMale.isChecked = true
                    } else if (gender.equals("Perempuan", ignoreCase = true)) {
                        rbFemale.isChecked = true
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData() {
        val uid = auth.currentUser?.uid ?: return

        val fullName = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val birthDate = etBirthDate.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val address = etAddress.text.toString().trim()

        if (fullName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Nama lengkap dan username harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val gender = when (rgGender.checkedRadioButtonId) {
            R.id.rbMale -> "Laki-laki"
            R.id.rbFemale -> "Perempuan"
            else -> ""
        }

        val updates = mutableMapOf<String, Any>()
        updates["name"] = fullName
        updates["username"] = username
        updates["birthDate"] = birthDate
        updates["gender"] = gender
        updates["phoneNumber"] = phone
        updates["email"] = email
        updates["address"] = address

        if (password.isNotEmpty()) {
            updates["password"] = password
            auth.currentUser?.updatePassword(password)
                ?.addOnFailureListener {
                    Toast.makeText(this, "Gagal update password: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        db.collection("users").document(uid)
            .update(updates as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}