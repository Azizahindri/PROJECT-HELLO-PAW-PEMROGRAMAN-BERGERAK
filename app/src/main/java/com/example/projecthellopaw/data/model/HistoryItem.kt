package com.example.projecthellopaw.data.model

data class HistoryItem(
    val chatRoomId: String,
    val doctorId: String,
    val doctorName: String,
    val chatStatus: String,
    val lastMessage: String
)