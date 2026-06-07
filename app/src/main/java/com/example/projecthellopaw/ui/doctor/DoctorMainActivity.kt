package com.example.projecthellopaw.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityDoctorMainBinding
import com.example.projecthellopaw.model.Article
import com.example.projecthellopaw.ui.user.ArticleAdapter
import com.example.projecthellopaw.ui.article.AddArticleActivity
import com.example.projecthellopaw.ui.auth.LoginActivity // ◄── TAMBAHKAN IMPORT INI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DoctorMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val articleList = mutableListOf<Article>()
    private lateinit var articleAdapter: ArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupArticlesRecyclerView()
        setupMenuGrid()
        setupBottomNav()
        loadDoctorProfile()

        // Ambil data artikel asli dari Firestore
        loadArticlesFromFirestore()
    }

    private fun loadDoctorProfile() {
        val uid = auth.currentUser?.uid ?: return
        binding.tvDoctorEmail.text = auth.currentUser?.email ?: ""

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc.getString("name") ?: "drh. I Nyoman Swerdi Jaya"
                binding.tvDoctorName.text = name

                val photoUrl = userDoc.getString("photoUrl") ?: ""
                if (photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_doctor_placeholder)
                        .circleCrop()
                        .into(binding.ivDoctorPhoto)
                }
            }
    }

    private fun setupArticlesRecyclerView() {
        binding.rvArticles.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter(articleList)
        binding.rvArticles.adapter = articleAdapter
    }

    private fun loadArticlesFromFirestore() {
        db.collection("articles").get()
            .addOnSuccessListener { documents ->
                articleList.clear()
                for (document in documents) {
                    val article = document.toObject(Article::class.java)
                    articleList.add(article)
                }
                articleAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat artikel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupMenuGrid() {
        binding.menuNotification.setOnClickListener {
            // Open NotificationActivity
        }
        binding.menuSchedule.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.nav_appointment
        }

        binding.menuArticle.setOnClickListener {
            startActivity(Intent(this, AddArticleActivity::class.java))
        }

        binding.menuPetStuff.setOnClickListener {
            // Open Pet Store (optional, bisa di-hide)
        }

        // ✅ TAMBAHAN LOGIKA: Klik Ikon Logout di Pojok Kanan Atas Header
        binding.ivDoctorLogout.setOnClickListener {
            // 1. Hancurkan sesi autentikasi akun Dokter di Firebase
            auth.signOut()

            Toast.makeText(this, "Dokter Logout Berhasil", Toast.LENGTH_SHORT).show()

            // 2. Tendang kembali ke halaman Login & bersihkan stack activity sebelumnya
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // 3. Tutup halaman utama dokter agar tidak bisa ditekan tombol back HP
            finish()
        }

        // Catatan: Jika ivNotificationBell masih dipertahankan di XML, biarkan baris klik ini ada.
        // Jika sudah diganti total oleh ivDoctorLogout, baris di bawah ini bisa kamu hapus aman.
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.scrollView.visibility = View.VISIBLE
                    supportFragmentManager.popBackStack()
                    true
                }
                R.id.nav_appointment -> {
                    binding.scrollView.visibility = View.GONE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AppointmentFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, EditDoctorProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadArticlesFromFirestore()
    }
}