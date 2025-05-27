package com.example.kursovaya

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {
    private lateinit var tagManager: TagManager
    private lateinit var auth: FirebaseAuth
    private var isCreatedButtonActive = false
    private lateinit var searchEditText: EditText
    private lateinit var searchImageButton: ImageButton
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isAdmin = it.getBoolean("isAdmin", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_page, container, false)

        searchEditText = view.findViewById(R.id.SearchEditText)
        searchImageButton = view.findViewById(R.id.SearchImageButton)

        tagManager = TagManager()
        auth = FirebaseAuth.getInstance()

        val tag = arguments?.getString("tag")
        if (tag != null) {
            searchEditText.setText(tag)
            val tagsList = listOf(tag)
            tagManager.updateTagPriorities("search", tagsList)
            search(tag)
        }


        searchImageButton.setOnClickListener {
            val query = searchEditText.text.toString()

            if (query.startsWith("#")) {
                val tagsList = listOf(query)
                tagManager.updateTagPriorities("search", tagsList)
            }

            search(query)
        }

        return view
    }

    private fun search(query: String) {
        val db = FirebaseFirestore.getInstance()
        val keywords = query.split(" ")

        db.collection("images")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val results = mutableListOf<Pair<String, String>>()

                for (document in querySnapshot.documents) {
                    val base64Image = document.getString("base64Image") ?: continue
                    val title = document.getString("title") ?: ""
                    val tags = document.get("tags") as? List<String> ?: emptyList()

                    val titleMatches = keywords.any { keyword ->
                        title.contains(keyword, ignoreCase = true)
                    }
                    val tagsMatch = tags.any { tag ->
                        keywords.any { keyword ->
                            tag.contains(keyword, ignoreCase = true)
                        }
                    }

                    if (titleMatches || tagsMatch) {
                        results.add(Pair(document.id, base64Image))
                    }
                }

                val leftColumn = view?.findViewById<LinearLayout>(R.id.left_column)
                val rightColumn = view?.findViewById<LinearLayout>(R.id.right_column)

                if (leftColumn != null && rightColumn != null) {
                    val adapter = ImageMainAdapter(results, leftColumn, rightColumn, isCreatedButtonActive, isAdmin)
                    adapter.bind()
                } else {
                    Log.w("My log", "Left or right column is null")
                }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error searching images", e)
            }
    }
}