package com.example.kursovaya

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class AdminPanelActivity : AppCompatActivity() {
    private var isAdmin: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_panel)

        val homeButton: ImageButton = findViewById(R.id.ButtonHome)
        val searchButton: ImageButton = findViewById(R.id.ButtonSearch)
        val profileButton: ImageButton = findViewById(R.id.ButtonProfile)

        val tag = intent.getStringExtra("tag")
        if (tag != null) {
            searchButton.setImageResource(R.drawable.search_filled)
            replaceFragment(SearchFragment(), isAdmin, tag)
        } else if (savedInstanceState == null) {
            homeButton.setImageResource(R.drawable.home_filled)
            replaceFragment(ScrollPageFragment(), isAdmin)
        }


        fun resetButtonImages() {
            homeButton.setImageResource(R.drawable.home)
            searchButton.setImageResource(R.drawable.search)
            profileButton.setImageResource(R.drawable.user)
        }

        homeButton.setOnClickListener {
            resetButtonImages()
            homeButton.setImageResource(R.drawable.home_filled)
            replaceFragment(ScrollPageFragment(),isAdmin) }

        searchButton.setOnClickListener {
            resetButtonImages()
            searchButton.setImageResource(R.drawable.search_filled)
            replaceFragment(SearchFragment(),isAdmin) }

        profileButton.setOnClickListener {
            resetButtonImages()
            profileButton.setImageResource(R.drawable.user_filled)
            replaceFragment(ProfileFragment(),isAdmin) }


    }

    private fun replaceFragment(fragment: Fragment, isAdmin: Boolean, tag: String? = null) {
        fragment.arguments = Bundle().apply {
            putBoolean("isAdmin", isAdmin)
            tag?.let { putString("tag", it)}
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


}