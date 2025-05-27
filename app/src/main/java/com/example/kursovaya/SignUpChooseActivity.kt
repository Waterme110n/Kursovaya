package com.example.kursovaya

import ImageHandler
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SignUpChooseActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var imageHandler: ImageHandler
    private lateinit var buttonNext: Button
    private var selectedCount: Int = 0
        get() = selectedTags.size
    private lateinit var auth: FirebaseAuth
    private val selectedTags = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_chooce)
        enableEdgeToEdge()
        auth = FirebaseAuth.getInstance()

        recyclerView = findViewById(R.id.recyclerView)
        buttonNext = findViewById(R.id.ButtonNext)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val items = listOf(
            SignUpItem(R.drawable.sign_up_fst, "jacket"),
            SignUpItem(R.drawable.sign_up_sec, "pixel"),
            SignUpItem(R.drawable.sign_up_trd, "nature"),
            SignUpItem(R.drawable.sign_up_fourth, "b&w"),
            SignUpItem(R.drawable.sign_up_fifth, "cars"),
            SignUpItem(R.drawable.sign_up_sixth, "japan"),
            SignUpItem(R.drawable.sign_up_sevth, "anime"),
            SignUpItem(R.drawable.sign_up_eigth, "minecraft"),
            SignUpItem(R.drawable.sign_up_ninth, "film"),
            SignUpItem(R.drawable.sign_up_tenth, "cartoon")

        )

        val adapter = ImageSignUpAdapter(items) { tag, isChecked ->
            if (isChecked) {
                selectedTags.add(tag)
            } else {
                selectedTags.remove(tag)
            }
            println(selectedTags)
            updateButtonState()
        }

        recyclerView.adapter = adapter

        imageHandler = ImageHandler()

        buttonNext.setOnClickListener{
            startActivity(Intent(this, MainActivity::class.java))
            val taggedTags = selectedTags.map { "#$it" }
            saveUserTag(taggedTags)
        }
    }


    private fun updateButtonState() {
        when(selectedCount){
            0 ->{
                buttonNext.text = "Choose at least 3 categories"
                buttonNext.isEnabled = false
            }
            1 ->{
                buttonNext.text = "Choose 2 more"
                buttonNext.isEnabled = false
            }
            2 ->{
                buttonNext.text = "Choose 1 more"
                buttonNext.isEnabled = false
            }
            else -> {
                buttonNext.text = "next"
                buttonNext.isEnabled = true
            }

        }
    }

    private fun saveUserTag(tags: List<String>) {
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val userTags = tags.map { UserTag(it, 5) }

        val userData = hashMapOf("tags" to userTags)

        db.collection("users").document(userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("My log", "User tags saved successfully")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error saving user name", e)
            }
    }

}