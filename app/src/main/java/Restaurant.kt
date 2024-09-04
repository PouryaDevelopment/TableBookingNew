package com.example.tablebooking

data class Restaurant(
    val name: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val location: String = "",
    val availableSeats: Long = 0
)