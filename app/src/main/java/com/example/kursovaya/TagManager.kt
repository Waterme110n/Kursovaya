package com.example.kursovaya

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class UserTag(
    val tag: String,
    val priority: Int
)

class TagManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()


    fun updateTagPriorities(actionType: String, imageTags: List<String> = emptyList()) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userTags = document.get("tags") as? List<Map<String, Any>> ?: emptyList()

                    val updatedTags = userTags.toMutableList()
                    imageTags.forEach { tag ->
                        if (updatedTags.none { it["tag"] == tag }) {
                            updatedTags.add(hashMapOf("tag" to tag, "priority" to 5))
                        }
                    }

                    val finalTags = updatedTags.map { tagMap ->
                        val tag = tagMap["tag"] as String
                        var priority = (tagMap["priority"] as? Number)?.toInt() ?: 0


                        if (imageTags.contains(tag)) {
                            when (actionType) {
                                "search" -> priority += 3
                                "view" -> priority += 2
                                "like" -> priority += 3
                            }
                        }

                        priority = (priority - 1).coerceAtLeast(0)


                        priority = priority.coerceAtMost(10)

                        hashMapOf("tag" to tag, "priority" to priority)
                    }

                    val filteredTags = finalTags.filter { (it["priority"] as? Int ?: 0) > 0}

                    db.collection("users").document(userId)
                        .set(hashMapOf("tags" to filteredTags), SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("My log", "Tag priorities updated successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.w("My log", "Error updating tag priorities", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("My log", "Error fetching user tags", e)
            }
    }
}