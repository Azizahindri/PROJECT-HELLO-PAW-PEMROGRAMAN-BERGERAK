package com.example.projecthellopaw.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityDoctorMainBinding
import com.example.projecthellopaw.model.Article
import com.example.projecthellopaw.ui.article.AddArticleActivity
import com.example.projecthellopaw.ui.user.ArticleAdapter
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
        setupBackHandler()
        loadDoctorProfile()
        loadArticlesFromFirestore()

        if (intent.getBooleanExtra("OPEN_APPOINTMENT", false)) {
            showAppointmentPage()
        } else {
            showHomePage()
        }
    }

    private fun loadDoctorProfile() {
        val uid = auth.currentUser?.uid ?: return

        binding.tvDoctorEmail.text = auth.currentUser?.email ?: ""

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                val name = userDoc.getString("name") ?: "Dokter Hewan"
                val photoUrl = userDoc.getString("photoUrl") ?: ""

                binding.tvDoctorName.text = name

                if (photoUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_doctor_placeholder)
                        .circleCrop()
                        .into(binding.ivDoctorPhoto)
                } else {
                    binding.ivDoctorPhoto.setImageResource(R.drawable.ic_doctor_placeholder)
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Gagal memuat profil dokter",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupArticlesRecyclerView() {
        binding.rvArticles.layoutManager = LinearLayoutManager(this)
        articleAdapter = ArticleAdapter(articleList)
        binding.rvArticles.adapter = articleAdapter
    }

    private fun loadArticlesFromFirestore() {
        db.collection("articles")
            .get()
            .addOnSuccessListener { documents ->
                articleList.clear()

                for (document in documents) {
                    val article = document.toObject(Article::class.java)
                    articleList.add(article)
                }

                articleAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat artikel: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupMenuGrid() {
        binding.menuNotification.setOnClickListener {
            startActivity(
                Intent(this, DoctorNotificationActivity::class.java)
            )
        }

        binding.menuSchedule.setOnClickListener {
            showAppointmentPage()
        }

        binding.menuArticle.setOnClickListener {
            startActivity(
                Intent(this, AddArticleActivity::class.java)
            )
        }

        binding.menuPetStuff.setOnClickListener {
            Toast.makeText(
                this,
                "Menu Pet Stuff belum tersedia",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.ivDoctorSettings.setOnClickListener {
            startActivity(
                Intent(this, DoctorSettingsActivity::class.java)
            )
        }
    }

    private fun showHomePage() {
        binding.scrollView.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE

        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
    }

    private fun showAppointmentPage() {
        binding.scrollView.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AppointmentFragment())
            .commit()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.fragmentContainer.visibility == View.VISIBLE) {
                        showHomePage()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        loadDoctorProfile()
        loadArticlesFromFirestore()
    }
}