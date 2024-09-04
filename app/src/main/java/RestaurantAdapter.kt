package com.example.tablebooking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso



class RestaurantAdapter(
    private val restaurantList: List<Restaurant>,
    private val listener: OnRestaurantClickListener
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        val currentItem = restaurantList[position]
        Picasso.get().load(currentItem.imageUrl).into(holder.imageView)
        holder.textViewName.text = currentItem.name
        holder.textViewLocation.text = currentItem.location
        holder.textViewDescription.text = currentItem.description
        holder.textViewAvailableSeats.text = "Available Seats: ${currentItem.availableSeats}"
        holder.itemView.setOnClickListener {
            listener.onRestaurantClick(currentItem)
        }
    }

    override fun getItemCount(): Int {
        return restaurantList.size
    }

    class RestaurantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val textViewName: TextView = view.findViewById(R.id.textViewName)
        val textViewLocation: TextView = view.findViewById(R.id.textViewLocation)
        val textViewDescription: TextView = view.findViewById(R.id.textViewDescription)
        val textViewAvailableSeats: TextView = view.findViewById(R.id.textViewAvailableSeats)
    }

    interface OnRestaurantClickListener {
        fun onRestaurantClick(restaurant: Restaurant)
    }
}

