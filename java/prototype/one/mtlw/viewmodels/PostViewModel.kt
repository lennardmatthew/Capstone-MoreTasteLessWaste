@file:Suppress("DEPRECATION")

package prototype.one.mtlw.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import prototype.one.mtlw.models.Comment
import prototype.one.mtlw.models.Post

sealed class PostUiState {
    object Loading : PostUiState()
    data class Success(
        val posts: List<Post>
    ) : PostUiState()
    data class Error(val message: String) : PostUiState()
}

class PostViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")
    private val deletedPostsCollection = firestore.collection("deleted_posts")
    private var postsListener: ListenerRegistration? = null
    
    init {
        // Configure Firestore settings
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings

        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            postsListener?.remove()
            if (firebaseAuth.currentUser != null) {
                listenToPostsRealtime()
            } else {
                _uiState.value = PostUiState.Loading
            }
        }
    }

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState: StateFlow<PostUiState> = _uiState

    fun listenToPostsRealtime() {
        postsListener?.remove()
        val currentUser = auth.currentUser
        postsListener = postsCollection
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("PostViewModel", "Error loading posts: ", error)
                    _uiState.value = PostUiState.Error("Failed to load posts: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val posts = snapshot.toObjects(Post::class.java)
                        .filter { !it.isDeleted && it.isVisible }
                    _uiState.value = PostUiState.Success(posts)
                } else {
                    _uiState.value = PostUiState.Success(emptyList())
                }
            }
    }

    suspend fun loadPosts() {
        try {
            _uiState.value = PostUiState.Loading
            val posts = postsCollection
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Post::class.java)
                .filter { it.isVisible && (!it.isDeleted || isCurrentUserAdmin()) }
            _uiState.value = PostUiState.Success(posts = posts)
        } catch (e: Exception) {
            Log.e("PostViewModel", "Error loading posts: ${e.message}")
            _uiState.value = PostUiState.Error("Failed to load posts: ${e.message}")
        }
    }

    fun createPost(content: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                val post = Post(
                    userId = currentUser?.uid ?: "",
                    username = currentUser?.displayName ?: "Anonymous",
                    content = content,
                    timestamp = com.google.firebase.Timestamp.now(),
                    avatarUrl = currentUser?.photoUrl?.toString()
                )
                postsCollection.add(post.toMap()).await()
                loadPosts() // Reload to get the latest posts
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error creating post: ${e.message}")
                _uiState.value = PostUiState.Error(e.message ?: "Failed to create post")
            }
        }
    }

    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                val comment = Comment(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = currentUser?.uid ?: "",
                    username = currentUser?.displayName ?: "Anonymous",
                    content = content,
                    timestamp = com.google.firebase.Timestamp.now(),
                    avatarUrl = currentUser?.photoUrl?.toString()
                )
                postsCollection.document(postId).update(
                    "comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment.toMap())
                ).await()
                loadPosts() // Reload to get the latest posts
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error adding comment: ${e.message}")
                _uiState.value = PostUiState.Error(e.message ?: "Failed to add comment")
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                val postRef = postsCollection.document(postId)
                val post = postRef.get().await().toObject(Post::class.java)
                    ?: throw IllegalStateException("Post not found")
                val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
                
                // Get the current likes array or create a new one
                val likedBy = post.likedBy.toMutableList()
                val isLiked = likedBy.contains(currentUser.uid)
                
                if (isLiked) {
                    // Unlike: remove user from likedBy and decrease likes count
                    likedBy.remove(currentUser.uid)
                    postRef.update(
                        "likedBy", likedBy,
                        "likes", com.google.firebase.firestore.FieldValue.increment(-1)
                    ).await()
                } else {
                    // Like: add user to likedBy and increase likes count
                    likedBy.add(currentUser.uid)
                    postRef.update(
                        "likedBy", likedBy,
                        "likes", com.google.firebase.firestore.FieldValue.increment(1)
                    ).await()
                }
                
                loadPosts() // Reload to get the latest posts
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error toggling like: ${e.message}")
                _uiState.value = PostUiState.Error(e.message ?: "Failed to toggle like")
            }
        }
    }

    fun deletePost(postId: String) {
        if (postId.isBlank()) {
            Log.e("PostViewModel", "deletePost called with blank ID")
            return
        }
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
                val postRef = postsCollection.document(postId)
                val post = postRef.get().await().toObject(Post::class.java)
                    ?: throw IllegalStateException("Post not found")
                if (post.userId != currentUser.uid) {
                    throw IllegalStateException("You can only delete your own posts")
                }
                // Soft delete: set isDeleted
                postRef.update("isDeleted", true, "deletedAt", com.google.firebase.Timestamp.now()).await()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error deleting post: ${e.message}")
                _uiState.value = PostUiState.Error(e.message ?: "Failed to delete post")
            }
        }
    }

    fun deletePostPermanently(postId: String) {
        if (postId.isBlank()) {
            Log.e("PostViewModel", "deletePostPermanently called with blank ID")
            return
        }
        viewModelScope.launch {
            try {
                val postRef = postsCollection.document(postId)
                postRef.delete().await()
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error permanently deleting post: ${e.message}")
                _uiState.value = PostUiState.Error(e.message ?: "Failed to permanently delete post")
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: throw IllegalStateException("User not logged in")
                
                val postRef = postsCollection.document(postId)
                val post = postRef.get().await().toObject(Post::class.java)
                    ?: throw IllegalStateException("Post not found")
                
                val comment = post.comments.find { it.id == commentId }
                    ?: throw IllegalStateException("Comment not found")
                
                if (comment.userId != currentUser.uid) {
                    throw IllegalStateException("You can only delete your own comments")
                }
                
                postRef.update(
                    "comments", com.google.firebase.firestore.FieldValue.arrayRemove(comment.toMap())
                ).await()
                
                loadPosts() // Reload to get the latest posts
            } catch (e: Exception) {
                Log.e("PostViewModel", "Error deleting comment: ${e.message}")
                _uiState.value = PostUiState.Error(e.message ?: "Failed to delete comment")
            }
        }
    }

    // Deprecated: Do not use. Filter for trashed posts in the UI using the real-time posts state.

    private fun isCurrentUserAdmin(): Boolean {
        // TODO: Implement admin check (e.g., check custom claim or UID)
        return false
    }

    override fun onCleared() {
        super.onCleared()
        postsListener?.remove()
    }
} 