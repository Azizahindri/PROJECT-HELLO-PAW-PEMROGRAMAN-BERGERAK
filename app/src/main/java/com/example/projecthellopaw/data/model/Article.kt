package com.example.projecthellopaw.model

data class Article(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val url: String = "",
    val doctorId: String = "",
    val createdAt: Long = 0L,
    val category: String = ""
)