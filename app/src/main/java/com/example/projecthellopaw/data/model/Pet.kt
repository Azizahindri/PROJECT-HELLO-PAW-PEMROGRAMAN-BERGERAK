package com.example.projecthellopaw.data.model

data class Pet(
    val petId: String = "",
    val ownerId: String = "",
    val petName: String = "",
    val petType: String = "", // Kucing, Anjing, dll
    val petAge: Int = 0
)