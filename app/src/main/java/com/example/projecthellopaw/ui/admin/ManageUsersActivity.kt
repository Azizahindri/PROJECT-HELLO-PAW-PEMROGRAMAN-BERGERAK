package com.example.projecthellopaw.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projecthellopaw.R
import com.example.projecthellopaw.adapters.UsersAdapter
import com.example.projecthellopaw.data.model.User
import com.example.projecthellopaw.databinding.ActivityManageUsersBinding

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageUsersBinding
    private lateinit var adapter: UsersAdapter
    private var userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get role dari intent
        val role = intent.getStringExtra("role") ?: "USER"
        val title = if (role == "DOCTOR") "Daftar Dokter" else "Daftar Pengguna"

        // Set title ke toolbar
        binding.toolbarTitle.text = title

        // Setup RecyclerView
        adapter = UsersAdapter(userList) { user ->
            showUserDetail(user)
        }

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        // Load data dummy
        loadData(role)

        // Back button
        binding.ivBack.setOnClickListener {
            finish()
        }

        // Search
        binding.etSearchUser.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadData(role: String) {
        // Data dummy
        val allUsers = listOf(
            User(uid = "1", name = "Andi Pratama", email = "andi@email.com", role = "OWNER"),
            User(uid = "2", name = "Budi Santoso", email = "budi@email.com", role = "OWNER"),
            User(uid = "3", name = "dr. Citra Dewi", email = "citra@klinik.com", role = "DOCTOR"),
            User(uid = "4", name = "dr. Dedi Kurniawan", email = "dedi@klinik.com", role = "DOCTOR"),
            User(uid = "5", name = "Eka Fitriani", email = "eka@email.com", role = "OWNER"),
            User(uid = "6", name = "dr. Fajar Hermawan", email = "fajar@klinik.com", role = "DOCTOR")
        )

        userList.clear()
        if (role == "DOCTOR") {
            userList.addAll(allUsers.filter { it.role == "DOCTOR" })
        } else {
            userList.addAll(allUsers.filter { it.role != "DOCTOR" })
        }
        adapter.updateData(userList)
    }

    private fun filterUsers(query: String) {
        if (query.isEmpty()) {
            adapter.updateData(userList)
        } else {
            val filtered = userList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.email.contains(query, ignoreCase = true)
            }
            adapter.updateData(filtered)
        }
    }

    private fun showUserDetail(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Detail Pengguna")
            .setMessage("""
                Nama: ${user.name}
                Email: ${user.email}
                Role: ${if (user.role == "DOCTOR") "Dokter" else "Pengguna"}
                Username: ${user.username}
                No HP: ${user.phoneNumber}
            """.trimIndent())
            .setPositiveButton("Tutup", null)
            .show()
    }
}