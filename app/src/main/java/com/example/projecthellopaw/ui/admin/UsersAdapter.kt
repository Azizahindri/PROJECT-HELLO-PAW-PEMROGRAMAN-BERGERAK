package com.example.projecthellopaw.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ItemUserRowBinding
import com.example.projecthellopaw.data.model.User

class UsersAdapter(
    private var userList: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size

    fun updateData(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }

    inner class UserViewHolder(private val binding: ItemUserRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                // Set nama & email
                tvUserName.text = user.name.ifEmpty { "Tidak Ada Nama" }
                tvUserEmail.text = user.email.ifEmpty { "Tidak Ada Email" }

                // Set initial (huruf pertama)
                val initial = if (user.name.isNotEmpty()) user.name.first().toString() else "?"
                tvUserInitial.text = initial.uppercase()

                // Set warna berdasarkan role
                when (user.role) {
                    "DOCTOR" -> {
                        tvUserInitial.setBackgroundResource(R.drawable.bg_avatar_green)
                        tvUserRole.text = "Dokter"
                        tvUserRole.setBackgroundResource(R.drawable.bg_role_badge_green)
                        tvUserRole.setTextColor(ContextCompat.getColor(itemView.context, R.color.green_700))
                    }
                    else -> {
                        tvUserInitial.setBackgroundResource(R.drawable.bg_avatar_red)
                        tvUserRole.text = "Pengguna"
                        tvUserRole.setBackgroundResource(R.drawable.bg_role_badge_red)
                        tvUserRole.setTextColor(ContextCompat.getColor(itemView.context, R.color.red_700))
                    }
                }

                // Event klik
                root.setOnClickListener {
                    onItemClick(user)
                }
            }
        }
    }
}