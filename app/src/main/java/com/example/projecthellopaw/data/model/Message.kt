package com.example.projecthellopaw.data.model

import java.util.Date

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Date? = null,
    val isAiGenerated: Boolean = false
)