package com.example.projecthellopaw.data.model

import com.google.firebase.Timestamp

data class Review(
    val id: String = "",
    val chatRoomId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val ownerId: String = "",
    val userId: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val petName: String = "",
    val duration: Int = 0,
    val timestamp: Timestamp? = null
)