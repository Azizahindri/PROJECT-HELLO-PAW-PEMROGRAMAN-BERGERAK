package com.example.projecthellopaw.ui.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.data.model.UserAdminModel

class UsersAdapter(
    private val usersList: List<UserAdminModel>,
    private val roleType: String
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_user_name)
        val ivIcon: ImageView = itemView.findViewById(R.id.iv_user_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_row, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = usersList[position]
        holder.tvName.text = user.name

        // Sesuaikan warna tema ikon berdasarkan role seperti di Figma
        if (roleType == "DOCTOR") {
            holder.tvName.setTextColor(Color.parseColor("#2E7D32")) // Teks Hijau Dokter
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_compass) // Ganti ikon beda jika perlu
            holder.ivIcon.setColorFilter(Color.parseColor("#2E7D32"))
        } else {
            holder.tvName.setTextColor(Color.parseColor("#C62828")) // Teks Merah Owner
            holder.ivIcon.setColorFilter(Color.parseColor("#C62828"))
        }
    }

    override fun getItemCount(): Int = usersList.size
}