package prototype.one.mtlw.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import prototype.one.mtlw.models.Comment
import prototype.one.mtlw.models.Post
import prototype.one.mtlw.viewmodels.PostUiState
import prototype.one.mtlw.viewmodels.PostViewModel
import prototype.one.mtlw.viewmodels.UserViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PostViewModel,
    userViewModel: UserViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var showNewPostDialog by remember { mutableStateOf(false) }
    var newPostContent by remember { mutableStateOf("") }
    var reloadTrigger by remember { mutableStateOf(false) }
    val maxChars = 2000
    val isOverLimit = newPostContent.length > maxChars
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid

    // Always listen for posts for the current user
    LaunchedEffect(userId) {
        viewModel.listenToPostsRealtime()
    }

    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger) {
            viewModel.loadPosts()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // 1. Post list (LazyColumn)
        when (val state = uiState) {
            is PostUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is PostUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Failed to load posts. Please try again.",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { reloadTrigger = !reloadTrigger }) {
                        Text("Retry", color = Color(0xFF222222), maxLines = 1)
                    }
                }
            }
            is PostUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.posts.filter { !it.isPostDeleted() }) { post ->
                        PostCard(
                            post = post,
                            onLikeClick = { postId ->
                                viewModel.toggleLike(postId)
                            },
                            onCommentSubmit = { postId, comment ->
                                viewModel.addComment(postId, comment)
                            },
                            onDeletePost = { postId ->
                                viewModel.deletePost(postId)
                            },
                            onDeleteComment = { postId, commentId ->
                                viewModel.deleteComment(postId, commentId)
                            }
                        )
                    }
                }
            }
        }

        // 2. Floating action button for creating a post (last so it overlaps and is clickable)
        androidx.compose.material3.FloatingActionButton(
            onClick = { showNewPostDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 24.dp),
            containerColor = Color(0xFF8BC34A), // Green color
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create Post",
                tint = Color.White
            )
        }
    }

    if (showNewPostDialog) {
        AlertDialog(
            onDismissRequest = { showNewPostDialog = false },
            title = { Text("Create New Post") },
            containerColor = Color.White,
            text = {
                prototype.one.mtlw.ui.OutlinedWhiteCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newPostContent,
                        onValueChange = { if (it.length <= maxChars) newPostContent = it },
                        label = { Text("What's on your mind?", color = Color(0xFF444444)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("What's on your mind?", color = Color(0xFFBBBBBB)) },
                        singleLine = true,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "${newPostContent.length}/$maxChars",
                            color = if (isOverLimit) Color.Red else Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPostContent.isNotBlank() && !isOverLimit) {
                            viewModel.createPost(newPostContent)
                            userViewModel.incrementForumPostCount()
                            newPostContent = ""
                            showNewPostDialog = false
                        }
                    },
                    enabled = newPostContent.isNotBlank() && !isOverLimit
                ) {
                    Text("Post", color = Color(0xFF222222), maxLines = 1)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPostDialog = false }) {
                    Text("Cancel", color = Color(0xFF222222), maxLines = 1)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCard(
    post: Post,
    onLikeClick: (String) -> Unit,
    onCommentSubmit: (String, String) -> Unit,
    onDeletePost: (String) -> Unit,
    onDeleteComment: (String, String) -> Unit
) {
    var showComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    prototype.one.mtlw.ui.OutlinedWhiteCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)

    )
    {
        // Post content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Username, avatar, timestamp, and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        color = Color(0xFF8BC34A)
                    ) {
                        if (post.avatarUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(post.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                modifier = Modifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = post.username,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF444444),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = dateFormat.format(post.getFormattedDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666),
                        maxLines = 1
                    )
                    
                    if (FirebaseAuth.getInstance().currentUser?.uid == post.userId) {
                        IconButton(
                            onClick = { onDeletePost(post.id) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Post",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Like and comment actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onLikeClick(post.id) }) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) Color(0xFF1E824C) else Color(0xFF444444),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = if (post.isLiked) 1.2f else 1f
                                    scaleY = if (post.isLiked) 1.2f else 1f
                                }
                        )
                    }
                    Text(
                        text = "${post.likes}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (post.isLiked) Color(0xFF1E824C) else Color(0xFF333333),
                        maxLines = 1
                    )
                }
                TextButton(onClick = { showComments = !showComments }) {
                    Text("${post.comments.size} Comments", maxLines = 1)
                }
            }

            // Comments section
            if (showComments) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    post.comments.forEach { comment ->
                        CommentItem(
                            comment = comment,
                            onDeleteComment = { commentId -> onDeleteComment(post.id, commentId) }
                        )
                    }

                    // Add comment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Add a comment...", color = Color.Black, maxLines = 1) },
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White, shape = RoundedCornerShape(8.dp)),
                            maxLines = 1,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    onCommentSubmit(post.id, commentText)
                                    commentText = ""
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    onDeleteComment: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Comment avatar
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (comment.avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(comment.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Avatar",
                    modifier = Modifier.padding(6.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.username,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
                            .format(comment.getFormattedDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    
                    if (FirebaseAuth.getInstance().currentUser?.uid == comment.userId) {
                        IconButton(
                            onClick = { onDeleteComment(comment.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Comment",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
} 