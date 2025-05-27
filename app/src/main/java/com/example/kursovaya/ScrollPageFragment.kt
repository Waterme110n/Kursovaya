package com.example.kursovaya

import ImageHandler
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ScrollPageFragment : Fragment(R.layout.fragment_scroll_page) {

    private lateinit var auth: FirebaseAuth
    private lateinit var imageHandler: ImageHandler
    private var isCreatedButtonActive = false
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            isAdmin = it.getBoolean("isAdmin", false)
        }

        auth = FirebaseAuth.getInstance()
        imageHandler = ImageHandler()
        loadImages()
    }

    override fun onResume() {
        super.onResume()
        loadImages()
    }

    private fun loadImages() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Получаем теги пользователя
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDocument ->
                val userTags = userDocument.get("tags") as? List<Map<String, Any>> ?: emptyList()

                // Создаем карту тегов и их приоритетов
                val tagPriorities: Map<String, Int> = userTags.associate { tagMap ->
                    val tag = tagMap["tag"] as? String ?: "" // Получаем тег
                    val priority =
                        (tagMap["priority"] as? Number)?.toInt() ?: 0 // Получаем приоритет
                    tag to priority
                }

                // Получаем изображения
                db.collection("images").get()
                    .addOnSuccessListener { querySnapshot ->
                        val imageList = mutableListOf<Pair<String, Pair<String, List<String>>>>()

                        for (document in querySnapshot.documents) {
                            val base64Image = document.getString("base64Image") ?: continue
                            val tags = document.get("tags") as? List<String>
                                ?: emptyList() // Получаем теги из документа
                            imageList.add(Pair(document.id, Pair(base64Image, tags)))
                        }

                        // Сортируем изображения по тегам и приоритетам
                        val sortedImageList = imageList.sortedWith { a, b ->
                            val aTags = a.second.second // Теги для изображения a
                            val bTags = b.second.second // Теги для изображения b

                            val aPriority = aTags.mapNotNull { tagPriorities[it] }.maxOrNull() ?: 0
                            val bPriority = bTags.mapNotNull { tagPriorities[it] }.maxOrNull() ?: 0

                            bPriority.compareTo(aPriority) // Сортируем по убыванию приоритета
                        }

                        val leftColumn = view?.findViewById<LinearLayout>(R.id.left_column)
                        val rightColumn = view?.findViewById<LinearLayout>(R.id.right_column)

                        leftColumn?.let { left ->
                            rightColumn?.let { right ->
                                val adapter = ImageMainAdapter(
                                    sortedImageList.map { Pair(it.first, it.second.first) },
                                    left,
                                    right,
                                    isCreatedButtonActive,
                                    isAdmin
                                )
                                adapter.bind()
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("My log", "Error loading images", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error loading user tags", e)
            }
    }
}