package com.example.kursovaya

import ImageData
import ImageHandler
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ImageEditActivity : AppCompatActivity() {
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var auth: FirebaseAuth
    private lateinit var imageData: ImageData
    private lateinit var imageId: String
    private var imagePath: Bitmap? = null
    private lateinit var pictureButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_image_edit)

        imageId = intent.getStringExtra("imageId")!!
        val buttonClose: ImageButton = findViewById(R.id.ButtonClose)
        val buttonEdit: Button = findViewById(R.id.ButtonEdit)
        val buttonDelete: Button = findViewById(R.id.ButtonDelete)
        pictureButton = findViewById(R.id.pictureButton)

        loadImageDetails(imageId)
        auth = FirebaseAuth.getInstance()

        buttonClose.setOnClickListener {
            finish()
        }
        buttonEdit.setOnClickListener {
            updateImageData()

        }
        buttonDelete.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Deletion confirmation")
            builder.setMessage(" Are you sure you want to delete this image?")
            builder.setPositiveButton("Да") { dialog, which ->
                deleteImageData()
            }
            builder.setNegativeButton("Нет") { dialog, which ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
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
        findViewById<EditText>(R.id.editTextName).setText(imageData.title)
        findViewById<EditText>(R.id.editTextDesc).setText(imageData.description)

    }

    private fun updateImageData() {
        val db = FirebaseFirestore.getInstance()
        val updatedTitle = findViewById<EditText>(R.id.editTextName).text.toString()
        val updatedDescription = findViewById<EditText>(R.id.editTextDesc).text.toString()


        if (updatedTitle.isBlank()) {
            Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData: MutableMap<String, Any> = hashMapOf(
            "title" to updatedTitle,
            "description" to updatedDescription,
        )

        imageId.let {
            db.collection("images").document(it)
                .update(updatedData)
                .addOnSuccessListener {
                    Log.d("My log", "Image data updated successfully")
                    Toast.makeText(this, "Обновление успешно", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w("My log", "Error updating image data", e)
                    Toast.makeText(this, "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteImageData() {
        val db = FirebaseFirestore.getInstance()

        imageId.let {
            db.collection("images").document(it)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Изображение успешно удалено", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.w("My log", "Error deleting image data", e)
                    Toast.makeText(this, "Ошибка удаления изображения", Toast.LENGTH_SHORT).show()
                }
        }
    }
}