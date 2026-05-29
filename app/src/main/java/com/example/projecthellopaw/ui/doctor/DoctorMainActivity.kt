package com.example.projecthellopaw.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.ActivityDoctorMainBinding

data class Article(
    val title: String,
    val description: String,
    val imageUrl: String = ""
)

class ArticleAdapter(private val articles: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder>() {

    inner class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        val tvReadMore: TextView = itemView.findViewById(R.id.tv_read_more)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_article, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.tvTitle.text = article.title
        holder.tvDescription.text = article.description
        if (article.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(article.imageUrl)
                .placeholder(R.drawable.ic_pet_placeholder)
                .into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_pet_placeholder)
        }
        holder.tvReadMore.setOnClickListener { /* buka artikel */ }
    }

    override fun getItemCount() = articles.size
}

class DoctorMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDoctorMainBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val sampleArticles = listOf(
        Article(
            "7 Penyebab Kucing Jadi Pendiam dan Cara Mengatasinya",
            "Kucing yang tiba-tiba menjadi pendiam bisa menjadi tanda adanya masalah kesehatan atau stres. Simak penyebab dan cara mengatasinya."
        ),
        Article(
            "Pentingnya Vaksinasi Pada Hewan Peliharaan",
            "Vaksinasi sangat penting untuk melindungi hewan peliharaan dari berbagai penyakit berbahaya. Kenali jadwal vaksinasi yang tepat."
        ),
        Article(
            "4 Makanan untuk Tingkatkan Imunitas Burung Peliharaan",
            "Burung peliharaan juga membutuhkan asupan nutrisi yang tepat untuk menjaga daya tahan tubuhnya."
        ),
        Article(
            "Cara Merawat Hewan Peliharaan dengan Baik",
            "Merawat hewan peliharaan tidak hanya tentang memberi makan. Simak beberapa tips penting lainnya."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDoctorMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupArticles()
        setupMenuGrid()
        setupBottomNav()
        loadDoctorProfile()
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

        db.collection("doctor_profiles").document(uid).get()
            .addOnSuccessListener { doctorDoc ->
                // Additional doctor data can be loaded here
            }
    }

    // ✅ PERBAIKAN: Ubah ke VERTICAL (sesuai target)
    private fun setupArticles() {
        // Vertical Layout Manager (bukan horizontal)
        binding.rvArticles.layoutManager = LinearLayoutManager(this)
        binding.rvArticles.adapter = ArticleAdapter(sampleArticles)
    }

    private fun setupMenuGrid() {
        binding.menuNotification.setOnClickListener {
            // Open NotificationActivity
        }
        binding.menuSchedule.setOnClickListener {
            binding.bottomNav.selectedItemId = R.id.nav_appointment
        }
        binding.menuArticle.setOnClickListener {
            // Open ArticleListActivity
        }
        binding.menuPetStuff.setOnClickListener {
            // Open Pet Store (optional, bisa di-hide)
        }
        binding.ivNotificationBell.setOnClickListener {
            // Open notifications
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Kembali ke home, tampilkan scroll view
                    binding.scrollView.visibility = View.VISIBLE
                    supportFragmentManager.popBackStack()
                    true
                }
                R.id.nav_appointment -> {
                    // Sembunyikan scroll view, tampilkan fragment appointment
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
}