package com.example.projecthellopaw.ui.doctor

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.model.Article
import com.example.projecthellopaw.ui.user.ArticleAdapter
import com.google.firebase.firestore.FirebaseFirestore

class DoctorArticleListActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var rvArticles: RecyclerView
    private lateinit var tvEmptyArticle: TextView
    private lateinit var articleAdapter: ArticleAdapter

    private val articleList = mutableListOf<Article>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_article_list)

        val ivBack = findViewById<ImageView>(R.id.ivBackDoctorArticleList)

        rvArticles = findViewById(R.id.rvDoctorArticleList)
        tvEmptyArticle = findViewById(R.id.tvEmptyDoctorArticle)

        ivBack.setOnClickListener {
            finish()
        }

        rvArticles.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter(articleList)
        rvArticles.adapter = articleAdapter

        loadArticles()
    }

    private fun loadArticles() {
        db.collection("articles")
            .get()
            .addOnSuccessListener { documents ->
                articleList.clear()

                if (documents.isEmpty) {
                    tvEmptyArticle.visibility = View.VISIBLE
                    rvArticles.visibility = View.GONE
                } else {
                    for (document in documents) {
                        val article = document.toObject(Article::class.java)
                        articleList.add(article)
                    }

                    tvEmptyArticle.visibility = View.GONE
                    rvArticles.visibility = View.VISIBLE
                    articleAdapter.updateData(articleList)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat artikel: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}