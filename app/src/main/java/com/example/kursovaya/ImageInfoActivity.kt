package com.example.kursovaya

import ImageData
import ImageHandler
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ImageInfoActivity : AppCompatActivity() {
    private lateinit var tagManager: TagManager
    private lateinit var auth: FirebaseAuth
    private lateinit var imageData: ImageData
    private lateinit var likeButton: ImageButton
    private lateinit var dislikeButton: ImageButton
    private lateinit var likeCount: TextView
    private lateinit var imageId: String
    private lateinit var downloadButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private var isAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_info)



        imageId = intent.getStringExtra("imageId")!!
        val buttonClose: ImageButton = findViewById(R.id.ButtonClose)
        val commentButton: ImageButton =  findViewById(R.id.CommentButton)

        likeButton = findViewById(R.id.likeButton)
        dislikeButton = findViewById(R.id.dislikeButton)
        likeCount = findViewById(R.id.likeCount)
        downloadButton = findViewById(R.id.downloadButton)

        tagManager = TagManager()
        loadImageDetails(imageId)
        auth = FirebaseAuth.getInstance()


        isAdmin = intent.getBooleanExtra("isAdmin", false)
        deleteButton = findViewById(R.id.deleteButton)

        if (!isAdmin) {
            deleteButton.visibility = View.GONE
        } else {
            deleteButton.setOnClickListener {
                deleteImage(imageId)
            }
        }

        buttonClose.setOnClickListener {
            finish()
        }

        likeButton.setOnClickListener {
            addUserIdToLikeList(imageId)

        }
        dislikeButton.setOnClickListener {
            addUserIdToDislikeList(imageId)
        }
        commentButton.setOnClickListener{
            showCommentsFragment()
        }

        downloadButton.setOnClickListener {
            imageData.let { data ->
                ImageHandler().downloadImage(data.base64Image, this)
            }
        }

    }


    private fun showCommentsFragment() {
        val bottomSheet = CommentsFragment().apply {
            arguments = Bundle().apply {
                putString("imageId", imageId)
                putBoolean("isAdmin",isAdmin)
            }
        }
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    }

    private fun addUserIdToLikeList(imageId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val comentatorId = currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()


        db.collection("images").document(imageId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val likes = document.get("like") as? MutableList<String> ?: mutableListOf()
                    val dislikes = document.get("dislike") as? MutableList<String> ?: mutableListOf()

                    if (likes.contains(comentatorId)) {
                        likes.remove(comentatorId)
                        db.collection("images").document(imageId)
                            .update("like", FieldValue.arrayRemove(comentatorId))
                            .addOnSuccessListener {
                                Log.d("My log", "User $comentatorId removed from like list.")
                                imageData.like = likes
                                updateLikeButtonIcon(imageData)
                            }
                            .addOnFailureListener { e ->
                                Log.w("My log", "Error removing user from like list", e)
                            }

                    } else {
                        likes.add(comentatorId)
                        db.collection("images").document(imageId)
                            .update("like", FieldValue.arrayUnion(comentatorId))
                            .addOnSuccessListener {
                                Log.d("My log", "User $comentatorId added to like list.")
                                imageData.like = likes
                                tagManager.updateTagPriorities("like",imageData.tags)
                                updateLikeButtonIcon(imageData)
                            }
                            .addOnFailureListener { e ->
                                Log.w("My log", "Error adding user to like list", e)
                            }
                        if(dislikes.contains(comentatorId)){
                            dislikes.remove(comentatorId)
                            db.collection("images").document(imageId)
                                .update("dislike", FieldValue.arrayRemove(comentatorId))
                                .addOnSuccessListener {
                                    Log.d("My log", "User $comentatorId removed from dislike list.")
                                    imageData.dislike = dislikes
                                    updateDislikeButtonIcon(imageData)
                                }
                                .addOnFailureListener { e ->
                                    Log.w("My log", "Error removing user from dislike list", e)
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error fetching image document", e)
            }
    }

    private fun addUserIdToDislikeList(imageId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val comentatorId = currentUser?.uid ?: return // Выход, если пользователь не авторизован
        val db = FirebaseFirestore.getInstance()

        // Сначала получаем текущие данные изображения
        db.collection("images").document(imageId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val dislikes = document.get("dislike") as? MutableList<String> ?: mutableListOf()
                    val likes = document.get("like") as? MutableList<String> ?: mutableListOf()

                    if (dislikes.contains(comentatorId)) {
                        dislikes.remove(comentatorId)
                        db.collection("images").document(imageId)
                            .update("dislike", FieldValue.arrayRemove(comentatorId))
                            .addOnSuccessListener {
                                Log.d("My log", "User $comentatorId removed from dislike list.")
                                imageData.dislike = dislikes // Обновляем local imageData
                                updateDislikeButtonIcon(imageData) // Обновляем иконку после удаления
                            }
                            .addOnFailureListener { e ->
                                Log.w("My log", "Error removing user from dislike list", e)
                            }
                    } else {
                        dislikes.add(comentatorId)
                        db.collection("images").document(imageId)
                            .update("dislike", FieldValue.arrayUnion(comentatorId))
                            .addOnSuccessListener {
                                Log.d("My log", "User $comentatorId added to dislike list.")
                                imageData.dislike = dislikes
                                updateDislikeButtonIcon(imageData)
                            }
                            .addOnFailureListener { e ->
                                Log.w("My log", "Error adding user to dislike list", e)
                            }
                        if(likes.contains(comentatorId)){
                            likes.remove(comentatorId)
                            db.collection("images").document(imageId)
                                .update("like", FieldValue.arrayRemove(comentatorId))
                                .addOnSuccessListener {
                                    Log.d("My log", "User $comentatorId removed from dislike list.")
                                    imageData.like = likes // Обновляем local imageData
                                    updateLikeButtonIcon(imageData) // Обновляем иконку после удаления
                                }
                                .addOnFailureListener { e ->
                                    Log.w("My log", "Error removing user from dislike list", e)
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error fetching image document", e)
            }
    }

    private fun loadImageDetails(imageId: String?) {
        val db = FirebaseFirestore.getInstance()

        imageId?.let {
            db.collection("images").document(it).get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Извлечение данных из документа
                        val base64Image = document.getString("base64Image") ?: ""
                        val title = document.getString("title") ?: ""
                        val description = document.getString("description") ?: ""
                        val creatorId = document.getString("creatorId") ?: ""
                        val like = document.get("like") as? List<String> ?: emptyList()
                        val dislike = document.get("dislike") as? List<String> ?: emptyList()

                        // Извлечение списка тегов
                        val tags = document.get("tags") as? List<String> ?: emptyList()
                        tagManager.updateTagPriorities("view",tags)
                        // Извлечение списка комментариев
                        val comments = document.get("comments") as? List<Map<String, String>> ?: emptyList()
                        val commentPairs = comments.map { Pair(it["userId"] ?: "", it["comment"] ?: "") }

                        // Создание экземпляра ImageData
                        imageData = ImageData(
                            base64Image = base64Image,
                            tags = tags,
                            title = title,
                            description = description,
                            creatorId = creatorId,
                            like = like,
                            dislike = dislike,
                            comments = commentPairs
                        )

                        displayImageDetails(imageData)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("My log", "Error loading image details", e)
                }
        }
    }

    private fun displayImageDetails(imageData: ImageData) {

        val imageButton = findViewById<ImageButton>(R.id.pictureButton)
        val bitmaps = ImageHandler().decodeBase64ToImages(imageData.base64Image)
        if (bitmaps.isNotEmpty()) {
            imageButton.setImageBitmap(bitmaps[0])
        }

        findViewById<TextView>(R.id.namePicture).text = imageData.title
        findViewById<TextView>(R.id.editTextDesc).text = imageData.description

        displayTags(imageData.tags)

        val profileButton = findViewById<Button>(R.id.profileTextButton)
        val author = loadCreatorName(imageData.creatorId)
        profileButton.text = author.toString()

        profileButton.setOnClickListener{
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("USER_ID", imageData.creatorId)
            intent.putExtra("isAdmin", isAdmin)
            startActivity(intent)
        }

        updateLikeButtonIcon(imageData)
        updateDislikeButtonIcon(imageData)
    }

    private fun updateLikeButtonIcon(imageData: ImageData) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val creatorId = currentUser?.uid ?: ""

        if (creatorId != null && imageData.like.contains(creatorId)) {
            likeButton.setImageResource(R.drawable.like_filled)
        } else {
            likeButton.setImageResource(R.drawable.like)
        }

        val likeCountValue = imageData.like.size
        if (likeCountValue == 0){
            likeCount.text = ""
        } else{
            likeCount.text = likeCountValue.toString()
        }
    }

    private fun updateDislikeButtonIcon(imageData: ImageData) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val creatorId = currentUser?.uid ?: ""

        if (creatorId != null && imageData.dislike.contains(creatorId)) {
            dislikeButton.setImageResource(R.drawable.dislike_filled)
        } else {
            dislikeButton.setImageResource(R.drawable.dislike)
        }

        val likeCountValue = imageData.like.size
        if (likeCountValue == 0){
            likeCount.text = ""
        } else{
            likeCount.text = likeCountValue.toString()
        }
    }

    private fun loadCreatorName(creatorId: String) {
        if (creatorId.isEmpty()) {
            Log.w("My log", "User ID is empty")
            return
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(creatorId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val creatorName = document.getString("name")
                    val imageProfileBase64 = document.getString("ImageProfile")

                    val profileButton = findViewById<Button>(R.id.profileTextButton)
                    profileButton.text = creatorName ?: "Неизвестный пользователь"

                    val profileImageButton = findViewById<ImageButton>(R.id.profileImageButton)
                    if (!imageProfileBase64.isNullOrEmpty()) {
                        val bitmaps = ImageHandler().decodeBase64ToImages(imageProfileBase64)
                        if (bitmaps.isNotEmpty()) {
                            profileImageButton.setImageBitmap(bitmaps[0])
                            profileImageButton.setOnClickListener{
                                val intent = Intent(this, UserProfileActivity::class.java)
                                intent.putExtra("USER_ID", imageData.creatorId)
                                intent.putExtra("isAdmin", isAdmin)
                                startActivity(intent)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error loading creator name", e)
            }
    }

    private fun displayTags(tags: List<String>) {
        val tagsContainer = findViewById<FlexboxLayout>(R.id.tagsContainer)
        tagsContainer.removeAllViews()

        for (tag in tags) {
            val tagView = layoutInflater.inflate(R.layout.item_tag, tagsContainer, false)
            val button = tagView.findViewById<Button>(R.id.profileTextButton)
            button.text = tag

            button.setOnClickListener {
                val intent: Intent
                if (isAdmin) {
                    intent = Intent(this, AdminPanelActivity::class.java)
                } else {
                    intent = Intent(this, MainActivity::class.java)
                }
                // Передаем тег в выбранную активность
                intent.putExtra("tag", tag)
                startActivity(intent)
            }

            tagsContainer.addView(tagView)
        }
    }

    private fun deleteImage(imageId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("images").document(imageId)
            .delete()
            .addOnSuccessListener {
                Log.d("My log", "Image successfully deleted!")
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error deleting image", e)
            }
    }

}