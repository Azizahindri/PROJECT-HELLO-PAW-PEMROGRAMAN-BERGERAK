package com.example.projecthellopaw.ui.doctor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityEditDoctorBinding

class EditDoctorProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDoctorBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedPhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPhotoUri = it
            Glide.with(this).load(it).circleCrop().into(binding.ivDoctorPhoto)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDoctorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profil Dokter"

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        loadDoctorProfile()
        setupClickListeners()
    }

    private fun loadDoctorProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("doctor_profiles").document(uid).get()
            .addOnSuccessListener { doc ->
                binding.etSpecialization.setText(doc.getString("specialization") ?: "")
                binding.etStrNumber.setText(doc.getString("strNumber") ?: "")
                binding.etConsultationFee.setText(doc.getLong("consultationFee")?.toString() ?: "50000")
                binding.etBio.setText(doc.getString("bio") ?: "")
                binding.etExperience.setText(doc.getLong("yearsOfExperience")?.toString() ?: "")
                binding.etClinicName.setText(doc.getString("clinicName") ?: "")
                binding.switchOnlineStatus.isChecked = doc.getBoolean("isOnline") ?: false

                // Load koordinat yang sudah tersimpan
                val lat = doc.getDouble("latitude") ?: 0.0
                val lng = doc.getDouble("longitude") ?: 0.0
                if (lat != 0.0) binding.etLatitude.setText(lat.toString())
                if (lng != 0.0) binding.etLongitude.setText(lng.toString())

                val photoUrl = doc.getString("photoUrl") ?: ""
                if (photoUrl.isNotEmpty()) {
                    Glide.with(this).load(photoUrl).circleCrop().into(binding.ivDoctorPhoto)
                }
            }
    }

    private fun setupClickListeners() {
        binding.btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnOpenMaps.setOnClickListener {
            val uri = Uri.parse("https://maps.google.com")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        binding.btnSave.setOnClickListener {
            if (validateForm()) saveProfile()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateForm(): Boolean {
        val specialization = binding.etSpecialization.text.toString().trim()
        val strNumber = binding.etStrNumber.text.toString().trim()
        val fee = binding.etConsultationFee.text.toString().trim()

        if (specialization.isEmpty()) {
            binding.tilSpecialization.error = "Spesialisasi tidak boleh kosong"
            return false
        }
        binding.tilSpecialization.error = null

        if (strNumber.isEmpty()) {
            binding.tilStrNumber.error = "Nomor STR tidak boleh kosong"
            return false
        }
        binding.tilStrNumber.error = null

        if (fee.isEmpty() || fee.toLongOrNull() == null) {
            binding.tilConsultationFee.error = "Masukkan tarif yang valid"
            return false
        }
        binding.tilConsultationFee.error = null

        return true
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Menyimpan..."

        val lat = binding.etLatitude.text.toString().trim().toDoubleOrNull() ?: 0.0
        val lng = binding.etLongitude.text.toString().trim().toDoubleOrNull() ?: 0.0

        val profileData = hashMapOf(
            "specialization" to binding.etSpecialization.text.toString().trim(),
            "strNumber" to binding.etStrNumber.text.toString().trim(),
            "consultationFee" to (binding.etConsultationFee.text.toString().toLongOrNull() ?: 50000L),
            "bio" to binding.etBio.text.toString().trim(),
            "yearsOfExperience" to (binding.etExperience.text.toString().toLongOrNull() ?: 0L),
            "clinicName" to binding.etClinicName.text.toString().trim(),
            "isOnline" to binding.switchOnlineStatus.isChecked,
            "latitude" to lat,
            "longitude" to lng,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        db.collection("doctor_profiles").document(uid)
            .set(profileData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profil berhasil disimpan!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Simpan Perubahan"
            }
    }
}