package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.FragmentHomeBinding
import com.example.projecthellopaw.model.Article
import com.example.projecthellopaw.ui.user.DoctorListFragment
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val articleList = mutableListOf<Article>()
    private val filteredList = mutableListOf<Article>()
    private lateinit var adapter: ArticleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadArticles()
        setupSearch()
        setupButtons()
    }

    private fun setupRecyclerView() {
        binding.rvArticles.layoutManager = LinearLayoutManager(requireContext())
        adapter = ArticleAdapter(filteredList)
        binding.rvArticles.adapter = adapter
    }

    private fun loadArticles() {
        db.collection("articles")
            .get()
            .addOnSuccessListener { documents ->
                articleList.clear()

                for (document in documents) {
                    try {
                        val id = document.id
                        val title = document.getString("title") ?: ""
                        val description = document.getString("description") ?: ""
                        val thumbnail = document.getString("thumbnail") ?: ""
                        val url = document.getString("url") ?: ""
                        val doctorId = document.getString("doctorId") ?: ""
                        val createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()

                        val article = Article(
                            id = id,
                            title = title,
                            description = description,
                            thumbnail = thumbnail,
                            url = url,
                            doctorId = doctorId,
                            createdAt = createdAt
                        )
                        articleList.add(article)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                filteredList.clear()
                filteredList.addAll(articleList)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat artikel", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearch() {
        binding.etSearchHome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    binding.ivClearSearchHome.visibility = View.GONE
                    filterArticles("")
                } else {
                    binding.ivClearSearchHome.visibility = View.VISIBLE
                    filterArticles(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ivClearSearchHome.setOnClickListener {
            binding.etSearchHome.setText("")
            binding.ivClearSearchHome.visibility = View.GONE
            filterArticles("")
        }
    }

    private fun filterArticles(query: String) {
        filteredList.clear()

        if (query.isEmpty()) {
            filteredList.addAll(articleList)
        } else {
            val lowerQuery = query.lowercase()
            filteredList.addAll(
                articleList.filter { article ->
                    article.title.lowercase().contains(lowerQuery) ||
                            article.description.lowercase().contains(lowerQuery)
                }
            )
        }

        adapter.notifyDataSetChanged()
    }

    private fun setupButtons() {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}