package com.example.projecthellopaw.ui.user

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.model.Article

class ArticleAdapter(
    private var items: List<Article>
) : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        val tvReadMore: TextView = itemView.findViewById(R.id.tv_read_more)
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val cardRoot: CardView = itemView.findViewById(R.id.card_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description
        holder.tvReadMore.text = "Baca selengkapnya →"

        // ✅ PERBAIKAN: Hapus isNotEmpty()
        val imageUrl = item.imageUrl
        if (imageUrl != null) {
            // Gunakan Glide jika ada
            // Glide.with(holder.itemView.context).load(imageUrl).into(holder.ivThumbnail)
        }

        holder.cardRoot.setOnClickListener {
            // Buka detail artikel
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Article>) {
        items = newItems
        notifyDataSetChanged()
    }
}