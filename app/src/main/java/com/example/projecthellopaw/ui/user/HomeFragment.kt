package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.example.projecthellopaw.model.Article
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var rvArticles: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var ivClear: ImageView
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ArticleAdapter
    private val articleList = mutableListOf<Article>()
    private val filteredList = mutableListOf<Article>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        rvArticles = view.findViewById(R.id.rv_articles)
        etSearch = view.findViewById(R.id.etSearchHome)
        ivClear = view.findViewById(R.id.ivClearSearchHome)

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        loadArticles()

        // Search Listener
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    ivClear.visibility = View.GONE
                    filterArticles("")
                } else {
                    ivClear.visibility = View.VISIBLE
                    filterArticles(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear Button
        ivClear.setOnClickListener {
            etSearch.setText("")
            ivClear.visibility = View.GONE
            filterArticles("")
        }

        return view
    }

    private fun setupRecyclerView() {
        adapter = ArticleAdapter(filteredList)
        rvArticles.layoutManager = LinearLayoutManager(context)
        rvArticles.adapter = adapter
    }

    private fun loadArticles() {
        db.collection("articles").get()
            .addOnSuccessListener { documents ->
                articleList.clear()
                for (document in documents) {
                    val article = document.toObject(Article::class.java)
                    articleList.add(article)
                }
                filteredList.clear()
                filteredList.addAll(articleList)
                adapter.updateData(filteredList)
            }
            .addOnFailureListener { e ->
                Log.e("HOME_FRAGMENT", "Gagal memuat artikel", e)
                Toast.makeText(context, "Gagal memuat artikel", Toast.LENGTH_SHORT).show()
            }
    }

    // 🔍 FILTER ARTIKEL BERDASARKAN JUDUL
    private fun filterArticles(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(articleList)
        } else {
            val lowerQuery = query.lowercase()
            val filtered = articleList.filter { article ->
                article.title.lowercase().contains(lowerQuery) ||
                        article.description.lowercase().contains(lowerQuery)
            }
            filteredList.addAll(filtered)
        }
        adapter.updateData(filteredList)
    }
}