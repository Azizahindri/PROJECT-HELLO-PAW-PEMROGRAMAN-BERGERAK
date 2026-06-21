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
import com.bumptech.glide.Glide
import com.example.projecthellopaw.R
import com.example.projecthellopaw.model.Article
import java.text.SimpleDateFormat
import java.util.*

class ArticleAdapter(
    private val articles: List<Article>
) : RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivArticleThumbnail)
        val tvTitle: TextView = itemView.findViewById(R.id.tvArticleTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvArticleDescription)
        val tvDate: TextView = itemView.findViewById(R.id.tvArticleDate)
        val tvReadMore: TextView = itemView.findViewById(R.id.tv_read_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val article = articles[position]

            holder.tvTitle.text = article.title
            holder.tvDescription.text = article.description

            val date = Date(article.createdAt)
            val format = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
            holder.tvDate.text = format.format(date)

            if (article.thumbnail.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(article.thumbnail)
                    .placeholder(R.drawable.ic_pet_placeholder)
                    .into(holder.ivThumbnail)
            } else {
                holder.ivThumbnail.setImageResource(R.drawable.ic_pet_placeholder)
            }

            holder.itemView.setOnClickListener {
                try {
                    if (article.url.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                        holder.itemView.context.startActivity(intent)
                    } else {
                        Toast.makeText(
                            holder.itemView.context,
                            "Tidak ada tautan artikel",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        holder.itemView.context,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            holder.tvReadMore.setOnClickListener {
                try {
                    if (article.url.isNotEmpty()) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                        holder.itemView.context.startActivity(intent)
                    } else {
                        Toast.makeText(
                            holder.itemView.context,
                            "Tidak ada tautan artikel",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        holder.itemView.context,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = articles.size
}