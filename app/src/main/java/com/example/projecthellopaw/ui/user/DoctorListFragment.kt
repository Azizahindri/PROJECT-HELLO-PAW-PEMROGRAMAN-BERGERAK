package com.example.projecthellopaw.ui.user

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.*

class DoctorListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var ivClear: ImageView
    private lateinit var adapter: DoctorAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val db = FirebaseFirestore.getInstance()
    private val doctorList = mutableListOf<DoctorItem>()
    private val filteredList = mutableListOf<DoctorItem>()

    // Lokasi user saat ini (null = belum dapat lokasi)
    private var userLat: Double? = null
    private var userLng: Double? = null

    // Mode tampilan: "rating" atau "nearby"
    private var sortMode = "rating"

    companion object {
        private const val TAG = "DoctorListFragment"
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            getUserLocation()
        } else {
            Toast.makeText(context, "Izin lokasi ditolak. Menampilkan semua dokter berdasarkan rating.", Toast.LENGTH_SHORT).show()
        }
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupRecyclerView()
        setupSortButtons(view)
        loadDoctors()
        requestLocationPermission()

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

    private fun setupSortButtons(view: View) {
        val btnSortRating = view.findViewById<TextView>(R.id.btnSortRating)
        val btnSortNearby = view.findViewById<TextView>(R.id.btnSortNearby)

        if (btnSortRating == null || btnSortNearby == null) return

        btnSortRating.setOnClickListener {
            sortMode = "rating"
            btnSortRating.isSelected = true
            btnSortNearby.isSelected = false
            applySortAndFilter()
        }

        btnSortNearby.setOnClickListener {
            if (userLat == null) {
                Toast.makeText(context, "Sedang mengambil lokasi...", Toast.LENGTH_SHORT).show()
                requestLocationPermission()
                return@setOnClickListener
            }
            sortMode = "nearby"
            btnSortRating.isSelected = false
            btnSortNearby.isSelected = true
            applySortAndFilter()
        }
    }

    private fun requestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            getUserLocation()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
                        Log.d(TAG, "User location: $userLat, $userLng")
                        // Kalau list sudah ter-load, langsung hitung jarak
                        if (doctorList.isNotEmpty()) {
                            applySortAndFilter()
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed to get location", it)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }

    // Haversine formula untuk hitung jarak (km) antara dua koordinat
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Radius bumi dalam km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun applySortAndFilter() {
        val query = etSearch.text.toString().trim().lowercase()

        var sorted = if (sortMode == "nearby" && userLat != null && userLng != null) {
            // Urutkan berdasarkan jarak, dokter tanpa lokasi taruh paling akhir
            doctorList.sortedWith(compareBy { doctor ->
                val dLat = doctor.latitude
                val dLng = doctor.longitude
                if (dLat != 0.0 && dLng != 0.0) {
                    haversineDistance(userLat!!, userLng!!, dLat, dLng)
                } else {
                    Double.MAX_VALUE
                }
            })
        } else {
            doctorList.sortedByDescending { it.rating }
        }

        if (query.isNotEmpty()) {
            sorted = sorted.filter { doctor ->
                doctor.name.lowercase().contains(query) ||
                        doctor.specialization.lowercase().contains(query)
            }
        }

        filteredList.clear()
        filteredList.addAll(sorted)
        updateUIState()
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
                    updateUIState()
                    return@addOnCompleteListener
                }

                var processedCount = 0
                val totalDoctors = userSnapshots.size()

                for (userDoc in userSnapshots.documents) {
                    val uid = userDoc.id
                    val name = userDoc.getString("name") ?: "Dokter Hewan"
                    val basePhoto = userDoc.getString("photoUrl") ?: ""

                    db.collection("doctor_profiles").document(uid).get()
                        .addOnCompleteListener { profileTask ->
                            if (!profileTask.isSuccessful || profileTask.result == null || !profileTask.result!!.exists()) {
                                processedCount++
                                if (processedCount == totalDoctors) updateUIState()
                                return@addOnCompleteListener
                            }

                            val profileDoc = profileTask.result!!
                            val spec = profileDoc.getString("specialization") ?: "Dokter Hewan"
                            val bio = profileDoc.getString("bio") ?: "Halo, saya siap membantu."
                            val fee = profileDoc.getLong("consultationFee")?.toInt() ?: 50000
                            val exp = profileDoc.getLong("yearsOfExperience")?.toInt() ?: 0
                            val online = profileDoc.getBoolean("isOnline") ?: false
                            val profilePhoto = profileDoc.getString("photoUrl") ?: ""
                            val docLat = profileDoc.getDouble("latitude") ?: 0.0
                            val docLng = profileDoc.getDouble("longitude") ?: 0.0

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
                                        totalReviews = count,
                                        latitude = docLat,
                                        longitude = docLng
                                    )
                                    doctorList.add(doctor)
                                    processedCount++

                                    if (processedCount == totalDoctors) {
                                        applySortAndFilter()
                                    }
                                }
                        }
                }
            }
    }

    private fun filterDoctors(query: String) {
        applySortAndFilter()
    }

    private fun updateUIState() {
        if (!isAdded) return
        requireActivity().runOnUiThread {
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
}