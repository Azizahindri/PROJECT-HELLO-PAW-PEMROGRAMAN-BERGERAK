package com.example.projecthellopaw.model

data class Article(
    val title: String = "",
    val description: String = "",
    val thumbnail: String = "",
    val url: String = "" // ◄── Untuk menyimpan link artikel asli
)