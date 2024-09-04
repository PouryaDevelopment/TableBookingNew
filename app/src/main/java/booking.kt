package com.example.tablebooking
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Booking(
    val bookingId: String? = null,
    val userId: String = "",
    val restaurantId: String = "",
    val restaurantName: String = "",
    val date: String = "",
    val time: String = "",
    val numberOfPeople: Int = 0,
) : Parcelable