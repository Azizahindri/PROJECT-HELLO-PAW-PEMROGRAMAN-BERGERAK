package com.example.projecthellopaw.model

import com.google.firebase.Timestamp

data class Article(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,  // ← PAKAI nullable
    val category: String = "",
    val createdAt: Timestamp? = null
)