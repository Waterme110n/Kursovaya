package com.example.kursovaya

import ImageHandler
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class ProfileFragment : Fragment() {
    private lateinit var auth: FirebaseAuth

    private lateinit var nameText: TextView
    private lateinit var descText: TextView
    private lateinit var imageProfile: ImageView
    private lateinit var imageButton: ImageButton
    private var userName: String? = null
    private var userDesc: String? = null
    private var userImage: Bitmap? = null
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var createdButton: Button
    private lateinit var likedButton: Button
    private lateinit var cardViewCreated: CardView
    private lateinit var cardViewLiked: CardView
    private var isCreatedButtonActive = true
    private var isLikedButtonActive = false
    private var isAdmin = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        nameText = view.findViewById(R.id.UsernameText)
        descText = view.findViewById(R.id.DescriptionText)
        imageProfile = view.findViewById(R.id.imageView)

        createdButton = view.findViewById(R.id.ButtonCreated)
        likedButton = view.findViewById(R.id.ButtonLiked)
        cardViewCreated = view.findViewById(R.id.CardViewCreated)
        cardViewLiked = view.findViewById(R.id.CardViewLiked)

        auth = FirebaseAuth.getInstance()
        loadUserData()

        arguments?.let {
            isAdmin = it.getBoolean("isAdmin", false)
        }
        createdButton.visibility = if (isAdmin) View.GONE else View.VISIBLE
        likedButton.visibility = if (isAdmin)  View.GONE else View.VISIBLE
        cardViewCreated.visibility = if (isAdmin) View.GONE else View.VISIBLE
        cardViewLiked.visibility = if (isAdmin)  View.GONE else View.VISIBLE

        loadCreatedImages()

        createdButton.setOnClickListener {
            handleButtonClick(true)
            loadCreatedImages()
        }

        likedButton.setOnClickListener {
            handleButtonClick(false)
            loadLikedImages()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (isCreatedButtonActive) {
            loadCreatedImages()
        } else {
            loadLikedImages()
        }
    }

    private fun loadUserData() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            db.collection("users").document(userId).get()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_profile, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_out -> {
                auth.signOut()
                startActivity(Intent(requireContext(), SignUpActivity::class.java))
                true
            }
            R.id.action_edit -> {
                showEditDialog(userName, userDesc, userImage)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showEditDialog(userName: String?, userDesc: String?, userImage: Bitmap?) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit, null)
        builder.setView(dialogView)

        val closeDialog: ImageButton = dialogView.findViewById(R.id.ButtonClose)
        val saveProfile: Button = dialogView.findViewById(R.id.ButtonSave)
        imageButton = dialogView.findViewById(R.id.imageButton)
        val editTextName = dialogView.findViewById<EditText>(R.id.editTextName)
        val editTextDesc = dialogView.findViewById<EditText>(R.id.editTextDesc)

        editTextName.setText(userName)
        editTextDesc.setText(userDesc)

        userImage?.let {
            imageButton.setImageBitmap(it)
        }

        val dialog = builder.create()
        dialog.show()

        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        closeDialog.setOnClickListener {
            loadUserData()
            dialog.dismiss()
        }

        saveProfile.setOnClickListener {
            val name = editTextName.text.toString()
            val description = editTextDesc.text.toString()
            val imageBase64 = ImageHandler().encodeImageToBase64((imageButton.drawable as BitmapDrawable).bitmap)

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Введите имя", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (imageBase64 == null) {
                Toast.makeText(requireContext(), "Введите имя", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveUserData(name, description, imageBase64)
            dialog.dismiss()
            loadUserData()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                userImage = bitmap
                imageButton.setImageBitmap(bitmap)
            }
        }
    }

    private fun saveUserData(name: String, description: String, imageBase64: String) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid

            val userData = hashMapOf<String, Any>(
                "name" to name,
                "description" to description,
                "ImageProfile" to imageBase64
            )

            db.collection("users").document(userId)
                .update(userData)
                .addOnSuccessListener {
                    Log.d("User Data", "User data successfully updated!")
                }
                .addOnFailureListener { e ->
                    Log.w("User Data", "Error updating user data", e)
                }
        } else {
            Log.w("User Data", "User is not authenticated")
        }
    }

    private fun loadCreatedImages() {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val creatorId = currentUser?.uid ?: return

        db.collection("images")
            .whereEqualTo("creatorId", creatorId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val imageList = mutableListOf<Pair<String, String>>()

                for (document in querySnapshot.documents) {
                    val base64Image = document.getString("base64Image") ?: continue
                    imageList.add(Pair(document.id, base64Image))
                }

                val leftColumn = view?.findViewById<LinearLayout>(R.id.left_column)
                val rightColumn = view?.findViewById<LinearLayout>(R.id.right_column)

                if (leftColumn != null && rightColumn != null) {
                    val adapter = ImageMainAdapter(imageList, leftColumn, rightColumn, isCreatedButtonActive, false)
                    adapter.bind()
                } else {
                    Log.w("My log", "Left or right column is null")
                }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error loading images", e)
            }
    }

    private fun loadLikedImages() {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("images").get()
            .addOnSuccessListener { querySnapshot ->
                val imageList = mutableListOf<Pair<String, String>>()

                for (document in querySnapshot.documents) {
                    val base64Image = document.getString("base64Image") ?: continue
                    val likes = document.get("like") as? List<String> ?: continue

                    if (likes.contains(currentUserId)) {
                        imageList.add(Pair(document.id, base64Image))
                    }
                }

                val leftColumn = view?.findViewById<LinearLayout>(R.id.left_column)
                val rightColumn = view?.findViewById<LinearLayout>(R.id.right_column)

                if (leftColumn != null && rightColumn != null) {
                    val adapter = ImageMainAdapter(imageList, leftColumn, rightColumn, isCreatedButtonActive, false)
                    adapter.bind()
                } else {
                    Log.w("My log", "Left or right column is null")
                }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error loading images", e)
            }
    }

    private fun handleButtonClick(isCreated: Boolean) {
        val backFromResources = ContextCompat.getColor(requireContext(), R.color.back)
        val labelsFromResources = ContextCompat.getColor(requireContext(), R.color.labels)
        if (isCreated) {
            if (isCreatedButtonActive) return // Если уже активен, ничего не делаем

            cardViewCreated.setCardBackgroundColor(labelsFromResources)
            createdButton.setTextColor(backFromResources)

            // Деактивируем кнопку "Liked"
            if (isLikedButtonActive) {
                cardViewLiked.setCardBackgroundColor(backFromResources)
                likedButton.setTextColor(labelsFromResources)
                isLikedButtonActive = false
            }

            isCreatedButtonActive = true
        } else {
            if (isLikedButtonActive) return

            cardViewLiked.setCardBackgroundColor(labelsFromResources)
            likedButton.setTextColor(backFromResources)

            if (isCreatedButtonActive) {
                cardViewCreated.setCardBackgroundColor(backFromResources)
                createdButton.setTextColor(labelsFromResources)
                isCreatedButtonActive = false
            }

            isLikedButtonActive = true
        }
    }
}