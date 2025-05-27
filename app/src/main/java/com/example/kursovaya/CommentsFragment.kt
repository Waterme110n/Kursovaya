package com.example.kursovaya

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class CommentItem(
    val userId: String,
    val comment: String,
    val name: String,
    val imageProfile: String
)

class CommentsFragment : BottomSheetDialogFragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var editTextNewComment: EditText
    private lateinit var nextButton: ImageButton
    private lateinit var imageId: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CommentsAdapter
    private val commentsList = mutableListOf<CommentItem>()
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imageId = arguments?.getString("imageId") ?: ""

        arguments?.let {
            isAdmin = it.getBoolean("isAdmin", false)

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        auth = FirebaseAuth.getInstance()


        editTextNewComment = view.findViewById(R.id.EditTextNewComment)
        nextButton = view.findViewById(R.id.NextWindow)
        recyclerView = view.findViewById(R.id.recyclerViewComments)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = CommentsAdapter(commentsList,requireContext(),isAdmin,imageId){
            loadComments()
        }
        recyclerView.adapter = adapter

        loadComments()

        nextButton.setOnClickListener {
            if (editTextNewComment.text.toString().isNotEmpty()){
                addCommentToImage(editTextNewComment.text.toString())
                editTextNewComment.text.clear()
            } else{
                Toast.makeText(requireContext(), "Комментарий не может быть пустой", Toast.LENGTH_SHORT).show()
            }


        }

    }

    private fun addCommentToImage(comment: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val comentatorId = currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val commentPair = Pair(comentatorId, comment)

        db.collection("images").document(imageId)
            .update("comments", FieldValue.arrayUnion(commentPair))
            .addOnSuccessListener {
                Log.d("Firestore", "Comment added successfully")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding comment", e)
            }
        loadComments()
    }

    private fun loadComments() {
        val db = FirebaseFirestore.getInstance()

        db.collection("images").document(imageId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val commentsData = document.get("comments") as? List<Map<String, String>> ?: emptyList()
                    commentsList.clear()

                    val userIds = commentsData.mapNotNull { it["first"] }.distinct().filter { it.isNotEmpty() }

                    val userRequests = userIds.map { userId ->
                        db.collection("users").document(userId).get().continueWith { userDocument ->
                            if (userDocument.isSuccessful && userDocument.result != null) {
                                val userDoc = userDocument.result
                                val name = userDoc.getString("name") ?: ""
                                val imageProfile = userDoc.getString("ImageProfile") ?: ""

                                commentsData.forEach { commentMap ->
                                    if (commentMap["first"] == userId) {
                                        val comment = commentMap["second"] ?: ""
                                        commentsList.add(CommentItem(userId, comment, name, imageProfile))
                                    }
                                }
                            }
                        }
                    }

                    Tasks.whenAllComplete(userRequests).addOnCompleteListener {
                        adapter.notifyDataSetChanged()
                    }

                } else {
                    Log.d("My log", "No document found")
                }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error fetching image document", e)
            }
    }


}