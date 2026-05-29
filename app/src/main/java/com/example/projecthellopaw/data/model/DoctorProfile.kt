package com.example.projecthellopaw.data.model

data class DoctorProfile(
    val doctorId: String = "",
    val specialization: String = "",
    val strNumber: String = "",
    val price: Long = 0,
    val isOnline: Boolean = false
)