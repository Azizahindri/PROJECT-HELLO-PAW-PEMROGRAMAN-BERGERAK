package com.example.projecthellopaw.data.model

import com.google.firebase.Timestamp

data class ChatRoom(
    val chatRoomId: String = "",
    val ownerId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val ownerName: String = "",
    val petName: String = "",
    val paymentStatus: String = "PENDING", // PENDING, SUCCESS
    val chatStatus: String = "active", // active, completed, cancelled
    val createdAt: Timestamp? = null, // Waktu mulai sesi
    val endedAt: Timestamp? = null, // Waktu selesai sesi
    val duration: Long = 0, // Durasi dalam menit
    val hasReview: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null
)