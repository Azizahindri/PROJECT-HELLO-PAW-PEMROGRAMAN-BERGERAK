package com.example.projecthellopaw.data.model

data class ChatRoom(
    val chatRoomId: String = "",
    val ownerId: String = "",
    val doctorId: String = "",
    val paymentStatus: String = "PENDING" // PENDING, SUCCESS
)