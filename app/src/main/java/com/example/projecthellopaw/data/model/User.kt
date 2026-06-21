package com.example.projecthellopaw.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val username: String = "",
    val profileImage: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val birthDate: String = "",
    val gender: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)