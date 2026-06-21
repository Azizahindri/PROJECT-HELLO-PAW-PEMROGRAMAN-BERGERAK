package com.example.projecthellopaw.ui.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

data class DoctorPatientItem(
    val chatRoomId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val petName: String = "",
    val petType: String = "",
    val chatStatus: String = "",
    val paymentStatus: String = "",
    val hasReview: Boolean = false,
    val duration: Int = 0,
    val createdAtText: String = ""
)

class DoctorPatientListActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var rvPatientList: RecyclerView
    private lateinit var tvEmptyPatient: TextView
    private lateinit var adapter: DoctorPatientAdapter

    private val patientList = mutableListOf<DoctorPatientItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_patient_list)

        val ivBack = findViewById<ImageView>(R.id.ivBackDoctorPatientList)

        rvPatientList = findViewById(R.id.rvDoctorPatientList)
        tvEmptyPatient = findViewById(R.id.tvEmptyDoctorPatient)

        ivBack.setOnClickListener {
            finish()
        }

        rvPatientList.layoutManager = LinearLayoutManager(this)

        adapter = DoctorPatientAdapter(patientList) { patient ->
            showPatientDetailDialog(patient)
        }

        rvPatientList.adapter = adapter

        loadPatients()
    }

    private fun loadPatients() {
        val doctorId = auth.currentUser?.uid

        if (doctorId == null) {
            Toast.makeText(
                this,
                "Dokter belum login",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        db.collection("chat_rooms")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { documents ->
                patientList.clear()

                val filteredDocs = documents.documents.filter { doc ->
                    val paymentStatus = doc.getString("paymentStatus") ?: ""

                    paymentStatus.equals("SUCCESS", ignoreCase = true) ||
                            paymentStatus.equals("success", ignoreCase = true)
                }.sortedByDescending { doc ->
                    doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                for (doc in filteredDocs) {
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()

                    val createdAtText = if (createdAt != null) {
                        SimpleDateFormat(
                            "dd MMM yyyy, HH:mm",
                            Locale("id", "ID")
                        ).format(createdAt)
                    } else {
                        "-"
                    }

                    val item = DoctorPatientItem(
                        chatRoomId = doc.getString("chatRoomId") ?: doc.id,
                        ownerId = doc.getString("ownerId") ?: "",
                        ownerName = doc.getString("ownerName") ?: "Pemilik Hewan",
                        doctorId = doc.getString("doctorId") ?: doctorId,
                        doctorName = doc.getString("doctorName") ?: "Dokter",
                        petName = doc.getString("petName") ?: "Anabul",
                        petType = doc.getString("petType") ?: "Hewan",
                        chatStatus = doc.getString("chatStatus") ?: "",
                        paymentStatus = doc.getString("paymentStatus") ?: "",
                        hasReview = doc.getBoolean("hasReview") ?: false,
                        duration = doc.getLong("duration")?.toInt() ?: 0,
                        createdAtText = createdAtText
                    )

                    patientList.add(item)
                }

                if (patientList.isEmpty()) {
                    tvEmptyPatient.visibility = View.VISIBLE
                    rvPatientList.visibility = View.GONE
                } else {
                    tvEmptyPatient.visibility = View.GONE
                    rvPatientList.visibility = View.VISIBLE
                    adapter.updateData(patientList)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat profil pasien: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                tvEmptyPatient.visibility = View.VISIBLE
                rvPatientList.visibility = View.GONE
            }
    }

    private fun showPatientDetailDialog(patient: DoctorPatientItem) {
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_patient_profile_detail,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val tvName = dialogView.findViewById<TextView>(R.id.tvDialogPatientName)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tvDialogPatientEmail)
        val tvPhone = dialogView.findViewById<TextView>(R.id.tvDialogPatientPhone)
        val tvAddress = dialogView.findViewById<TextView>(R.id.tvDialogPatientAddress)

        val tvCurrentPet = dialogView.findViewById<TextView>(R.id.tvDialogCurrentPet)
        val tvConsultationStatus = dialogView.findViewById<TextView>(R.id.tvDialogConsultationStatus)
        val tvReviewStatus = dialogView.findViewById<TextView>(R.id.tvDialogReviewStatus)
        val tvConsultationDate = dialogView.findViewById<TextView>(R.id.tvDialogConsultationDate)

        val tvPatientPets = dialogView.findViewById<TextView>(R.id.tvDialogPatientPets)
        val tvPatientHistory = dialogView.findViewById<TextView>(R.id.tvDialogPatientHistory)
        val tvClose = dialogView.findViewById<TextView>(R.id.tvClosePatientDialog)

        tvName.text = "Nama: ${patient.ownerName}"

        // Untuk versi stabil dulu, data akun lengkap tidak dipaksa ambil dari users.
        // Ini mencegah crash karena field user tidak konsisten.
        tvEmail.text = "Email: -"
        tvPhone.text = "No. HP: -"
        tvAddress.text = "Alamat: -"

        tvCurrentPet.text = "${patient.petName} · ${patient.petType}"

        tvConsultationStatus.text = "Status konsultasi: ${formatChatStatus(patient.chatStatus)}"

        tvReviewStatus.text = if (patient.hasReview) {
            "Rating: Pasien sudah memberi rating"
        } else {
            "Rating: Pasien belum memberi rating"
        }

        tvConsultationDate.text = "Tanggal: ${patient.createdAtText}"

        tvPatientPets.text = "${patient.petName} · ${patient.petType}"

        tvPatientHistory.text = """
        Hewan: ${patient.petName} · ${patient.petType}
        Status: ${formatChatStatus(patient.chatStatus)}
        Pembayaran: ${patient.paymentStatus}
        Durasi: ${patient.duration} menit
        Rating: ${if (patient.hasReview) "Sudah rating" else "Belum rating"}
        Tanggal: ${patient.createdAtText}
    """.trimIndent()

        tvClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadPatientAccountForUi(
        patient: DoctorPatientItem,
        tvName: TextView,
        tvEmail: TextView,
        tvPhone: TextView,
        tvAddress: TextView
    ) {
        if (patient.ownerId.isEmpty()) {
            return
        }

        db.collection("users")
            .document(patient.ownerId)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: patient.ownerName
                val email = doc.getString("email") ?: "-"
                val phone = doc.getString("phoneNumber") ?: "-"
                val address = doc.getString("address") ?: "-"

                tvName.text = "Nama: $name"
                tvEmail.text = "Email: $email"
                tvPhone.text = "No. HP: $phone"
                tvAddress.text = "Alamat: $address"
            }
            .addOnFailureListener {
                tvName.text = "Nama: ${patient.ownerName}"
                tvEmail.text = "Email: -"
                tvPhone.text = "No. HP: -"
                tvAddress.text = "Alamat: -"
            }
    }

    private fun loadPatientPetsForUi(
        patient: DoctorPatientItem,
        tvPatientPets: TextView
    ) {
        if (patient.ownerId.isEmpty()) {
            tvPatientPets.text = "Belum ada data hewan."
            return
        }

        db.collection("pets")
            .whereEqualTo("ownerId", patient.ownerId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    tvPatientPets.text = "Belum ada data hewan."
                    return@addOnSuccessListener
                }

                val builder = StringBuilder()

                for ((index, doc) in documents.withIndex()) {
                    val petName = doc.getString("name")
                        ?: doc.getString("petName")
                        ?: "Hewan"

                    val petType = doc.getString("type")
                        ?: doc.getString("petType")
                        ?: doc.getString("jenis")
                        ?: "Jenis tidak diketahui"

                    val petAge = doc.getLong("age")
                        ?: doc.getLong("petAge")

                    builder.append("${index + 1}. $petName · $petType")

                    if (petAge != null) {
                        builder.append(" · $petAge tahun")
                    }

                    builder.append("\n")
                }

                tvPatientPets.text = builder.toString().trim()
            }
            .addOnFailureListener {
                tvPatientPets.text = "Gagal memuat data hewan."
            }
    }

    private fun loadPatientHistoryForUi(
        patient: DoctorPatientItem,
        tvPatientHistory: TextView
    ) {
        if (patient.ownerId.isEmpty()) {
            tvPatientHistory.text = "Belum ada riwayat konsultasi."
            return
        }

        db.collection("chat_rooms")
            .whereEqualTo("ownerId", patient.ownerId)
            .get()
            .addOnSuccessListener { documents ->
                val filteredDocs = documents.documents.filter { doc ->
                    val docDoctorId = doc.getString("doctorId") ?: ""
                    val paymentStatus = doc.getString("paymentStatus") ?: ""

                    val paid = paymentStatus.equals("SUCCESS", ignoreCase = true) ||
                            paymentStatus.equals("success", ignoreCase = true)

                    docDoctorId == patient.doctorId && paid
                }.sortedByDescending { doc ->
                    doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                if (filteredDocs.isEmpty()) {
                    tvPatientHistory.text = "Belum ada riwayat konsultasi."
                    return@addOnSuccessListener
                }

                val builder = StringBuilder()

                for ((index, doc) in filteredDocs.withIndex()) {
                    val petName = doc.getString("petName") ?: "Anabul"
                    val petType = doc.getString("petType") ?: "Hewan"
                    val status = doc.getString("chatStatus") ?: "-"
                    val hasReview = doc.getBoolean("hasReview") ?: false
                    val duration = doc.getLong("duration")?.toInt() ?: 0
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

                    val reviewText = if (hasReview) {
                        "Sudah rating"
                    } else {
                        "Belum rating"
                    }

                    builder.append("${index + 1}. $petName · $petType\n")
                    builder.append("   Tanggal: $dateText\n")
                    builder.append("   Status: $statusText\n")
                    builder.append("   Durasi: $duration menit\n")
                    builder.append("   Review: $reviewText\n\n")
                }

                tvPatientHistory.text = builder.toString().trim()
            }
            .addOnFailureListener {
                tvPatientHistory.text = "Gagal memuat riwayat konsultasi."
            }
    }

    private fun formatChatStatus(status: String): String {
        return when (status.lowercase()) {
            "active" -> "Sedang Konsultasi"
            "completed" -> "Selesai Konsultasi"
            else -> "Pasien Terdaftar"
        }
    }
}

class DoctorPatientAdapter(
    private var items: List<DoctorPatientItem>,
    private val onItemClick: (DoctorPatientItem) -> Unit
) : RecyclerView.Adapter<DoctorPatientAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOwnerName: TextView = itemView.findViewById(R.id.tvPatientOwnerName)
        val tvPetInfo: TextView = itemView.findViewById(R.id.tvPatientPetInfo)
        val tvStatus: TextView = itemView.findViewById(R.id.tvPatientStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_patient, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvOwnerName.text = item.ownerName
        holder.tvPetInfo.text = "${item.petName} · ${item.petType}"
        holder.tvStatus.text = formatStatus(item.chatStatus)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateData(newItems: List<DoctorPatientItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun formatStatus(status: String): String {
        return when (status.lowercase()) {
            "active" -> "Sedang Konsultasi"
            "completed" -> "Selesai Konsultasi"
            else -> "Pasien Terdaftar"
        }
    }
}