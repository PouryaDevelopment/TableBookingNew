package com.example.tablebooking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

interface BookingActionsListener {
    fun onDeleteBooking(position: Int)
    fun onEditBooking(position: Int)
}

class BookingAdapter(
    private val bookingList: MutableList<Booking>,
    private val listener: BookingActionsListener
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookingList[position]
        holder.bind(booking)
    }

    override fun getItemCount(): Int = bookingList.size

    class BookingViewHolder(view: View, private val listener: BookingActionsListener) : RecyclerView.ViewHolder(view) {
        private val textViewName: TextView = view.findViewById(R.id.textViewName)
        private val textViewDate: TextView = view.findViewById(R.id.textViewDate)
        private val textViewTime: TextView = view.findViewById(R.id.textViewTime)
        private val buttonDelete: TextView = view.findViewById(R.id.buttonDelete)
        private val buttonEdit: TextView = view.findViewById(R.id.buttonEdit)

        init {
            buttonDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onDeleteBooking(position)
                }
            }

            buttonEdit.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEditBooking(position)
                }
            }
        }

        fun bind(booking: Booking) {
            textViewName.text = booking.restaurantName // Use restaurantName instead of restaurantId
            textViewDate.text = booking.date
            textViewTime.text = booking.time
        }
    }
}