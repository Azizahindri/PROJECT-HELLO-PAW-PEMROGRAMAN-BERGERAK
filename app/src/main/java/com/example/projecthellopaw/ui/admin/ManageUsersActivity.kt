package com.example.projecthellopaw.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecthellopaw.R
import com.example.projecthellopaw.adapters.UsersAdapter
import com.example.projecthellopaw.data.model.User
import com.example.projecthellopaw.databinding.ActivityManageUsersBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private lateinit var adapter: UsersAdapter
    private var userList = mutableListOf<User>()
    private var filteredList = mutableListOf<User>()

    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    companion object {
        private const val TAG = "ManageUsersActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val role = intent.getStringExtra("role") ?: "USER"
        val title = if (role == "DOCTOR") "Daftar Dokter" else "Daftar Pengguna"

        binding.toolbarTitle.text = title

        setupRecyclerView()
        loadDataFromFirestore(role)

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.etSearchUser.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = UsersAdapter(filteredList) { user ->
            showUserDetail(user)
        }
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun loadDataFromFirestore(role: String) {
        listener?.remove()

        listener = db.collection("users")
            .whereEqualTo("role", if (role == "DOCTOR") "DOCTOR" else "OWNER")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error loading users: ${error.message}", error)
                    Toast.makeText(this, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.d(TAG, "No users found")
                    userList.clear()
                    filteredList.clear()
                    adapter.updateData(filteredList)
                    return@addSnapshotListener
                }

                userList.clear()

                for (document in snapshots.documents) {
                    try {
                        val uid = document.id
                        val name = document.getString("name") ?: "Pengguna"
                        val email = document.getString("email") ?: ""
                        val userRole = document.getString("role") ?: "OWNER"
                        val username = document.getString("username") ?: ""
                        val phoneNumber = document.getString("phoneNumber") ?: ""

                        val user = User(
                            uid = uid,
                            name = name,
                            email = email,
                            role = userRole,
                            username = username,
                            phoneNumber = phoneNumber
                        )
                        userList.add(user)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user: ${e.message}", e)
                    }
                }

                filteredList.clear()
                filteredList.addAll(userList)
                adapter.updateData(filteredList)

                Log.d(TAG, "Users loaded: ${filteredList.size}")
            }
    }

    private fun filterUsers(query: String) {
        filteredList.clear()

        if (query.isEmpty()) {
            filteredList.addAll(userList)
        } else {
            val lowerQuery = query.lowercase()
            filteredList.addAll(
                userList.filter {
                    it.name.lowercase().contains(lowerQuery) ||
                            it.email.lowercase().contains(lowerQuery) ||
                            it.username.lowercase().contains(lowerQuery)
                }
            )
        }

        adapter.updateData(filteredList)
    }

    private fun showUserDetail(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Detail Pengguna")
            .setMessage("""
                Nama: ${user.name}
                Email: ${user.email}
                Role: ${if (user.role == "DOCTOR") "👨‍⚕️ Dokter" else "👤 Pengguna"}
                Username: ${user.username}
                No HP: ${user.phoneNumber}
            """.trimIndent())
            .setPositiveButton("Tutup", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
        listener = null
    }
}