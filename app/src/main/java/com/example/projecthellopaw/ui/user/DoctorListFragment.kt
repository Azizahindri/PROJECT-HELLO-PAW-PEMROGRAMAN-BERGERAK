package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.google.firebase.firestore.FirebaseFirestore

data class DoctorItem(
    val id: String,
    val name: String,
    val specialization: String,
    val fee: Int,
    val rating: Float,
    val experience: Int,
    val bio: String,
    val isOnline: Boolean
)

class DoctorListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var adapter: DoctorAdapter
    private val db = FirebaseFirestore.getInstance()
    private val doctorList = mutableListOf<DoctorItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_list, container, false)

        // ── Bind View ──────────────────────────────────────────────────────────
        recyclerView     = view.findViewById(R.id.rvDoctorList)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = DoctorAdapter(doctorList) { doctor ->
            val intent = Intent(requireContext(), DoctorDetailActivity::class.java)
            intent.putExtra("DOCTOR_ID",           doctor.id)
            intent.putExtra("DOCTOR_NAME",         doctor.name)
            intent.putExtra("DOCTOR_SPECIALIZATION", doctor.specialization)
            intent.putExtra("DOCTOR_FEE",          doctor.fee)
            intent.putExtra("DOCTOR_RATING",       doctor.rating)
            intent.putExtra("DOCTOR_EXPERIENCE",   doctor.experience)
            intent.putExtra("DOCTOR_BIO",          doctor.bio)
            intent.putExtra("DOCTOR_STATUS",       doctor.isOnline)
            startActivity(intent)
        }

        recyclerView.adapter = adapter
        loadDoctors()

        return view
    }

    private fun loadDoctors() {
        db.collection("doctors")
            .get()
            .addOnSuccessListener { result ->
                doctorList.clear()
                for (document in result) {
                    val doctor = DoctorItem(
                        id             = document.id,
                        name           = document.getString("name")           ?: "",
                        specialization = document.getString("specialization") ?: "",
                        fee            = document.getLong("fee")?.toInt()     ?: 50000,
                        rating         = document.getDouble("rating")?.toFloat() ?: 0f,
                        experience     = document.getLong("experience")?.toInt() ?: 0,
                        bio            = document.getString("bio")            ?: "",
                        isOnline       = document.getBoolean("isOnline")      ?: false
                    )
                    doctorList.add(doctor)
                }
                adapter.notifyDataSetChanged()

                // ── Tampilkan empty state jika tidak ada dokter ────────────────
                if (doctorList.isEmpty()) {
                    layoutEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility     = View.GONE
                } else {
                    layoutEmptyState.visibility = View.GONE
                    recyclerView.visibility     = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memuat data dokter", Toast.LENGTH_SHORT).show()
                // Tetap tampilkan empty state saat gagal load
                layoutEmptyState.visibility = View.VISIBLE
                recyclerView.visibility     = View.GONE
            }
    }
}

// ─── Data Class ────────────────────────────────────────────────────────────────
