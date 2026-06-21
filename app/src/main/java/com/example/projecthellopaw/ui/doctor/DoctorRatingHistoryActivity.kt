package com.example.projecthellopaw.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.user.ReviewActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

data class DoctorRatingHistoryItem(
    val chatRoomId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val petName: String = "",
    val duration: Int = 0,
    val rating: Double = 0.0,
    val comment: String = "",
    val timestampText: String = ""
)

class DoctorRatingHistoryActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var rvRatingHistory: RecyclerView
    private lateinit var tvEmptyRating: TextView
    private lateinit var adapter: DoctorRatingHistoryAdapter

    private val ratingList = mutableListOf<DoctorRatingHistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_rating_history)

        val ivBack = findViewById<ImageView>(R.id.ivBackDoctorRatingHistory)

        rvRatingHistory = findViewById(R.id.rvDoctorRatingHistory)
        tvEmptyRating = findViewById(R.id.tvEmptyDoctorRating)

        ivBack.setOnClickListener {
            finish()
        }

        rvRatingHistory.layoutManager = LinearLayoutManager(this)

        adapter = DoctorRatingHistoryAdapter(ratingList) { item ->
            val intent = Intent(this, ReviewActivity::class.java)
            intent.putExtra("CHAT_ROOM_ID", item.chatRoomId)
            intent.putExtra("DOCTOR_ID", item.doctorId)
            intent.putExtra("DOCTOR_NAME", item.doctorName)
            intent.putExtra("OWNER_ID", item.ownerId)
            intent.putExtra("PET_NAME", item.petName)
            intent.putExtra("DURATION", item.duration)
            intent.putExtra("IS_READ_ONLY", true)
            startActivity(intent)
        }

        rvRatingHistory.adapter = adapter

        loadDoctorRatings()
    }

    private fun loadDoctorRatings() {
        val doctorId = auth.currentUser?.uid

        if (doctorId == null) {
            Toast.makeText(
                this,
                "Dokter belum login",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        db.collection("reviews")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { documents ->
                ratingList.clear()

                if (documents.isEmpty) {
                    showEmpty()
                    return@addOnSuccessListener
                }

                val sortedDocuments = documents.documents.sortedByDescending { doc ->
                    doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                }

                for (doc in sortedDocuments) {
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()
                    val timestampText = if (timestamp != null) {
                        SimpleDateFormat(
                            "dd MMM yyyy, HH:mm",
                            Locale("id", "ID")
                        ).format(timestamp)
                    } else {
                        ""
                    }

                    val item = DoctorRatingHistoryItem(
                        chatRoomId = doc.getString("chatRoomId") ?: "",
                        doctorId = doc.getString("doctorId") ?: doctorId,
                        doctorName = doc.getString("doctorName") ?: "Dokter",
                        ownerId = doc.getString("ownerId") ?: "",
                        ownerName = "Pasien",
                        petName = doc.getString("petName") ?: "Anabul",
                        duration = doc.getLong("duration")?.toInt() ?: 0,
                        rating = doc.getDouble("rating") ?: 0.0,
                        comment = doc.getString("comment") ?: "",
                        timestampText = timestampText
                    )

                    ratingList.add(item)
                }

                if (ratingList.isEmpty()) {
                    showEmpty()
                } else {
                    tvEmptyRating.visibility = View.GONE
                    rvRatingHistory.visibility = View.VISIBLE
                    adapter.updateData(ratingList)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat rating: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmpty()
            }
    }

    private fun showEmpty() {
        tvEmptyRating.visibility = View.VISIBLE
        rvRatingHistory.visibility = View.GONE
    }
}

class DoctorRatingHistoryAdapter(
    private var items: List<DoctorRatingHistoryItem>,
    private val onItemClick: (DoctorRatingHistoryItem) -> Unit
) : RecyclerView.Adapter<DoctorRatingHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPetName: TextView = itemView.findViewById(R.id.tvRatingPetName)
        val tvComment: TextView = itemView.findViewById(R.id.tvRatingComment)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvRatingTimestamp)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarDoctorHistory)
        val tvReadDetail: TextView = itemView.findViewById(R.id.tvReadRatingDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor_rating_history, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvPetName.text = "Hewan: ${item.petName}"
        holder.tvComment.text = item.comment.ifEmpty { "Tidak ada komentar" }
        holder.tvTimestamp.text = item.timestampText
        holder.ratingBar.rating = item.rating.toFloat()
        holder.tvReadDetail.text = "Lihat detail review"

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.tvReadDetail.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun updateData(newItems: List<DoctorRatingHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}