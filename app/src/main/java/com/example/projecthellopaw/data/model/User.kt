package com.example.projecthellopaw.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "" // Isinya nanti: "OWNER" atau "DOCTOR"
)