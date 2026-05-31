package com.example.projecthellopaw.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecthellopaw.data.model.UserAdminModel
import com.example.projecthellopaw.databinding.ActivityManageUsersBinding
import com.google.firebase.firestore.FirebaseFirestore

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private lateinit var db: FirebaseFirestore
    private val usersList = ArrayList<UserAdminModel>()
    private lateinit var usersAdapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        // 1. Ambil data penanda role yang dikirim dari AdminMainActivity
        val roleType = intent.getStringExtra("ROLE_TYPE") ?: "OWNER"

        // 2. Set Judul Halaman di XML secara dinamis biar mirip mockup Figma
        if (roleType == "DOCTOR") {
            binding.tvListTitle.text = "Dokter"
        } else {
            binding.tvListTitle.text = "Pengguna"
        }

        // 3. Setup RecyclerView & Adapter-nya
        setupRecyclerView(roleType)

        // 4. Tarik data dari Firestore sesuai role
        loadUsersFromFirestore(roleType)
    }

    private fun setupRecyclerView(roleType: String) {
        binding.rvUsersList.layoutManager = LinearLayoutManager(this)
        usersAdapter = UsersAdapter(usersList, roleType)
        binding.rvUsersList.adapter = usersAdapter
    }

    private fun loadUsersFromFirestore(role: String) {
        usersList.clear() // Bersihkan list agar tidak duplikat saat reload

        db.collection("users")
            .whereEqualTo("role", role)
            .get()
            .addOnSuccessListener { snapshots ->
                if (!snapshots.isEmpty) {
                    for (document in snapshots) {
                        val user = UserAdminModel(
                            id = document.id,
                            name = document.getString("name") ?: "Tanpa Nama",
                            email = document.getString("email") ?: "",
                            role = document.getString("role") ?: ""
                        )
                        usersList.add(user)
                    }
                    // Kasih tahu adapter kalau data dari Firebase sudah masuk semua
                    usersAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Tidak ada data $role yang terdaftar", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}