package com.example.projecthellopaw.ui.user

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
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

data class ReviewItem(
    val id: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val petName: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

class ReviewListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rvReviews: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var adapter: ReviewListAdapter
    private val reviewList = mutableListOf<ReviewItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_list)

        val doctorId = intent.getStringExtra("DOCTOR_ID") ?: ""
        val doctorName = intent.getStringExtra("DOCTOR_NAME") ?: "Dokter"

        val tvTitle = findViewById<TextView>(R.id.tvReviewListTitle)
        tvTitle.text = "Review untuk drh. $doctorName"

        val ivBack = findViewById<ImageView>(R.id.ivBackReviewList)
        ivBack.setOnClickListener { finish() }

        rvReviews = findViewById(R.id.rvReviewList)
        tvEmptyState = findViewById(R.id.tvEmptyReviewList)

        rvReviews.layoutManager = LinearLayoutManager(this)
        adapter = ReviewListAdapter(reviewList)
        rvReviews.adapter = adapter

        loadReviews(doctorId)
    }

    private fun loadReviews(doctorId: String) {
        db.collection("reviews")
            .whereEqualTo("doctorId", doctorId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                reviewList.clear()
                if (documents.isEmpty) {
                    tvEmptyState.visibility = View.VISIBLE
                    rvReviews.visibility = View.GONE
                } else {
                    for (doc in documents) {
                        val item = ReviewItem(
                            id = doc.id,
                            doctorId = doc.getString("doctorId") ?: "",
                            doctorName = doc.getString("doctorName") ?: "",
                            rating = doc.getDouble("rating") ?: 0.0,
                            comment = doc.getString("comment") ?: "",
                            petName = doc.getString("petName") ?: "",
                            timestamp = doc.getTimestamp("timestamp")
                        )
                        reviewList.add(item)
                    }

                    tvEmptyState.visibility = View.GONE
                    rvReviews.visibility = View.VISIBLE
                    adapter.updateData(reviewList)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat review: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

class ReviewListAdapter(
    private var items: List<ReviewItem>
) : RecyclerView.Adapter<ReviewListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPetName: TextView = itemView.findViewById(R.id.tvReviewPetName)
        val tvComment: TextView = itemView.findViewById(R.id.tvReviewComment)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvReviewTimestamp)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvPetName.text = "Hewan: ${item.petName} (Pasien Anonim)"
        holder.tvComment.text = item.comment
        holder.ratingBar.rating = item.rating.toFloat()

        val date = item.timestamp?.toDate()
        val dateStr = date?.let {
            SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(it)
        } ?: ""
        holder.tvTimestamp.text = dateStr
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ReviewItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}