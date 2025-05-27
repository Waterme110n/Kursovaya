package com.example.kursovaya

import ImageHandler
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CommentsAdapter(
    private val comments: List<CommentItem>,
    private val context: Context,
    private val isAdmin: Boolean,
    private val imageId: String,
    private val loadComments: () -> Unit

) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageButton: ImageButton = itemView.findViewById(R.id.profileImageButton)
        val profileTextButton: Button = itemView.findViewById(R.id.profileTextButton)
        val editTextDesc: TextView = itemView.findViewById(R.id.editTextDesc)
        val deleteCommentButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val commentItem = comments[position]
        holder.editTextDesc.text = commentItem.comment
        holder.profileTextButton.text = commentItem.name
        val bitmap = ImageHandler().decodeBase64ToImages(commentItem.imageProfile)
        holder.profileImageButton.setImageBitmap(bitmap[0])

        holder.deleteCommentButton.visibility = if (isAdmin) View.VISIBLE else View.GONE

        holder.profileTextButton.setOnClickListener {
            openUserProfile(commentItem.userId)
        }

        holder.profileImageButton.setOnClickListener {
            openUserProfile(commentItem.userId)
        }

        holder.deleteCommentButton.setOnClickListener {
            deleteComment(position)
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    private fun openUserProfile(userId: String) {
        val intent = Intent(context, UserProfileActivity::class.java)
        intent.putExtra("USER_ID", userId)
        context.startActivity(intent)
    }

    private fun deleteComment(position: Int) {
        val db = FirebaseFirestore.getInstance()
        val imageDocRef = db.collection("images").document(imageId)

        // Получаем комментарий для удаления
        val commentToDelete = comments[position]
        val commentMap = mapOf("first" to commentToDelete.userId, "second" to commentToDelete.comment)

        // Удаляем комментарий
        imageDocRef.update("comments", FieldValue.arrayRemove(commentMap))
            .addOnSuccessListener {
                Log.d("My log", "Comment successfully deleted!")
                loadComments()
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error deleting comment", e)
            }
    }


}