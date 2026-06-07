package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
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
    val isOnline: Boolean,
    val avatarUrl: String = "" // Tambahan opsional untuk Glide di adapter
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

    // 🔄 PERBAIKAN LOGIKA: Menggabungkan koleksi 'users' dan 'doctor_profiles'
    private fun loadDoctors() {
        // Langkah 1: Cari user yang memiliki role "dokter"
        db.collection("users")
            .whereEqualTo("role", "DOCTOR")
            .get()
            .addOnSuccessListener { userSnapshots ->
                doctorList.clear()
                val totalDoctors = userSnapshots.size()

                // Jika di Firestore tidak ada user dengan role 'dokter'
                if (totalDoctors == 0) {
                    updateUIState()
                    return@addOnSuccessListener
                }

                var processedCount = 0
                for (userDoc in userSnapshots.documents) {
                    val uid = userDoc.id
                    val name = userDoc.getString("name") ?: "Dokter Hewan"
                    val basePhoto = userDoc.getString("photoUrl") ?: ""

                    // Langkah 2: Cari detail tarif, bio, dll di koleksi 'doctor_profiles' berdasarkan UID
                    db.collection("doctor_profiles").document(uid).get()
                        .addOnSuccessListener { profileDoc ->
                            val spec = profileDoc.getString("specialization") ?: "Dokter Hewan"
                            val bio = profileDoc.getString("bio") ?: "Halo, saya siap membantu berkonsultasi mengenai kesehatan hewan peliharaan Anda."
                            val fee = profileDoc.getLong("consultationFee")?.toInt() ?: 50000
                            val exp = profileDoc.getLong("yearsOfExperience")?.toInt() ?: 0
                            val online = profileDoc.getBoolean("isOnline") ?: false
                            val profilePhoto = profileDoc.getString("photoUrl") ?: ""

                            // Bungkus menjadi satu data DoctorItem utuh
                            val doctor = DoctorItem(
                                id = uid,
                                name = name,
                                specialization = spec,
                                fee = fee,
                                rating = 4.8f, // Hardcode rating default agar tidak 0.0 kosong saat demo
                                experience = exp,
                                bio = bio,
                                isOnline = online,
                                avatarUrl = profilePhoto.ifEmpty { basePhoto }
                            )
                            doctorList.add(doctor)
                            processedCount++

                            // Jika seluruh proses penggabungan data selesai, refresh tampilan
                            if (processedCount == totalDoctors) {
                                updateUIState()
                            }
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == totalDoctors) {
                                updateUIState()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DOCTOR_LIST", "Gagal load data", e)
                Toast.makeText(requireContext(), "Gagal memuat data dokter", Toast.LENGTH_SHORT).show()
                updateUIState()
            }
    }

    // Fungsi bantu untuk memunculkan atau menyembunyikan Empty State secara rapi
    private fun updateUIState() {
        adapter.notifyDataSetChanged()
        if (doctorList.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            recyclerView.visibility     = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            recyclerView.visibility     = View.VISIBLE
        }
    }
}