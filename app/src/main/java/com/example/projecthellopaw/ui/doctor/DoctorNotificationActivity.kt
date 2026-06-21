package com.example.projecthellopaw.ui.doctor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

data class DoctorNotificationItem(
    val title: String = "",
    val message: String = "",
    val status: String = "",
    val createdAtText: String = ""
)

class DoctorNotificationActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var rvNotifications: RecyclerView
    private lateinit var tvEmptyNotification: TextView
    private lateinit var adapter: DoctorNotificationAdapter

    private val notificationList = mutableListOf<DoctorNotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_notification)

        val ivBack = findViewById<ImageView>(R.id.ivBackDoctorNotification)

        rvNotifications = findViewById(R.id.rvDoctorNotifications)
        tvEmptyNotification = findViewById(R.id.tvEmptyDoctorNotification)

        ivBack.setOnClickListener {
            finish()
        }

        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = DoctorNotificationAdapter(notificationList)
        rvNotifications.adapter = adapter

        loadDoctorNotifications()
    }

    private fun loadDoctorNotifications() {
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
                notificationList.clear()

                val filteredDocs = documents.documents.filter { doc ->
                    val paymentStatus = doc.getString("paymentStatus") ?: ""

                    paymentStatus.equals("SUCCESS", ignoreCase = true) ||
                            paymentStatus.equals("success", ignoreCase = true) ||
                            paymentStatus.equals("PAID", ignoreCase = true) ||
                            paymentStatus.equals("paid", ignoreCase = true)
                }.sortedByDescending { doc ->
                    doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }

                for (doc in filteredDocs) {
                    val ownerName = doc.getString("ownerName") ?: "Pemilik Hewan"
                    val petName = doc.getString("petName") ?: "Anabul"
                    val petType = doc.getString("petType") ?: "Hewan"
                    val chatStatus = doc.getString("chatStatus") ?: ""
                    val hasReview = doc.getBoolean("hasReview") ?: false
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()

                    val createdAtText = if (createdAt != null) {
                        SimpleDateFormat(
                            "dd MMM yyyy, HH:mm",
                            Locale("id", "ID")
                        ).format(createdAt)
                    } else {
                        "-"
                    }

                    val notification = buildNotificationItem(
                        ownerName = ownerName,
                        petName = petName,
                        petType = petType,
                        chatStatus = chatStatus,
                        hasReview = hasReview,
                        createdAtText = createdAtText
                    )

                    notificationList.add(notification)
                }

                if (notificationList.isEmpty()) {
                    tvEmptyNotification.visibility = View.VISIBLE
                    rvNotifications.visibility = View.GONE
                } else {
                    tvEmptyNotification.visibility = View.GONE
                    rvNotifications.visibility = View.VISIBLE
                    adapter.updateData(notificationList)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat notifikasi: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()

                tvEmptyNotification.visibility = View.VISIBLE
                rvNotifications.visibility = View.GONE
            }
    }

    private fun buildNotificationItem(
        ownerName: String,
        petName: String,
        petType: String,
        chatStatus: String,
        hasReview: Boolean,
        createdAtText: String
    ): DoctorNotificationItem {
        val statusLower = chatStatus.lowercase()

        return when {
            statusLower == "waiting" || statusLower == "pending" -> {
                DoctorNotificationItem(
                    title = "Pasien baru menunggu konsultasi",
                    message = "$ownerName menunggu konsultasi untuk $petName · $petType.",
                    status = "Menunggu",
                    createdAtText = createdAtText
                )
            }

            statusLower == "active" -> {
                DoctorNotificationItem(
                    title = "Konsultasi sedang berlangsung",
                    message = "$ownerName sedang berkonsultasi untuk $petName · $petType.",
                    status = "Aktif",
                    createdAtText = createdAtText
                )
            }

            statusLower == "completed" && hasReview -> {
                DoctorNotificationItem(
                    title = "Rating baru diterima",
                    message = "$ownerName sudah memberi rating untuk konsultasi $petName · $petType.",
                    status = "Sudah Rating",
                    createdAtText = createdAtText
                )
            }

            statusLower == "completed" && !hasReview -> {
                DoctorNotificationItem(
                    title = "Konsultasi selesai",
                    message = "Konsultasi $ownerName untuk $petName · $petType sudah selesai, tetapi pasien belum memberi rating.",
                    status = "Belum Rating",
                    createdAtText = createdAtText
                )
            }

            statusLower == "finished" && hasReview -> {
                DoctorNotificationItem(
                    title = "Rating baru diterima",
                    message = "$ownerName sudah memberi rating untuk konsultasi $petName · $petType.",
                    status = "Sudah Rating",
                    createdAtText = createdAtText
                )
            }

            statusLower == "finished" && !hasReview -> {
                DoctorNotificationItem(
                    title = "Konsultasi selesai",
                    message = "Konsultasi $ownerName untuk $petName · $petType sudah selesai, tetapi pasien belum memberi rating.",
                    status = "Belum Rating",
                    createdAtText = createdAtText
                )
            }

            else -> {
                DoctorNotificationItem(
                    title = "Konsultasi pasien terdaftar",
                    message = "$ownerName memiliki konsultasi untuk $petName · $petType.",
                    status = "Terdaftar",
                    createdAtText = createdAtText
                )
            }
        }
    }
}

class DoctorNotificationAdapter(
    private var items: List<DoctorNotificationItem>
) : RecyclerView.Adapter<DoctorNotificationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        val tvMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        val tvStatus: TextView = itemView.findViewById(R.id.tvNotificationStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvNotificationDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_notification, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title
        holder.tvMessage.text = item.message
        holder.tvStatus.text = item.status
        holder.tvDate.text = item.createdAtText
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateData(newItems: List<DoctorNotificationItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}