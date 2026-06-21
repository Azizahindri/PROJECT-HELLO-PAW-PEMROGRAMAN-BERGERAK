package com.example.projecthellopaw.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    companion object {
        private const val TAG = "DoctorMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityDoctorMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupArticlesRecyclerView()
            setupMenuGrid()
            setupBackHandler()
            loadDoctorProfile()
            loadArticlesFromFirestore()

            if (intent.getBooleanExtra("OPEN_APPOINTMENT", false)) {
                showAppointmentPage()
                highlightMenu(binding.menuSchedule)
            } else {
                showHomePage()
                highlightMenu(binding.menuHome)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDoctorProfile() {
        try {
            val uid = auth.currentUser?.uid ?: return

            val tvName = binding.headerDoctor.tvDoctorName
            val tvEmail = binding.headerDoctor.tvDoctorEmail
            val ivPhoto = binding.headerDoctor.ivDoctorPhoto
            val ivSettings = binding.headerDoctor.ivDoctorSettings

            tvEmail.text = auth.currentUser?.email ?: ""

            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val name = userDoc.getString("name") ?: "Dokter Hewan"
                    val photoUrl = userDoc.getString("photoUrl") ?: ""

                    tvName.text = name

                    if (photoUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_doctor_placeholder)
                            .circleCrop()
                            .into(ivPhoto)
                    } else {
                        ivPhoto.setImageResource(R.drawable.ic_doctor_placeholder)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal memuat profil dokter", Toast.LENGTH_SHORT).show()
                }

            ivSettings.setOnClickListener {
                startActivity(Intent(this, DoctorSettingsActivity::class.java))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading doctor profile: ${e.message}", e)
        }
    }

    private fun setupArticlesRecyclerView() {
        try {
            binding.rvArticles.layoutManager = LinearLayoutManager(this)
            articleAdapter = ArticleAdapter(articleList)
            binding.rvArticles.adapter = articleAdapter
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up recyclerview: ${e.message}", e)
        }
    }

    private fun loadArticlesFromFirestore() {
        try {
            db.collection("articles")
                .get()
                .addOnSuccessListener { documents ->
                    articleList.clear()

                    if (documents.isEmpty) {
                        Log.d(TAG, "No articles found")
                        articleAdapter.notifyDataSetChanged()
                        return@addOnSuccessListener
                    }

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
                            Log.e(TAG, "Error parsing article: ${e.message}", e)
                        }
                    }

                    Log.d(TAG, "Articles loaded: ${articleList.size}")
                    articleAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to load articles: ${e.message}", e)
                    Toast.makeText(
                        this,
                        "Gagal memuat artikel: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadArticlesFromFirestore: ${e.message}", e)
        }
    }

    private fun setupMenuGrid() {
        try {
            binding.menuHome.setOnClickListener {
                showHomePage()
                highlightMenu(binding.menuHome)
            }

            binding.menuNotification.setOnClickListener {
                startActivity(Intent(this, DoctorNotificationActivity::class.java))
                highlightMenu(binding.menuNotification)
            }

            binding.menuSchedule.setOnClickListener {
                showAppointmentPage()
                highlightMenu(binding.menuSchedule)
            }

            binding.menuArticle.setOnClickListener {
                startActivity(Intent(this, AddArticleActivity::class.java))
                highlightMenu(binding.menuArticle)
            }

            binding.menuPetStuff.setOnClickListener {
                Toast.makeText(this, "Menu Pet Stuff belum tersedia", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up menu: ${e.message}", e)
        }
    }

    private fun highlightMenu(selectedMenu: View) {
        try {
            binding.menuHome.isSelected = false
            binding.menuNotification.isSelected = false
            binding.menuSchedule.isSelected = false
            binding.menuArticle.isSelected = false
            selectedMenu.isSelected = true
        } catch (e: Exception) {
            Log.e(TAG, "Error highlighting menu: ${e.message}", e)
        }
    }

    private fun showHomePage() {
        try {
            binding.scrollView.visibility = View.VISIBLE
            binding.fragmentContainer.visibility = View.GONE

            supportFragmentManager.popBackStack(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing home page: ${e.message}", e)
        }
    }

    private fun showAppointmentPage() {
        try {
            binding.scrollView.visibility = View.GONE
            binding.fragmentContainer.visibility = View.VISIBLE

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AppointmentFragment())
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing appointment page: ${e.message}", e)
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.fragmentContainer.visibility == View.VISIBLE) {
                        showHomePage()
                        highlightMenu(binding.menuHome)
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
        try {
            loadDoctorProfile()
            loadArticlesFromFirestore()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}", e)
        }
    }
}