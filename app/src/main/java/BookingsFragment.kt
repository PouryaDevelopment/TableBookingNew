package com.example.tablebooking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookingsFragment : Fragment(), BookingActionsListener {

    private lateinit var bookingsRecyclerView: RecyclerView
    private lateinit var bookingsAdapter: BookingAdapter
    private val bookingList: MutableList<Booking> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("BookingsFragment", "onViewCreated called")

        bookingsRecyclerView = view.findViewById(R.id.bookingsRecyclerView)
        bookingsRecyclerView.layoutManager = LinearLayoutManager(context)

        bookingsAdapter = BookingAdapter(bookingList, this)
        bookingsRecyclerView.adapter = bookingsAdapter

        fetchBookings()
    }

    private fun fetchBookings() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                bookingList.clear()
                for (document in querySnapshot) {
                    val booking = document.toObject(Booking::class.java).copy(bookingId = document.id)
                    bookingList.add(booking)
                }
                bookingsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("BookingsFragment", "Error fetching bookings: ${e.message}")
            }
    }

    override fun onDeleteBooking(position: Int) {
        val booking = bookingList[position]
        val firestore = FirebaseFirestore.getInstance()
        val restaurantRef = firestore.collection("restaurants").document(booking.restaurantId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(restaurantRef)
            val currentAvailableSeats = snapshot.getLong("availableSeats") ?: 0
            val newAvailableSeats = currentAvailableSeats + booking.numberOfPeople

            transaction.update(restaurantRef, "availableSeats", newAvailableSeats)
            transaction.delete(firestore.collection("bookings").document(booking.bookingId!!))
        }.addOnSuccessListener {
            bookingList.removeAt(position)
            bookingsAdapter.notifyItemRemoved(position)
            Toast.makeText(context, "Booking deleted successfully and seats restored.", Toast.LENGTH_SHORT).show()
            Log.d("BookingsFragment", "Booking deleted successfully and seats restored.")
        }.addOnFailureListener { e ->
            Log.e("BookingsFragment", "Error deleting booking: ${e.message}")
            Toast.makeText(context, "Error deleting booking: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEditBooking(position: Int) {
        val booking = bookingList[position]
        val fragment = EditBookingFragment.newInstance(booking, position)
        fragment.updateListener = object : EditBookingFragment.BookingUpdateListener {
            override fun onBookingUpdated() {
                fetchBookings()
            }
        }
        fragment.show(parentFragmentManager, "edit_booking")
    }
}