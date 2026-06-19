data class DoctorItem(
    val id: String,
    val name: String,
    val specialization: String,
    val fee: Int,
    val rating: Float,
    val experience: Int,
    val bio: String,
    val isOnline: Boolean,
    val avatarUrl: String = "",
    val totalReviews: Int = 0  // ← TAMBAHKAN
)