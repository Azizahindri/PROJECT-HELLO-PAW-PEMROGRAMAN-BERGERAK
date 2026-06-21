package com.example.projecthellopaw.ui.doctor

import android.content.Intent
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
import com.bumptech.glide.Glide
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

        loadDoctorProfile()

        rvNotifications = findViewById(R.id.rvDoctorNotifications)
        tvEmptyNotification = findViewById(R.id.tvEmptyDoctorNotification)

        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = DoctorNotificationAdapter(notificationList)
        rvNotifications.adapter = adapter

        loadDoctorNotifications()
    }

    private fun loadDoctorProfile() {
        val uid = auth.currentUser?.uid ?: return

        val headerView = findViewById<View>(R.id.headerDoctor)
        val tvName = headerView.findViewById<TextView>(R.id.tv_doctor_name)
        val tvEmail = headerView.findViewById<TextView>(R.id.tv_doctor_email)
        val ivPhoto = headerView.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.iv_doctor_photo)
        val ivSettings = headerView.findViewById<ImageView>(R.id.iv_doctor_settings)

        tvEmail.text = auth.currentUser?.email ?: ""

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc.getString("name") ?: "Dokter Hewan"
                val photoUrl = userDoc.getString("photoUrl") ?: ""

                tvName.text = name

                if (photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_doctor_placeholder)
                        .circleCrop()
                        .into(ivPhoto)
                } else {
                    ivPhoto.setImageResource(R.drawable.ic_doctor_placeholder)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Gagal memuat profil dokter",
                    Toast.LENGTH_SHORT
                ).show()
            }

        ivSettings.setOnClickListener {
            startActivity(Intent(this, DoctorSettingsActivity::class.java))
        }
    }

    private fun loadDoctorNotifications() {
        val doctorId = auth.currentUser?.uid

        if (doctorId == null) {
            Toast.makeText(
                this,
                "Dokter belum login",
                Toast.LENGTH_SHORT
            ).show()
            showEmptyState()
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

                if (filteredDocs.isEmpty()) {
                    showEmptyState()
                    return@addOnSuccessListener
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
                    showEmptyState()
                } else {
                    showRecyclerView()
                    adapter.updateData(notificationList)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat notifikasi: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmptyState()
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
                    title = "🟡 Pasien baru menunggu konsultasi",
                    message = "$ownerName menunggu konsultasi untuk $petName · $petType.",
                    status = "Menunggu",
                    createdAtText = createdAtText
                )
            }

            statusLower == "active" -> {
                DoctorNotificationItem(
                    title = "🟢 Konsultasi sedang berlangsung",
                    message = "$ownerName sedang berkonsultasi untuk $petName · $petType.",
                    status = "Aktif",
                    createdAtText = createdAtText
                )
            }

            (statusLower == "completed" || statusLower == "finished") && hasReview -> {
                DoctorNotificationItem(
                    title = "⭐ Rating baru diterima",
                    message = "$ownerName sudah memberi rating untuk konsultasi $petName · $petType.",
                    status = "Sudah Rating",
                    createdAtText = createdAtText
                )
            }

            (statusLower == "completed" || statusLower == "finished") && !hasReview -> {
                DoctorNotificationItem(
                    title = "✅ Konsultasi selesai",
                    message = "Konsultasi $ownerName untuk $petName · $petType sudah selesai.",
                    status = "Selesai",
                    createdAtText = createdAtText
                )
            }

            else -> {
                DoctorNotificationItem(
                    title = "📋 Konsultasi pasien terdaftar",
                    message = "$ownerName memiliki konsultasi untuk $petName · $petType.",
                    status = "Terdaftar",
                    createdAtText = createdAtText
                )
            }
        }
    }

    private fun showEmptyState() {
        tvEmptyNotification.visibility = View.VISIBLE
        rvNotifications.visibility = View.GONE
    }

    private fun showRecyclerView() {
        tvEmptyNotification.visibility = View.GONE
        rvNotifications.visibility = View.VISIBLE
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

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<DoctorNotificationItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}