package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class DoctorDetailActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var doctorId = ""
    private var doctorName = ""
    private var fee = 0
    private var petName = ""

    companion object {
        private const val TAG = "DoctorDetailActivity"
    }

    // ✅ GANTI startActivityForResult dengan Activity Result API
    private val paymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val paymentSuccess = result.data?.getBooleanExtra("PAYMENT_SUCCESS", false) ?: false
            if (paymentSuccess) {
                Log.d(TAG, "Payment success, consultation started!")
                Toast.makeText(this, getString(R.string.consultation_started), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_detail)

        Log.d(TAG, "=== DOCTOR DETAIL ACTIVITY STARTED ===")

        // Ambil data dari Intent
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: ""
        val specialization = intent.getStringExtra("DOCTOR_SPECIALIZATION") ?: ""
        fee = intent.getIntExtra("DOCTOR_FEE", 50000)
        val rating = intent.getFloatExtra("DOCTOR_RATING", 0f)
        val bio = intent.getStringExtra("DOCTOR_BIO") ?: ""
        val isOnline = intent.getBooleanExtra("DOCTOR_STATUS", false)

        Log.d(TAG, "doctorId: $doctorId")
        Log.d(TAG, "doctorName: $doctorName")
        Log.d(TAG, "fee: $fee")
        Log.d(TAG, "isOnline: $isOnline")

        // Bind View
        val tvName = findViewById<TextView>(R.id.tvDetailDoctorName)
        val tvSpec = findViewById<TextView>(R.id.tvDetailSpecialization)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBarDetail)
        val tvFee = findViewById<TextView>(R.id.tvTarifAmount)
        val tvBio = findViewById<TextView>(R.id.tvBio)
        val tvPraktik = findViewById<TextView>(R.id.tvPraktikTime)
        val btnConsult = findViewById<MaterialButton>(R.id.btnStartConsultation)

        // Isi Data
        tvName.text = getString(R.string.doctor_name_prefix, doctorName)
        tvSpec.text = specialization
        ratingBar.rating = rating
        tvFee.text = String.format(Locale.getDefault(), "Rp %s", String.format("%,d", fee).replace(',', '.'))
        tvBio.text = if (bio.isNotEmpty()) bio else getString(R.string.bio_not_available)
        tvPraktik.text = getString(R.string.practice_hours)

        if (isOnline) {
            btnConsult.text = getString(R.string.start_consultation)
            btnConsult.alpha = 1f
        } else {
            btnConsult.text = getString(R.string.doctor_offline)
            btnConsult.alpha = 0.5f
        }

        btnConsult.setOnClickListener {
            Log.d(TAG, "=== BUTTON CLICKED ===")

            if (!isOnline) {
                Toast.makeText(this, getString(R.string.doctor_offline_message), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "User ID: ${currentUser.uid}")
            checkAndSelectPet(currentUser.uid)
        }
    }

    private fun checkAndSelectPet(userId: String) {
        Log.d(TAG, "=== checkAndSelectPet ===")
        Log.d(TAG, "userId: $userId")

        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Pets found: ${documents.size()}")

                // ✅ PAKAI IF BIASA TANPA RETURN
                if (documents.isEmpty) {
                    Log.d(TAG, "NO PET, navigate to AddPetActivity")
                    Toast.makeText(this, getString(R.string.add_pet_first), Toast.LENGTH_LONG).show()
                    val intent = Intent(this, AddPetActivity::class.java)
                    startActivity(intent)
                    // ✅ TIDAK PAKAI return, LANGSUNG CLOSE LAMBDA
                } else if (documents.size() > 1) {
                    Log.d(TAG, "Multiple pets, show dialog")
                    showPetSelectionDialog(documents)
                } else {
                    val petName = documents.documents[0].getString("name") ?: getString(R.string.default_pet_name)
                    Log.d(TAG, "Pet found: $petName")
                    goToPayment(petName)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get pet data: ${e.message}", e)
                Toast.makeText(this, getString(R.string.error_get_pet, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showPetSelectionDialog(documents: com.google.firebase.firestore.QuerySnapshot) {
        val petNames = documents.map { it.getString("name") ?: getString(R.string.default_pet_name) }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_pet))
            .setItems(petNames.toTypedArray()) { _, which ->
                val selectedPetName = petNames[which]
                Log.d(TAG, "Selected pet: $selectedPetName")
                goToPayment(selectedPetName)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun goToPayment(petName: String) {
        this.petName = petName
        Log.d(TAG, "=== goToPayment ===")
        Log.d(TAG, "petName: $petName")

        val intent = Intent(this, PaymentActivity::class.java).apply {
            putExtra("DOCTOR_ID", doctorId)
            putExtra("DOCTOR_NAME", doctorName)
            putExtra("FEE", fee)
            putExtra("USER_ID", auth.currentUser?.uid ?: "")
            putExtra("PET_NAME", petName)
        }

        // ✅ PAKAI LAUNCHER BARU
        paymentLauncher.launch(intent)
    }
}