package com.example.projecthellopaw.ui.doctor

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.projecthellopaw.R
import com.example.projecthellopaw.databinding.FragmentAppointmentBinding
import com.example.projecthellopaw.ui.chat.ChatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppointmentItem(
    val chatRoomId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val petName: String = "",
    val petType: String = "",
    val lastMessageTime: Date? = null,
    val chatStatus: String = "active",
    val doctorId: String = ""
)

class AppointmentAdapter(
    private var items: List<AppointmentItem>,
    private val onItemClick: (AppointmentItem) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOwnerName: TextView = itemView.findViewById(R.id.tv_owner_name)
        val tvPetInfo: TextView = itemView.findViewById(R.id.tv_pet_info)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tv_status_badge)
        val cardRoot: CardView = itemView.findViewById(R.id.card_appointment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvOwnerName.text = item.ownerName.ifEmpty { "Pemilik Hewan" }
        holder.tvPetInfo.text = "${item.petName} · ${item.petType}"
        holder.tvTimestamp.text = formatTimestamp(item.lastMessageTime)

        if (item.chatStatus == "active") {
            holder.tvStatusBadge.text = "Sedang Berlangsung"
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_active)
            holder.tvStatusBadge.setTextColor(
                holder.itemView.context.getColor(R.color.badge_active_text)
            )
        } else {
            holder.tvStatusBadge.text = "Menunggu"
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pending)
            holder.tvStatusBadge.setTextColor(
                holder.itemView.context.getColor(R.color.badge_pending_text)
            )
        }

        holder.cardRoot.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<AppointmentItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun formatTimestamp(date: Date?): String {
        if (date == null) return ""
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance().apply { time = date }
        return when {
            isSameDay(now, cal) -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            isYesterday(now, cal) -> "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
            else -> SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(date)
        }
    }

    private fun isSameDay(c1: Calendar, c2: Calendar) =
        c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

    private fun isYesterday(now: Calendar, cal: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, cal)
    }
}

class AppointmentFragment : Fragment() {

    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerReg: ListenerRegistration? = null
    private lateinit var adapter: AppointmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        listenToAppointments()
    }

    private fun setupRecyclerView() {
        adapter = AppointmentAdapter(emptyList()) { item ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("CHAT_ROOM_ID", item.chatRoomId)
                putExtra("OWNER_ID", item.ownerId)
                putExtra("OWNER_NAME", item.ownerName)
                putExtra("PET_NAME", item.petName)
                putExtra("DOCTOR_ID", item.doctorId)
            }
            startActivity(intent)
        }
        binding.rvAppointment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppointment.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            listenToAppointments()
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun listenToAppointments() {
        val doctorId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        listenerReg?.remove()
        listenerReg = db.collection("chat_rooms")
            .whereEqualTo("doctorId", doctorId)
            .whereEqualTo("paymentStatus", "success")
            .addSnapshotListener { snapshots, error ->
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (error != null || snapshots == null) {
                    showEmpty()
                    return@addSnapshotListener
                }

                if (snapshots.isEmpty) {
                    showEmpty()
                    return@addSnapshotListener
                }

                val items = snapshots.documents.map { doc ->
                    AppointmentItem(
                        chatRoomId = doc.id,
                        ownerId = doc.getString("ownerId") ?: "",
                        ownerName = doc.getString("ownerName") ?: "",
                        petName = doc.getString("petName") ?: "",
                        petType = doc.getString("petType") ?: "",
                        lastMessageTime = doc.getDate("lastMessageTime"),
                        chatStatus = doc.getString("chatStatus") ?: "active",
                        doctorId = doctorId
                    )
                }

                binding.tvEmpty.visibility = View.GONE
                binding.rvAppointment.visibility = View.VISIBLE
                adapter.updateItems(items)
            }
    }

    private fun showEmpty() {
        binding.rvAppointment.visibility = View.GONE
        binding.tvEmpty.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _binding = null
    }
}