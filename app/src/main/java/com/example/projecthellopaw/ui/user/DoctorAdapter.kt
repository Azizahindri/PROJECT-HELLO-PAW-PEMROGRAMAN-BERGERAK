package com.example.projecthellopaw.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.projecthellopaw.R

data class DoctorItem(
    val id: String,
    val name: String,
    val specialization: String,
    val fee: Int,
    val rating: Float,
    val experience: Int,
    val bio: String,
    val isOnline: Boolean,
    val avatarUrl: String = "",
    val totalReviews: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

class DoctorAdapter(
    private var items: List<DoctorItem>,
    private val onItemClick: (DoctorItem) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvSpecialization: TextView = itemView.findViewById(R.id.tvSpecialization)
        val tvExperience: TextView = itemView.findViewById(R.id.tvExperience)
        val tvFee: TextView = itemView.findViewById(R.id.tvFee)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val tvRatingCount: TextView = itemView.findViewById(R.id.tvRatingCount)
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivDoctorAvatar)
        val ivConsultBtn: ImageView = itemView.findViewById(R.id.ivConsultBtn)
        val cardRoot: CardView = itemView.findViewById(R.id.card_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = "drh. ${item.name}"
        holder.tvSpecialization.text = item.specialization
        holder.tvExperience.text = "${item.experience} Tahun"
        holder.tvFee.text = "Rp ${String.format("%,d", item.fee).replace(',', '.')}"
        holder.tvRatingCount.text = if (item.totalReviews > 0) {
            "${item.rating} (${item.totalReviews} ulasan)"
        } else {
            "Belum ada ulasan"
        }
        holder.ratingBar.rating = item.rating
        holder.tvStatus.text = if (item.isOnline) "🟢 Online" else "⚫ Offline"

        if (item.avatarUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_person_placeholer)
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_person_placeholer)
        }

        holder.cardRoot.setOnClickListener { onItemClick(item) }
        holder.ivConsultBtn.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<DoctorItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}