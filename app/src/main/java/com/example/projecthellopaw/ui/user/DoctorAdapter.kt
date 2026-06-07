package com.example.projecthellopaw.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // ◄── TAMBAHKAN INI
import com.example.projecthellopaw.R

class DoctorAdapter(
    private val doctors: List<DoctorItem>,
    private val onItemClick: (DoctorItem) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDoctorName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvSpecialization: TextView = itemView.findViewById(R.id.tvSpecialization)
        val tvExperience: TextView = itemView.findViewById(R.id.tvExperience)
        val tvFee: TextView = itemView.findViewById(R.id.tvFee)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBar)
        val tvRatingCount: TextView = itemView.findViewById(R.id.tvRatingCount)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val ivDoctorAvatar: ImageView = itemView.findViewById(R.id.ivDoctorAvatar)
        val ivConsultBtn: ImageView = itemView.findViewById(R.id.ivConsultBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctors[position]

        holder.tvDoctorName.text = "drh. ${doctor.name}"
        holder.tvSpecialization.text = doctor.specialization
        holder.tvExperience.text = "${doctor.experience} Tahun"
        holder.tvFee.text = "Rp ${String.format("%,d", doctor.fee).replace(',', '.')},00"
        holder.ratingBar.rating = doctor.rating
        holder.tvRatingCount.text = String.format("%.1f", doctor.rating)

        // ◄── TAMBAHAN LOGIKA STATUS ONLINE / OFFLINE DOKTER
        if (doctor.isOnline) {
            holder.tvStatus.text = "Online"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark))
        } else {
            holder.tvStatus.text = "Offline"
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.darker_gray))
        }

        // ◄── TAMBAHAN LOAD AVATAR DOKTER DENGAN GLIDE
        // 🔍 CEK DI DOCTORADAPTER.KT
        if (doctor.avatarUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(doctor.avatarUrl)
                .placeholder(R.drawable.ic_doctor_placeholder) // Gambar default jika loading
                .circleCrop()
                .into(holder.ivDoctorAvatar)
        } else {
            holder.ivDoctorAvatar.setImageResource(R.drawable.ic_doctor_placeholder) // Gambar default jika kosong
        }

        // Klik seluruh card
        holder.itemView.setOnClickListener { onItemClick(doctor) }

        // Klik tombol konsultasi kecil di kanan
        holder.ivConsultBtn.setOnClickListener { onItemClick(doctor) }
    }

    override fun getItemCount(): Int = doctors.size
}