package com.example.kursovaya

import ImageHandler
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {
    private var USER_ID: String? = null

    private lateinit var auth: FirebaseAuth
    private var userName: String? = null
    private var userDesc: String? = null
    private var userImage: Bitmap? = null
    private lateinit var nameText: TextView
    private lateinit var descText: TextView
    private lateinit var imageProfile: ImageView
    private var isCreatedButtonActive = false
    private lateinit var deleteButton: ImageButton
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        USER_ID = intent.getStringExtra("USER_ID")
        isAdmin = intent.getBooleanExtra("isAdmin", false)


        auth = FirebaseAuth.getInstance()
        loadUserData()
        loadCreatedImages()
        nameText = findViewById(R.id.UsernameText)
        descText = findViewById(R.id.DescriptionText)
        imageProfile = findViewById(R.id.imageView)
        val buttonClose: ImageButton = findViewById(R.id.ButtonClose)

        buttonClose.setOnClickListener {
            finish()
        }

    }

    private fun loadUserData() {
        val db = FirebaseFirestore.getInstance()


        // Проверяем, что пользователь аутентифицирован
        if (USER_ID != null) {

            db.collection("users").document(USER_ID!!).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        userName = document.getString("name")
                        userDesc = document.getString("description")
                        val image = document.getString("ImageProfile")

                        if (image != null) {
                            val base64Images = ImageHandler().decodeBase64ToImages(image)
                            if (base64Images.isNotEmpty()) {
                                userImage = base64Images[0]
                                imageProfile.setImageBitmap(userImage)
                            }
                        }
                        nameText.text = userName
                        descText.text = userDesc

                    } else {
                        Log.d("User Data", "No such document")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("User Data", "Error getting user data", e)
                }
        } else {
            Log.w("User Data", "User is not authenticated")
        }
    }

    private fun loadCreatedImages() {
        val db = FirebaseFirestore.getInstance()

        db.collection("images")
            .whereEqualTo("creatorId", USER_ID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val imageList = mutableListOf<Pair<String, String>>()

                for (document in querySnapshot.documents) {
                    val base64Image = document.getString("base64Image") ?: continue
                    imageList.add(Pair(document.id, base64Image))
                }

                val leftColumn = findViewById<LinearLayout>(R.id.left_column)
                val rightColumn = findViewById<LinearLayout>(R.id.right_column)

                val adapter = ImageMainAdapter(imageList, leftColumn, rightColumn, isCreatedButtonActive, isAdmin)
                adapter.bind()
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error loading images", e)
            }
    }

}