package com.example.projecthellopaw.ui.doctor

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityDoctorArticleListBinding
import com.example.projecthellopaw.model.Article
import com.example.projecthellopaw.ui.user.ArticleAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorArticleListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorArticleListBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val articleList = mutableListOf<Article>()
    private lateinit var adapter: ArticleAdapter

    companion object {
        private const val TAG = "DoctorArticleList"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorArticleListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupRecyclerView()
        loadArticles()
    }

    private fun setupBackButton() {
        binding.ivBackDoctorArticleList.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.rvDoctorArticleList.layoutManager = LinearLayoutManager(this)
        adapter = ArticleAdapter(articleList)
        binding.rvDoctorArticleList.adapter = adapter
    }

    private fun loadArticles() {
        val doctorId = auth.currentUser?.uid
        if (doctorId.isNullOrEmpty()) {
            Toast.makeText(this, "Dokter belum login", Toast.LENGTH_SHORT).show()
            showEmptyState()
            return
        }

        db.collection("articles")
            .whereEqualTo("doctorId", doctorId)
            .get()
            .addOnSuccessListener { documents ->
                articleList.clear()

                if (documents.isEmpty) {
                    Log.d(TAG, "No articles found")
                    showEmptyState()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    try {
                        val id = document.id
                        val title = document.getString("title") ?: ""
                        val description = document.getString("description") ?: ""
                        val thumbnail = document.getString("thumbnail") ?: ""
                        val url = document.getString("url") ?: ""
                        val doctorIdDoc = document.getString("doctorId") ?: ""
                        val createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()

                        val article = Article(
                            id = id,
                            title = title,
                            description = description,
                            thumbnail = thumbnail,
                            url = url,
                            doctorId = doctorIdDoc,
                            createdAt = createdAt
                        )
                        articleList.add(article)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing article: ${e.message}", e)
                    }
                }

                Log.d(TAG, "Articles loaded: ${articleList.size}")

                if (articleList.isEmpty()) {
                    showEmptyState()
                } else {
                    showRecyclerView()
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load articles: ${e.message}", e)
                Toast.makeText(this, "Gagal memuat artikel: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun showEmptyState() {
        binding.tvEmptyDoctorArticle.visibility = View.VISIBLE
        binding.rvDoctorArticleList.visibility = View.GONE
    }

    private fun showRecyclerView() {
        binding.tvEmptyDoctorArticle.visibility = View.GONE
        binding.rvDoctorArticleList.visibility = View.VISIBLE
    }
}