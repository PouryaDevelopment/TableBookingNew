package com.example.tablebooking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EditBookingFragment : DialogFragment() {

    private lateinit var booking: Booking
    private var bookingPosition: Int = -1
    var updateListener: BookingUpdateListener? = null

    companion object {
        private const val ARG_BOOKING = "arg_booking"
        private const val ARG_POSITION = "arg_position"

        // Factory method to create a new instance of EditBookingFragment with the provided booking and position
        fun newInstance(booking: Booking, position: Int): EditBookingFragment {
            val args = Bundle().apply {
                putParcelable(ARG_BOOKING, booking)
                putInt(ARG_POSITION, position)
            }
            return EditBookingFragment().apply { arguments = args }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            booking = it.getParcelable(ARG_BOOKING) ?: throw IllegalArgumentException("Booking cannot be null")
            bookingPosition = it.getInt(ARG_POSITION)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_booking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Populate the EditText fields with the current booking details
        view.findViewById<EditText>(R.id.editTextDate).setText(booking.date)
        view.findViewById<EditText>(R.id.editTextTime).setText(booking.time)
        view.findViewById<EditText>(R.id.editTextNumberOfPeople).setText(booking.numberOfPeople.toString())
        view.findViewById<Button>(R.id.buttonSave).setOnClickListener { saveEditedBooking() }
    }

    private fun saveEditedBooking() {
        val dateEditText = view?.findViewById<EditText>(R.id.editTextDate)
        val timeEditText = view?.findViewById<EditText>(R.id.editTextTime)
        val numberOfPeopleEditText = view?.findViewById<EditText>(R.id.editTextNumberOfPeople)

        // Get the updated values from the EditText fields
        val date = dateEditText?.text.toString()
        val time = timeEditText?.text.toString()
        val numberOfPeople = numberOfPeopleEditText?.text.toString().toIntOrNull()

        // Ensure all fields are filled
        if (date.isEmpty() || time.isEmpty() || numberOfPeople == null) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        // Validate the time format
        if (!validateTime(time)) {
            Toast.makeText(context, "Invalid time format", Toast.LENGTH_SHORT).show()
            return
        }
        // Create a map with the updated booking details
        val updatedBookingMap: Map<String, Any> = hashMapOf(
            "date" to date,
            "time" to time,
            "numberOfPeople" to numberOfPeople
        )

        val db = Firebase.firestore
        booking.bookingId?.let { bookingId ->
            // Update the booking details in Firestore
            db.collection("bookings").document(bookingId).get().addOnSuccessListener { document ->
                val originalBooking = document.toObject(Booking::class.java)
                if (originalBooking != null) {
                    val restaurantRef = db.collection("restaurants").document(originalBooking.restaurantId)
                    db.runTransaction { transaction ->
                        val snapshot = transaction.get(restaurantRef)
                        val currentAvailableSeats = snapshot.getLong("availableSeats") ?: 0
                        val newAvailableSeats = currentAvailableSeats + originalBooking.numberOfPeople - numberOfPeople
                        // Update the available seats and the booking details
                        transaction.update(restaurantRef, "availableSeats", newAvailableSeats)
                        transaction.update(document.reference, updatedBookingMap)
                    }.addOnSuccessListener {
                        dismiss()
                        // Close the dialog
                        updateListener?.onBookingUpdated()
                    }.addOnFailureListener { e ->
                        // Handle errors
                        Log.e("EditBookingFragment", "Error updating booking and seats: ${e.message}")
                    }
                } else {
                    Toast.makeText(context, "Failed to fetch original booking data", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Log.e("EditBookingFragment", "Error fetching original booking data: ${e.message}")
            }
        } ?: Log.w("EditBookingFragment", "Booking ID is null")
    }
    private fun validateTime(time: String): Boolean {
        // Validate the time format (HH:mm)
        val timeRegex = Regex("^([01]\\d|2[0-3]):([0-5]\\d)$")
        return time.matches(timeRegex)
    }
    interface BookingUpdateListener {
        // Listener interface to notify about booking updates
        fun onBookingUpdated()
    }

}