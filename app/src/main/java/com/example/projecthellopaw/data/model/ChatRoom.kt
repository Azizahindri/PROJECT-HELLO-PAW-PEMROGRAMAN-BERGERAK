package com.example.projecthellopaw.data.model

import com.google.firebase.Timestamp

data class ChatRoom(
    val chatRoomId: String = "",
    val ownerId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val ownerName: String = "",
    val petName: String = "",
    val paymentStatus: String = "PENDING",
    val chatStatus: String = "active",
    val createdAt: Timestamp? = null,
    val endedAt: Timestamp? = null,
    val duration: Long = 0,
    val hasReview: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null
)