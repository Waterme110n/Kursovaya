package com.example.kursovaya

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val homeButton: ImageButton = findViewById(R.id.ButtonHome)
        val searchButton: ImageButton = findViewById(R.id.ButtonSearch)
        val addButton: ImageButton = findViewById(R.id.ButtonAdd)
        val profileButton: ImageButton = findViewById(R.id.ButtonProfile)

        val tag = intent.getStringExtra("tag")
        if (tag != null) {
            searchButton.setImageResource(R.drawable.search_filled)
            replaceFragment(SearchFragment().apply {
                arguments = Bundle().apply {
                    putString("tag", tag)
                }
            })
        }else if (savedInstanceState == null) {
            homeButton.setImageResource(R.drawable.home_filled)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ScrollPageFragment())
                .commit()
        }

        fun resetButtonImages() {
            homeButton.setImageResource(R.drawable.home)
            searchButton.setImageResource(R.drawable.search)
            addButton.setImageResource(R.drawable.add)
            profileButton.setImageResource(R.drawable.user)
        }

        homeButton.setOnClickListener {
            resetButtonImages()
            homeButton.setImageResource(R.drawable.home_filled)
            replaceFragment(ScrollPageFragment()) }

        searchButton.setOnClickListener {
            resetButtonImages()
            searchButton.setImageResource(R.drawable.search_filled)
            replaceFragment(SearchFragment()) }

        addButton.setOnClickListener {
            resetButtonImages()
            addButton.setImageResource(R.drawable.add_filled)
            replaceFragment(AddFragment()) }

        profileButton.setOnClickListener {
            resetButtonImages()
            profileButton.setImageResource(R.drawable.user_filled)
            replaceFragment(ProfileFragment()) }


    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }




}