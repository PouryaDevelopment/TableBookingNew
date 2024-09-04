package com.example.tablebooking

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), RestaurantAdapter.OnRestaurantClickListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var restaurantAdapter: RestaurantAdapter
    private var restaurantList = mutableListOf<Restaurant>()
    private var restaurantListener: ListenerRegistration? = null

    override fun onRestaurantClick(restaurant: Restaurant) {
        // Start BookingActivity when a restaurant is clicked
        val intent = Intent(this, BookingActivity::class.java).apply {
            putExtra("RESTAURANT_NAME", restaurant.name)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set up the drawer layout
        drawerLayout = findViewById(R.id.drawer_layout)
        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                // Sign out and navigate to LoginActivity
                R.id.nav_sign_out -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }

        // Set up the RecyclerView for displaying restaurants
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        restaurantAdapter = RestaurantAdapter(restaurantList, this)
        recyclerView.adapter = restaurantAdapter

        // Set up the bottom navigation view
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_back -> {
                    // Handle back navigation
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        supportFragmentManager.popBackStack()
                    } else {
                        finish()
                    }
                    true
                }
                R.id.nav_bookings -> {
                    // Show the bookings fragment
                    showBookingsFragment()
                    true
                }
                else -> false
            }
        }

        // Load restaurants from the database with real-time updates
        loadRestaurants()
        checkAndReplenishSeats() // Call the function to replenish seats if necessary
    }

    private fun loadRestaurants() {
        // Fetch restaurants from Firestore with real-time updates
        val db = Firebase.firestore
        restaurantListener = db.collection("restaurants")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    e.printStackTrace()
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    restaurantList.clear()
                    for (document in snapshots) {
                        val restaurant = document.toObject<Restaurant>()
                        restaurantList.add(restaurant)
                    }
                    restaurantAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun showBookingsFragment() {
        // Replace the current fragment with BookingsFragment
        val fragment = BookingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onBackPressed() {
        // Handle the back press to pop back stack or finish the activity
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    private fun checkAndReplenishSeats() {
        // Check for outdated bookings and replenish seats if necessary
        val firestore = Firebase.firestore
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        firestore.collection("bookings")
            .whereLessThan("date", currentDate)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    return@addOnSuccessListener
                }
                for (document in querySnapshot) {
                    // Update available seats and delete outdated bookings
                    val booking = document.toObject(Booking::class.java).copy(bookingId = document.id)
                    val restaurantRef = firestore.collection("restaurants").document(booking.restaurantId)
                    firestore.runTransaction { transaction ->
                        val snapshot = transaction.get(restaurantRef)
                        val currentAvailableSeats = snapshot.getLong("availableSeats") ?: 0
                        val newAvailableSeats = currentAvailableSeats + booking.numberOfPeople

                        transaction.update(restaurantRef, "availableSeats", newAvailableSeats)
                        transaction.delete(document.reference)
                    }.addOnSuccessListener {
                    }.addOnFailureListener { e ->
                        e.printStackTrace()
                    }
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detach the listener to prevent memory leaks
        restaurantListener?.remove()
    }

    // Uncaught exception handler
    init {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread, throwable)
        }
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        // Handle the exception and exit if needed
        throwable.printStackTrace()
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(1)
    }
}