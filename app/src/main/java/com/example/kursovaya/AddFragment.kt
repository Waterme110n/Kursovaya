package com.example.kursovaya

import ImageHandler
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class AddFragment : Fragment() {
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var imageButton: ImageButton
    private lateinit var titleEditText: EditText
    private lateinit var descEditText: EditText
    private lateinit var tagsEditText: EditText
    private var imagePath: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        val createButton: Button = view.findViewById(R.id.ButtonCreate)
        val clearButton: Button = view.findViewById(R.id.ButtonClear)
        imageButton = view.findViewById(R.id.imageButton)
        titleEditText = view.findViewById(R.id.editTextName)
        descEditText = view.findViewById(R.id.editTextDesc)
        tagsEditText = view.findViewById(R.id.editTextTags)


        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        createButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val desc = descEditText.text.toString()
            val tagsString = tagsEditText.text.toString()

            val tags = tagsString.split(", ", " ")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.startsWith("#") }

            if (imagePath == null) {
                Toast.makeText(requireContext(), "Пожалуйста, выберите изображение", Toast.LENGTH_SHORT).show()
            } else if (title.isBlank()) {
                Toast.makeText(requireContext(), "Введите заголовок", Toast.LENGTH_SHORT).show()
            } else if (tags.isEmpty()) {
                Toast.makeText(requireContext(), "Добавьте хотя бы один тег", Toast.LENGTH_SHORT).show()
            } else {
                ImageHandler().uploadImageToFirestore(imagePath!!, tags, title, desc)
                Toast.makeText(requireContext(), "Ура, вы создали картинку", Toast.LENGTH_LONG).show()
                clearFields()
            }
        }

        clearButton.setOnClickListener {
            clearFields()
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                imagePath = bitmap
                imageButton.setImageBitmap(bitmap)
            }
        }
    }

    private fun clearFields() {
        titleEditText.text.clear()
        descEditText.text.clear()
        tagsEditText.text.clear()
        imageButton.setImageResource(R.drawable.add_filled_dark)
        imagePath = null
    }
}