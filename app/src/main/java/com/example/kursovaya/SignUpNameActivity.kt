package com.example.kursovaya

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SignUpNameActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up_name)

        auth = FirebaseAuth.getInstance()
        val editTextName: EditText = findViewById(R.id.editTextName)
        val nextButton: ImageButton = findViewById(R.id.NextWindow)

        nextButton.setOnClickListener {
            val name = editTextName.text.toString().trim()
            if (name.isBlank()) {
                Log.w("My log", "Name cannot be empty")
                return@setOnClickListener
            }
            Log.d("My log", "Saving name: $name")
            saveUserName(name)
        }
    }

    private fun saveUserName(name: String) {
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val userData = hashMapOf("name" to name)

        db.collection("users").document(userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("My log", "User name saved successfully")
                startActivity(Intent(this, SignUpChooseActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error saving user name", e)
            }
    }
}