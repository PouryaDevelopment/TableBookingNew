// BookingActivity.kt
package com.example.tablebooking

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class BookingActivity : AppCompatActivity() {

    private lateinit var bookingDateEditText: EditText
    private lateinit var bookingTimeEditText: EditText
    private lateinit var numberOfGuestsEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        val restaurantName = intent.getStringExtra("RESTAURANT_NAME") ?: ""
        bookingDateEditText = findViewById(R.id.bookingDate)
        bookingTimeEditText = findViewById(R.id.bookingTime)
        numberOfGuestsEditText = findViewById(R.id.numberOfGuests)
        val confirmBookingButton: Button = findViewById(R.id.confirmBookingButton)

        confirmBookingButton.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val chosenDate = bookingDateEditText.text.toString()
            val chosenTime = bookingTimeEditText.text.toString()
            val numberOfPeople = numberOfGuestsEditText.text.toString().toIntOrNull() ?: 1

            if (validateDate(chosenDate) && validateTime(chosenTime)) {
                fetchRestaurantIdAndConfirmBooking(restaurantName, userId, chosenDate, chosenTime, numberOfPeople)
            }
        }
    }

    private fun validateDate(date: String): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            dateFormat.isLenient = false
            val chosenDate = dateFormat.parse(date)
            val currentDate = Calendar.getInstance().time

            val diff = chosenDate.time - currentDate.time
            val daysDiff = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)

            if (daysDiff in 0..14) {
                true
            } else {
                Toast.makeText(this, "Date must be within two weeks from today", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: ParseException) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun validateTime(time: String): Boolean {
        val timeRegex = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")
        return if (time.matches(timeRegex)) {
            true
        } else {
            Toast.makeText(this, "Invalid time format", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun fetchRestaurantIdAndConfirmBooking(restaurantName: String, userId: String, chosenDate: String, chosenTime: String, numberOfPeople: Int) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("restaurants")
            .whereEqualTo("name", restaurantName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val restaurantDocument = querySnapshot.documents[0]
                    val restaurantId = restaurantDocument.id
                    confirmBooking(userId, restaurantId, restaurantName, chosenDate, chosenTime, numberOfPeople)
                } else {
                    Log.d("BookingActivity", "Restaurant not found with name: $restaurantName")
                    Toast.makeText(this, "Restaurant not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("BookingActivity", "Failed to fetch restaurant ID: ${e.message}")
                Toast.makeText(this, "Failed to fetch restaurant ID: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmBooking(userId: String, restaurantId: String, restaurantName: String, chosenDate: String, chosenTime: String, numberOfPeople: Int) {
        if (userId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val restaurantRef = firestore.collection("restaurants").document(restaurantId)

        Log.d("BookingActivity", "Attempting to fetch restaurant with ID: $restaurantId")

        restaurantRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val currentAvailableSeats = document.getLong("availableSeats") ?: 0
                Log.d("BookingActivity", "Restaurant found: ${document.data}, available seats: $currentAvailableSeats")

                if (currentAvailableSeats >= numberOfPeople) {
                    val booking = Booking(
                        userId = userId,
                        restaurantId = restaurantId,
                        restaurantName = restaurantName,
                        date = chosenDate,
                        time = chosenTime,
                        numberOfPeople = numberOfPeople
                    )

                    val newAvailableSeats = currentAvailableSeats - numberOfPeople

                    firestore.runTransaction { transaction ->
                        transaction.set(firestore.collection("bookings").document(), booking)
                        transaction.update(restaurantRef, "availableSeats", newAvailableSeats)
                    }.addOnSuccessListener {
                        Toast.makeText(this, "Booking confirmed", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Booking failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Not enough available seats", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d("BookingActivity", "Restaurant document does not exist.")
                Toast.makeText(this, "Restaurant not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("BookingActivity", "Failed to fetch restaurant data: ${e.message}")
            Toast.makeText(this, "Failed to fetch restaurant data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}