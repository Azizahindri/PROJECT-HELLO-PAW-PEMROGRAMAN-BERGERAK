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
import com.bumptech.glide.Glide
import com.example.projecthellopaw.R
import com.example.projecthellopaw.ui.chat.ChatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.*

class DoctorListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var ivClear: ImageView
    private lateinit var adapter: DoctorAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val doctorList = mutableListOf<DoctorItem>()
    private val filteredList = mutableListOf<DoctorItem>()

    private var userLat: Double? = null
    private var userLng: Double? = null

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

        btnSortRating?.setOnClickListener {
            sortMode = "rating"
            btnSortRating.isSelected = true
            btnSortNearby?.isSelected = false
            applySortAndFilter()
        }

        btnSortNearby?.setOnClickListener {
            if (userLat == null) {
                Toast.makeText(context, "Sedang mengambil lokasi...", Toast.LENGTH_SHORT).show()
                requestLocationPermission()
                return@setOnClickListener
            }
            sortMode = "nearby"
            btnSortRating?.isSelected = false
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

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
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
            checkIfAlreadyHaveActiveChat(doctor.id) { hasActiveChat, chatRoomId ->
                if (hasActiveChat && chatRoomId != null) {
                    val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                        putExtra("CHAT_ROOM_ID", chatRoomId)
                        putExtra("DOCTOR_ID", doctor.id)
                        putExtra("DOCTOR_NAME", doctor.name)
                        putExtra("OWNER_ID", auth.currentUser?.uid ?: "")
                        putExtra("PET_NAME", "Anabul")
                    }
                    startActivity(intent)
                } else {
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
            }
        }
        recyclerView.adapter = adapter
    }

    private fun checkIfAlreadyHaveActiveChat(
        doctorId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            callback(false, null)
            return
        }

        db.collection("chat_rooms")
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("doctorId", doctorId)
            .whereIn("chatStatus", listOf("active", "waiting", "pending"))
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val chatRoomId = documents.documents.first().id
                    callback(true, chatRoomId)
                } else {
                    callback(false, null)
                }
            }
            .addOnFailureListener {
                callback(false, null)
            }
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
data class DoctorItem(
    val id: String = "",
    val name: String = "",
    val specialization: String = "",
    val fee: Int = 0,
    val rating: Float = 0f,
    val experience: Int = 0,
    val bio: String = "",
    val isOnline: Boolean = false,
    val avatarUrl: String = "",
    val totalReviews: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

class DoctorAdapter(
    private val doctors: List<DoctorItem>,
    private val onItemClick: (DoctorItem) -> Unit
) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: CircleImageView = itemView.findViewById(R.id.ivDoctorAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvDoctorName)
        val tvSpecialization: TextView = itemView.findViewById(R.id.tvDoctorSpecialization)
        val tvRating: TextView = itemView.findViewById(R.id.tvDoctorRating)
        val tvExperience: TextView = itemView.findViewById(R.id.tvDoctorExperience)
        val tvFee: TextView = itemView.findViewById(R.id.tvDoctorFee)
        val tvStatus: TextView = itemView.findViewById(R.id.tvDoctorStatus)
        val btnConsult: ImageView = itemView.findViewById(R.id.btnConsultDoctor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_doctor, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = doctors[position]

        holder.tvName.text = doctor.name
        holder.tvSpecialization.text = doctor.specialization
        holder.tvFee.text = String.format(java.util.Locale("id", "ID"), "Rp %,d", doctor.fee)
        holder.tvRating.text = if (doctor.rating > 0) {
            String.format(java.util.Locale("id", "ID"), "⭐ %.1f", doctor.rating)
        } else {
            "⭐ -"
        }
        holder.tvExperience.text = String.format(java.util.Locale("id", "ID"), "%d tahun", doctor.experience)
        holder.tvStatus.text = if (doctor.isOnline) "🟢 Online" else "🔴 Offline"
        holder.tvStatus.setTextColor(
            if (doctor.isOnline) android.graphics.Color.parseColor("#2E7D32")
            else android.graphics.Color.parseColor("#C62828")
        )

        if (doctor.avatarUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(doctor.avatarUrl)
                .placeholder(R.drawable.ic_doctor_placeholder)
                .circleCrop()
                .into(holder.ivAvatar)
        }

        holder.itemView.setOnClickListener {
            onItemClick(doctor)
        }

        holder.btnConsult.setOnClickListener {
            onItemClick(doctor)
        }
    }

    override fun getItemCount(): Int = doctors.size
}