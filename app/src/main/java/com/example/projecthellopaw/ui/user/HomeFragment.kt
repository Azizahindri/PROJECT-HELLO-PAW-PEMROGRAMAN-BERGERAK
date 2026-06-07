package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView // ◄── TAMBAHKAN INI
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.projecthellopaw.R
import com.example.projecthellopaw.model.Article
import com.example.projecthellopaw.ui.auth.LoginActivity // ◄── TAMBAHKAN INI

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvEmail: TextView
    private lateinit var ivLogoutHome: ImageView // ◄── TAMBAHKAN INI
    private lateinit var rvArticles: RecyclerView
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Inisialisasi View Komponen
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvEmail = view.findViewById(R.id.tvEmail)
        ivLogoutHome = view.findViewById(R.id.ivLogoutHome) // ◄── TAMBAHKAN INI

        // RecyclerView Artikel Dinamis
        rvArticles = view.findViewById(R.id.rv_articles)
        rvArticles.layoutManager = LinearLayoutManager(context)

        db = FirebaseFirestore.getInstance()

        // ◄── LOGIKA LOGOUT USER
        ivLogoutHome.setOnClickListener {
            // Hapus sesi login akun dari Firebase Auth
            FirebaseAuth.getInstance().signOut()

            Toast.makeText(context, "Logout Berhasil", Toast.LENGTH_SHORT).show()

            // Tendang balik ke halaman Login dan bersihkan history/stack page
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // Tutup UserMainActivity
            activity?.finish()
        }

        // Ambil data dari Firestore
        loadUserData()
        loadArticlesData()

        return view
    }

    private fun loadUserData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name")
                    val email = document.getString("email")

                    tvGreeting.text = "Hello ${name ?: "User"}!"
                    tvEmail.text = email ?: "email@kosong.com"
                }
            }
    }

    private fun loadArticlesData() {
        val articleList = mutableListOf<Article>()
        db.collection("articles").get()
            .addOnSuccessListener { documents ->
                articleList.clear()
                for (document in documents) {
                    val article = document.toObject(Article::class.java)
                    articleList.add(article)
                }
                val adapter = ArticleAdapter(articleList)
                rvArticles.adapter = adapter
            }
            .addOnFailureListener { e ->
                Log.e("HOME_FRAGMENT", "Gagal memuat artikel", e)
            }
    }
}