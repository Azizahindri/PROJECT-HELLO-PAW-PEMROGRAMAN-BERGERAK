package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.model.Article
import com.bumptech.glide.Glide

class ArticleAdapter(private val listArticle: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.iv_thumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvDescription: TextView = view.findViewById(R.id.tv_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = listArticle[position]
        holder.tvTitle.text = article.title
        holder.tvDescription.text = article.description

        if (article.thumbnail.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(article.thumbnail)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.ivThumbnail)
        }

        // LOGIKA KLIK: Mengarah langsung ke link browser asli
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val urlArtikel = article.url

            if (!urlArtikel.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(urlArtikel)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Link artikel tidak tersedia!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = listArticle.size
}