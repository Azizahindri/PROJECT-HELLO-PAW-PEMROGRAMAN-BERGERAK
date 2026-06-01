package com.example.projecthellopaw.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R

class PetAdapter(private val petList: List<PetModel>) :
    RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        val pet = petList[position]

        holder.tvName.text = pet.nama
        holder.tvType.text = "${pet.kategori} • ${pet.jenis}"
        holder.tvAge.text = pet.umur
        holder.tvGender.text = pet.jenisKelamin
    }

    override fun getItemCount(): Int {
        return petList.size
    }

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.img_item_pet_avatar)
        val tvName: TextView = itemView.findViewById(R.id.tv_item_pet_name)
        val tvType: TextView = itemView.findViewById(R.id.tv_item_pet_type)
        val tvAge: TextView = itemView.findViewById(R.id.tv_item_pet_age)
        val tvGender: TextView = itemView.findViewById(R.id.tv_item_pet_gender)
    }
}