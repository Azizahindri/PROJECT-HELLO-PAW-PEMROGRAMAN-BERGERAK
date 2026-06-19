package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.google.firebase.firestore.FirebaseFirestore

class DoctorListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var ivClear: ImageView
    private lateinit var adapter: DoctorAdapter
    private val db = FirebaseFirestore.getInstance()
    private val doctorList = mutableListOf<DoctorItem>()
    private val filteredList = mutableListOf<DoctorItem>()

    companion object {
        private const val TAG = "DoctorListFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_doctor_list, container, false)

        recyclerView = view.findViewById(R.id.rvDoctorList)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)
        etSearch = view.findViewById(R.id.etSearchDoctor)
        ivClear = view.findViewById(R.id.ivClearSearchDoctor)

        setupRecyclerView()
        loadDoctors()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isEmpty()) {
                    ivClear.visibility = View.GONE
                    filterDoctors("")
                } else {
                    ivClear.visibility = View.VISIBLE
                    filterDoctors(query)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        ivClear.setOnClickListener {
            etSearch.setText("")
            ivClear.visibility = View.GONE
            filterDoctors("")
        }

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = DoctorAdapter(filteredList) { doctor ->
            val intent = Intent(requireContext(), DoctorDetailActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctor.id)
            intent.putExtra("DOCTOR_NAME", doctor.name)
            intent.putExtra("DOCTOR_SPECIALIZATION", doctor.specialization)
            intent.putExtra("DOCTOR_FEE", doctor.fee)
            intent.putExtra("DOCTOR_RATING", doctor.rating)
            intent.putExtra("DOCTOR_EXPERIENCE", doctor.experience)
            intent.putExtra("DOCTOR_BIO", doctor.bio)
            intent.putExtra("DOCTOR_STATUS", doctor.isOnline)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun loadDoctors() {
        Log.d(TAG, "=== LOAD DOCTORS START ===")

        db.collection("users")
            .whereEqualTo("role", "DOCTOR")
            .get()
            .addOnCompleteListener { userTask ->
                if (!userTask.isSuccessful) {
                    Log.e(TAG, "Failed to load users: ${userTask.exception?.message}")
                    Toast.makeText(requireContext(), "Gagal memuat data dokter", Toast.LENGTH_SHORT).show()
                    updateUIState()
                    return@addOnCompleteListener
                }

                val userSnapshots = userTask.result
                Log.d(TAG, "Total users with role DOCTOR: ${userSnapshots?.size() ?: 0}")

                doctorList.clear()
                if (userSnapshots == null || userSnapshots.isEmpty) {
                    Log.d(TAG, "No doctors found!")
                    updateUIState()
                    return@addOnCompleteListener
                }

                var processedCount = 0
                val totalDoctors = userSnapshots.size()

                for (userDoc in userSnapshots.documents) {
                    val uid = userDoc.id
                    val name = userDoc.getString("name") ?: "Dokter Hewan"
                    val basePhoto = userDoc.getString("photoUrl") ?: ""

                    Log.d(TAG, "Processing doctor: $name ($uid)")

                    db.collection("doctor_profiles").document(uid).get()
                        .addOnCompleteListener { profileTask ->
                            if (!profileTask.isSuccessful) {
                                Log.e(TAG, "Failed to load profile for $uid", profileTask.exception)
                                processedCount++
                                if (processedCount == totalDoctors) {
                                    updateUIState()
                                }
                                return@addOnCompleteListener
                            }

                            val profileDoc = profileTask.result
                            if (profileDoc == null || !profileDoc.exists()) {
                                Log.e(TAG, "Profile not found for $uid")
                                processedCount++
                                if (processedCount == totalDoctors) {
                                    updateUIState()
                                }
                                return@addOnCompleteListener
                            }

                            val spec = profileDoc.getString("specialization") ?: "Dokter Hewan"
                            val bio = profileDoc.getString("bio") ?: "Halo, saya siap membantu."
                            val fee = profileDoc.getLong("consultationFee")?.toInt() ?: 50000
                            val exp = profileDoc.getLong("yearsOfExperience")?.toInt() ?: 0
                            val online = profileDoc.getBoolean("isOnline") ?: false
                            val profilePhoto = profileDoc.getString("photoUrl") ?: ""

                            Log.d(TAG, "Doctor $name isOnline: $online")

                            db.collection("reviews")
                                .whereEqualTo("doctorId", uid)
                                .get()
                                .addOnCompleteListener { reviewTask ->
                                    var totalRating = 0.0
                                    var count = 0

                                    if (reviewTask.isSuccessful) {
                                        val reviewDocs = reviewTask.result
                                        if (reviewDocs != null) {
                                            for (doc in reviewDocs) {
                                                totalRating += doc.getDouble("rating") ?: 0.0
                                                count++
                                            }
                                        }
                                    }

                                    val avgRating = if (count > 0) totalRating / count else 0.0

                                    val doctor = DoctorItem(
                                        id = uid,
                                        name = name,
                                        specialization = spec,
                                        fee = fee,
                                        rating = avgRating.toFloat(),
                                        experience = exp,
                                        bio = bio,
                                        isOnline = online,
                                        avatarUrl = profilePhoto.ifEmpty { basePhoto },
                                        totalReviews = count
                                    )
                                    doctorList.add(doctor)
                                    processedCount++

                                    if (processedCount == totalDoctors) {
                                        doctorList.sortByDescending { it.rating }
                                        filteredList.clear()
                                        filteredList.addAll(doctorList)
                                        updateUIState()
                                    }
                                }
                        }
                }
            }
    }

    private fun filterDoctors(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(doctorList)
        } else {
            val lowerQuery = query.lowercase()
            val filtered = doctorList.filter { doctor ->
                doctor.name.lowercase().contains(lowerQuery) ||
                        doctor.specialization.lowercase().contains(lowerQuery)
            }
            filteredList.addAll(filtered)
        }
        updateUIState()
    }

    private fun updateUIState() {
        adapter.notifyDataSetChanged()
        if (filteredList.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}