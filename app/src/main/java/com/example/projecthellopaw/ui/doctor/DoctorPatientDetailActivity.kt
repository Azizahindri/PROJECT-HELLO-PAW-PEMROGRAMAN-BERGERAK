package com.example.projecthellopaw.ui.doctor

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class DoctorPatientDetailActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var chatRoomId = ""
    private var ownerId = ""
    private var ownerName = ""
    private var doctorId = ""
    private var doctorName = ""
    private var petName = ""
    private var petType = ""
    private var chatStatus = ""
    private var paymentStatus = ""
    private var hasReview = false
    private var duration = 0

    private lateinit var tvPatientName: TextView
    private lateinit var tvPatientEmail: TextView
    private lateinit var tvPatientPhone: TextView
    private lateinit var tvPatientAddress: TextView
    private lateinit var tvPatientGender: TextView
    private lateinit var tvPatientBirthDate: TextView
    private lateinit var tvCurrentPetInfo: TextView
    private lateinit var tvConsultationStatus: TextView
    private lateinit var tvReviewStatus: TextView
    private lateinit var tvPatientPets: TextView
    private lateinit var tvPatientConsultationHistory: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_patient_detail)

        getIntentData()
        bindViews()
        setupInitialData()
        loadPatientAccount()
        loadPatientPets()
        loadPatientConsultationHistory()
    }

    private fun getIntentData() {
        chatRoomId = intent.getStringExtra("CHAT_ROOM_ID") ?: ""
        ownerId = intent.getStringExtra("OWNER_ID") ?: ""
        ownerName = intent.getStringExtra("OWNER_NAME") ?: "Pemilik Hewan"
        doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dokter"
        petName = intent.getStringExtra("PET_NAME") ?: "Anabul"
        petType = intent.getStringExtra("PET_TYPE") ?: "Hewan"
        chatStatus = intent.getStringExtra("CHAT_STATUS") ?: ""
        paymentStatus = intent.getStringExtra("PAYMENT_STATUS") ?: ""
        hasReview = intent.getBooleanExtra("HAS_REVIEW", false)
        duration = intent.getIntExtra("DURATION", 0)
    }

    private fun bindViews() {
        val ivBack = findViewById<ImageView>(R.id.ivBackDoctorPatientDetail)

        tvPatientName = findViewById(R.id.tvDetailPatientName)
        tvPatientEmail = findViewById(R.id.tvDetailPatientEmail)
        tvPatientPhone = findViewById(R.id.tvDetailPatientPhone)
        tvPatientAddress = findViewById(R.id.tvDetailPatientAddress)
        tvPatientGender = findViewById(R.id.tvDetailPatientGender)
        tvPatientBirthDate = findViewById(R.id.tvDetailPatientBirthDate)
        tvCurrentPetInfo = findViewById(R.id.tvDetailCurrentPet)
        tvConsultationStatus = findViewById(R.id.tvDetailConsultationStatus)
        tvReviewStatus = findViewById(R.id.tvDetailReviewStatus)
        tvPatientPets = findViewById(R.id.tvDetailPatientPets)
        tvPatientConsultationHistory = findViewById(R.id.tvDetailPatientConsultationHistory)

        ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupInitialData() {
        tvPatientName.text = ownerName
        tvCurrentPetInfo.text = "$petName · $petType"

        tvConsultationStatus.text = when (chatStatus.lowercase()) {
            "active" -> "Status konsultasi: Sedang berlangsung"
            "completed" -> "Status konsultasi: Selesai"
            else -> "Status konsultasi: Terdaftar"
        }

        tvReviewStatus.text = if (hasReview) {
            "Rating: Pasien sudah memberi rating"
        } else {
            "Rating: Pasien belum memberi rating"
        }
    }

    private fun loadPatientAccount() {
        if (ownerId.isEmpty()) {
            tvPatientEmail.text = "Email: -"
            tvPatientPhone.text = "No. HP: -"
            tvPatientAddress.text = "Alamat: -"
            tvPatientGender.text = "Jenis Kelamin: -"
            tvPatientBirthDate.text = "Tanggal Lahir: -"
            return
        }

        db.collection("users")
            .document(ownerId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("name") ?: ownerName
                    val email = doc.getString("email") ?: "-"
                    val phone = doc.getString("phoneNumber") ?: "-"
                    val address = doc.getString("address") ?: "-"
                    val gender = doc.getString("gender") ?: "-"
                    val birthDate = doc.getString("birthDate") ?: "-"

                    tvPatientName.text = name
                    tvPatientEmail.text = "Email: $email"
                    tvPatientPhone.text = "No. HP: $phone"
                    tvPatientAddress.text = "Alamat: $address"
                    tvPatientGender.text = "Jenis Kelamin: $gender"
                    tvPatientBirthDate.text = "Tanggal Lahir: $birthDate"
                } else {
                    setDefaultAccountInfo()
                }
            }
            .addOnFailureListener {
                setDefaultAccountInfo()
            }
    }

    private fun setDefaultAccountInfo() {
        tvPatientEmail.text = "Email: -"
        tvPatientPhone.text = "No. HP: -"
        tvPatientAddress.text = "Alamat: -"
        tvPatientGender.text = "Jenis Kelamin: -"
        tvPatientBirthDate.text = "Tanggal Lahir: -"
    }

    private fun loadPatientPets() {
        if (ownerId.isEmpty()) {
            tvPatientPets.text = "Belum ada data hewan."
            return
        }

        db.collection("pets")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvPatientPets.text = "Belum ada data hewan."
                    return@addOnSuccessListener
                }

                val builder = StringBuilder()

                for ((index, doc) in documents.withIndex()) {
                    val name = doc.getString("name")
                        ?: doc.getString("petName")
                        ?: "Hewan"

                    val type = doc.getString("type")
                        ?: doc.getString("petType")
                        ?: doc.getString("jenis")
                        ?: "Jenis tidak diketahui"

                    val age = doc.getLong("age")
                        ?: doc.getLong("petAge")

                    builder.append("${index + 1}. $name · $type")

                    if (age != null) {
                        builder.append(" · $age tahun")
                    }

                    builder.append("\n")
                }

                tvPatientPets.text = builder.toString().trim()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat hewan pasien: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                tvPatientPets.text = "Gagal memuat data hewan."
            }
    }

    private fun loadPatientConsultationHistory() {
        val currentDoctorId = doctorId.ifEmpty {
            auth.currentUser?.uid ?: ""
        }

        if (ownerId.isEmpty() || currentDoctorId.isEmpty()) {
            tvPatientConsultationHistory.text = "Belum ada riwayat konsultasi."
            return
        }

        db.collection("chat_rooms")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { documents ->
                val filteredDocs = documents.documents.filter { doc ->
                    val docDoctorId = doc.getString("doctorId") ?: ""
                    val payment = doc.getString("paymentStatus") ?: ""

                    val paid = payment.equals("SUCCESS", ignoreCase = true) ||
                            payment.equals("success", ignoreCase = true)

                    docDoctorId == currentDoctorId && paid
                }.sortedByDescending { doc ->
                    doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                if (filteredDocs.isEmpty()) {
                    tvPatientConsultationHistory.text = "Belum ada riwayat konsultasi."
                    return@addOnSuccessListener
                }

                val builder = StringBuilder()

                for ((index, doc) in filteredDocs.withIndex()) {
                    val historyPetName = doc.getString("petName") ?: "Anabul"
                    val historyPetType = doc.getString("petType") ?: "Hewan"
                    val status = doc.getString("chatStatus") ?: "-"
                    val review = doc.getBoolean("hasReview") ?: false
                    val timestamp = doc.getTimestamp("createdAt")?.toDate()

                    val dateText = if (timestamp != null) {
                        SimpleDateFormat(
                            "dd MMM yyyy, HH:mm",
                            Locale("id", "ID")
                        ).format(timestamp)
                    } else {
                        "-"
                    }

                    val statusText = when (status.lowercase()) {
                        "active" -> "Sedang berlangsung"
                        "completed" -> "Selesai"
                        else -> "Terdaftar"
                    }

                    val reviewText = if (review) {
                        "Sudah rating"
                    } else {
                        "Belum rating"
                    }

                    builder.append("${index + 1}. $historyPetName · $historyPetType\n")
                    builder.append("   Tanggal: $dateText\n")
                    builder.append("   Status: $statusText\n")
                    builder.append("   Review: $reviewText\n\n")
                }

                tvPatientConsultationHistory.text = builder.toString().trim()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat riwayat pasien: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                tvPatientConsultationHistory.text = "Gagal memuat riwayat konsultasi."
            }
    }
}