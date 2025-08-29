package prototype.one.mtlw.models

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val likes: Int = 0,
    val comments: List<Comment> = emptyList(),
    val likedBy: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val deletedAt: Timestamp? = null,
    val isDeleted: Boolean = false,
    val isVisible: Boolean = true,
    val trashedAt: Timestamp? = null
) {
    val isLiked: Boolean
        get() = FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            likedBy.contains(uid)
        } ?: false

    fun getFormattedDate(): Date {
        return timestamp?.toDate() ?: Date()
    }

    fun isPostDeleted(): Boolean {
        return isDeleted || deletedAt != null || trashedAt != null
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "username" to username,
        "content" to content,
        "timestamp" to timestamp,
        "likes" to likes,
        "comments" to comments.map { it.toMap() },
        "likedBy" to likedBy,
        "avatarUrl" to avatarUrl,
        "deletedAt" to deletedAt,
        "isDeleted" to isDeleted,
        "isVisible" to isVisible,
        "trashedAt" to trashedAt
    )
}

data class Comment(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val likes: Int = 0,
    val isLiked: Boolean = false,
    val avatarUrl: String? = null
) {
    fun getFormattedDate(): Date {
        return timestamp?.toDate() ?: Date()
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "username" to username,
        "content" to content,
        "timestamp" to timestamp,
        "likes" to likes,
        "avatarUrl" to avatarUrl
    )
} 