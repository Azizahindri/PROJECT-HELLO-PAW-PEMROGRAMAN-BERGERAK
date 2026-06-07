package com.example.projecthellopaw.model

data class Doctor(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val specialization: String = "Dokter Hewan",
    val clinicName: String = "",
    val consultationFee: Long = 50000L,
    val isOnline: Boolean = false,
    val avatarUrl: String = ""
)