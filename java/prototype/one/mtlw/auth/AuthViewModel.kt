package prototype.one.mtlw.auth

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import prototype.one.mtlw.models.User
import prototype.one.mtlw.utils.PreferencesManager
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

// Sealed class to represent login lockout state
sealed class LoginLockoutState {
    object NotLockedOut : LoginLockoutState()
    data class TimedOut(val timeRemainingMillis: Long) : LoginLockoutState()
    object PermanentlyLockedOut : LoginLockoutState()
}

// Sealed class to represent navigation events
sealed class NavigationEvent {
    object NavigateToForgotPassword : NavigationEvent()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    private val preferencesManager = PreferencesManager(application)

    // Add state for login lockout
    private val _loginLockoutState = MutableStateFlow<LoginLockoutState>(LoginLockoutState.NotLockedOut)
    val loginLockoutState: StateFlow<LoginLockoutState> = _loginLockoutState.asStateFlow()

    // Add channel for navigation events
    private val navigationEventChannel = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = navigationEventChannel.receiveAsFlow()

    // Add shared state for profile picture
    private val _profilePicture = MutableStateFlow<Bitmap?>(null)
    val profilePicture: StateFlow<Bitmap?> = _profilePicture.asStateFlow()

    private val _currentUserProfile = MutableStateFlow<User?>(null)
    val currentUserProfile: StateFlow<User?> = _currentUserProfile.asStateFlow()
    private var userProfileListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        // Check if we should stay signed in based on remember me preference
        val rememberMe = preferencesManager.getRememberMe()
        Log.d("AuthViewModel", "Remember me preference: $rememberMe")
        
        // Add auth state listener for current session
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("AuthViewModel", "Auth state changed: User is signed in: ${user.uid}")
                _authState.value = AuthState.Success
                listenToUserProfile(user.uid)
            } else {
                Log.d("AuthViewModel", "Auth state changed: User is signed out")
                _authState.value = AuthState.Initial
                listenToUserProfile(null)
            }
        }

        // Only check remember me on app start and only if we're not in the middle of signing in
        val currentUser = auth.currentUser
        if (currentUser != null && !rememberMe && _authState.value != AuthState.Loading) {
            Log.d("AuthViewModel", "Remember me is false on init and user is signed in, signing out.")
            signOut()
        }

        // Load initial profile picture
        loadProfilePicture()
        checkLoginLockout() // Check lockout status on init
        Log.d("AuthViewModel", "AuthViewModel initialized.")
    }

    private fun loadProfilePicture() {
        getProfilePictureBase64 { base64 ->
            _profilePicture.value = base64?.let { base64ToBitmap(it) }
        }
    }

    // Check lockout status and update state
    private fun checkLoginLockout(email: String? = null) {
        val currentEmail = email ?: auth.currentUser?.email
        if (currentEmail == null) {
            _loginLockoutState.value = LoginLockoutState.NotLockedOut
            Log.d("AuthViewModel", "checkLoginLockout: No email available, NotLockedOut.")
            return
        }

        val lockoutTimestamp = preferencesManager.getLockoutTimestamp(currentEmail)
        val failedAttempts = preferencesManager.getFailedAttempts(currentEmail)
        val currentTime = System.currentTimeMillis()

        Log.d("AuthViewModel", "checkLoginLockout for $currentEmail: lockoutTimestamp=$lockoutTimestamp, failedAttempts=$failedAttempts, currentTime=$currentTime")

        if (lockoutTimestamp > currentTime) {
            // Still in a timeout period
            val timeRemaining = lockoutTimestamp - currentTime
            _loginLockoutState.value = LoginLockoutState.TimedOut(timeRemaining)
            Log.d("AuthViewModel", "checkLoginLockout: State set to TimedOut with $timeRemaining ms remaining.")
            viewModelScope.launch { // Launch a coroutine to count down the timer
                Log.d("AuthViewModel", "checkLoginLockout: Starting timeout countdown coroutine.")
                while (lockoutTimestamp > System.currentTimeMillis()) {
                    val timeRemaining = lockoutTimestamp - System.currentTimeMillis()
                    _loginLockoutState.value = LoginLockoutState.TimedOut(timeRemaining)
                    delay(1000) // Update every second
                    Log.d("AuthViewModel", "checkLoginLockout: Timeout countdown: $timeRemaining ms remaining.")
                }
                Log.d("AuthViewModel", "checkLoginLockout: Timeout countdown finished.")
                // When timer is done, check if it was permanent or just a timeout
                checkLoginLockout(currentEmail) // Re-check to transition state
            }
        } else if (failedAttempts >= 5) { // Assuming 5 failed attempts leads to permanent lockout after 5 min timeout
             _loginLockoutState.value = LoginLockoutState.PermanentlyLockedOut
             Log.d("AuthViewModel", "checkLoginLockout: State set to PermanentlyLockedOut.")
        } else {
            // Not locked out
            _loginLockoutState.value = LoginLockoutState.NotLockedOut
            Log.d("AuthViewModel", "checkLoginLockout: State set to NotLockedOut.")
        }
    }

    // Calculate next lockout duration based on failed attempts
    private fun getNextLockoutDurationMillis(failedAttempts: Int): Long {
        return when (failedAttempts) {
            3 -> TimeUnit.SECONDS.toMillis(10) // 10 seconds timeout after 3 failed attempts
            4 -> TimeUnit.MINUTES.toMillis(1) // 1 minute timeout after 4 failed attempts
            else -> TimeUnit.MINUTES.toMillis(5) // 5 minutes timeout for 5+ failed attempts
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String, rememberMe: Boolean) {
        Log.d("AuthViewModel", "signInWithEmailAndPassword called for email: $email, rememberMe: $rememberMe")
        // Check lockout status before attempting login
        val lockoutTimestamp = preferencesManager.getLockoutTimestamp(email)
        val currentTime = System.currentTimeMillis()

        if (lockoutTimestamp > currentTime) {
            // Still in a timeout period, update state and return
            Log.d("AuthViewModel", "signInWithEmailAndPassword: Account for $email is currently locked out.")
            checkLoginLockout(email)
            _authState.value = AuthState.Error("Too many failed attempts. Please wait.")
            return
        }
         if (preferencesManager.getFailedAttempts(email) >= 5 && lockoutTimestamp > 0) { // Check for permanent lockout
             Log.d("AuthViewModel", "signInWithEmailAndPassword: Account for $email is permanently locked out.")
             _authState.value = AuthState.Error("Account permanently locked out due to too many failed attempts.")
             _loginLockoutState.value = LoginLockoutState.PermanentlyLockedOut
             return
         }

        _authState.value = AuthState.Loading
        Log.d("AuthViewModel", "Attempting to sign in with email: $email")

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "Sign in successful for email: $email")
                    // Clear failed attempts and lockout on successful login
                    preferencesManager.clearLoginAttempts(email)
                    _loginLockoutState.value = LoginLockoutState.NotLockedOut
                    Log.d("AuthViewModel", "Cleared login attempts for $email on successful login.")

                    val user = auth.currentUser
                    if (user != null) {
                        // Check if user document exists in Firestore
                        firestore.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // Create new user document if it doesn't exist
                                    val newUser = User(
                                        uid = user.uid,
                                        email = user.email ?: "",
                                        displayName = user.displayName ?: user.email?.split("@")?.get(0) ?: "User"
                                    )
                                    firestore.collection("users").document(user.uid)
                                        .set(newUser)
                                        .addOnSuccessListener {
                                            Log.d("AuthViewModel", "User document created in Firestore")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("AuthViewModel", "Error creating user document", e)
                                        }
                                }
                            }
                    }
                    // Set remember me preference
                    preferencesManager.setRememberMe(rememberMe)
                    _authState.value = AuthState.Success
                } else {
                    val errorMessage = task.exception?.message ?: "Sign in failed"
                    Log.e("AuthViewModel", "Sign in failed for $email with other error: $errorMessage")
                    _authState.value = AuthState.Error(errorMessage)
                }
            }
    }

    fun signInWithGoogleToken(idToken: String, rememberMe: Boolean = true) {
        _authState.value = AuthState.Loading
        Log.d("AuthViewModel", "Attempting to sign in with Google")
        
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AuthViewModel", "Google sign in successful")
                        val user = auth.currentUser
                        if (user != null) {
                            // Check if user document exists in Firestore
                            firestore.collection("users").document(user.uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (!document.exists()) {
                                        // Create new user document if it doesn't exist
                                        val newUser = User(
                                            uid = user.uid,
                                            email = user.email ?: "",
                                            displayName = user.displayName ?: user.email?.split("@")?.get(0) ?: "User"
                                        )
                                        firestore.collection("users").document(user.uid)
                                            .set(newUser)
                                            .addOnSuccessListener {
                                                Log.d("AuthViewModel", "User document created in Firestore")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("AuthViewModel", "Error creating user document", e)
                                            }
                                    }
                                }
                        }
                        // Set remember me preference for Google sign in
                        preferencesManager.setRememberMe(rememberMe)
                        _authState.value = AuthState.Success
                    } else {
                        val errorMessage = task.exception?.message ?: "Google sign in failed"
                        Log.e("AuthViewModel", "Google sign in failed: $errorMessage", task.exception)
                        _authState.value = AuthState.Error(errorMessage)
                    }
                }
        } catch (e: Exception) {
            val errorMessage = "Failed to process Google sign in: ${e.message}"
            Log.e("AuthViewModel", errorMessage, e)
            _authState.value = AuthState.Error(errorMessage)
        }
    }

    fun signUpWithEmailAndPassword(name: String, email: String, password: String) {
        _authState.value = AuthState.Loading
        Log.d("AuthViewModel", "Attempting to sign up with email: $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.updateProfile(UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    )?.addOnCompleteListener { profileTask ->
                        // Create a User document in Firestore
                        val newUser = User(
                            uid = user.uid,
                            email = user.email ?: "",
                            displayName = name
                        )
                        firestore.collection("users").document(user.uid)
                            .set(newUser)
                            .addOnSuccessListener {
                                Log.d("AuthViewModel", "User document created in Firestore")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AuthViewModel", "Error creating user document", e)
                            }
                        Log.d("AuthViewModel", "Sign up successful")
                        _authState.value = AuthState.Success
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Sign up failed"
                    Log.e("AuthViewModel", "Sign up failed: $errorMessage", task.exception)
                    _authState.value = AuthState.Error(errorMessage)
                }
            }
    }

    fun resetPassword(email: String) {
        Log.d("AuthViewModel", "Attempting to reset password for email: $email")
        
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "Password reset email sent successfully")
                } else {
                    Log.e("AuthViewModel", "Password reset failed", task.exception)
                }
            }
    }

    fun signOut() {
        Log.d("AuthViewModel", "Signing out user")
        try {
            // Clear remember me preference first
            preferencesManager.setRememberMe(false)
            // Then sign out from Firebase
            auth.signOut()
            _authState.value = AuthState.Initial
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error signing out: ${e.message}")
        }
    }

    fun isUserSignedIn(): Boolean {
        val isSignedIn = auth.currentUser != null
        Log.d("AuthViewModel", "User signed in status: $isSignedIn")
        return isSignedIn
    }

    // Convert Bitmap to Base64 with compression
    fun bitmapToBase64(bitmap: Bitmap): String {
        try {
            // Calculate target size (max 500KB)
            val maxSize = 500 * 1024 // 500KB in bytes
            var quality = 100
            var outputStream = ByteArrayOutputStream()
            
            // Compress with initial quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            var size = outputStream.size()
            
            // If still too large, reduce quality until it fits
            while (size > maxSize && quality > 10) {
                quality -= 10
                outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                size = outputStream.size()
                Log.d("AuthViewModel", "Compressing image: quality=$quality, size=$size bytes")
            }
            
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Log.d("AuthViewModel", "Final image size: ${base64.length} characters")
            return base64
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error converting bitmap to base64: ${e.message}")
            throw e
        }
    }

    // Convert Base64 to Bitmap
    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (_: Exception) {
            null
        }
    }

    // Upload Base64 string to Firestore
    fun uploadProfilePictureBase64(base64String: String, onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return onResult(false)
        
        // Log the size of the base64 string
        Log.d("AuthViewModel", "Attempting to upload profile picture. Size: ${base64String.length} characters")
        
        if (base64String.length > 1_000_000) { // ~1MB
            Log.e("AuthViewModel", "Profile picture too large: ${base64String.length} characters")
            onResult(false)
            return
        }

        viewModelScope.launch {
            try {
                Log.d("AuthViewModel", "Starting profile picture upload for user: ${user.uid}")
                
                // First check if the document exists
                val document = firestore.collection("users").document(user.uid).get().await()
                Log.d("AuthViewModel", "User document exists: ${document.exists()}")
                
                if (document.exists()) {
                    // Document exists, update it
                    Log.d("AuthViewModel", "Updating existing user document")
                    firestore.collection("users").document(user.uid)
                        .update(mapOf(
                            "profilePictureBase64" to base64String,
                            "profilePhotoUrl" to null // Clear any existing URL
                        ))
                        .await()
                    
                    withContext(Dispatchers.Main) {
                        try {
                            val bitmap = base64ToBitmap(base64String)
                            if (bitmap != null) {
                                _profilePicture.value = bitmap
                                // Update the current user profile to reflect the change
                                _currentUserProfile.value = _currentUserProfile.value?.copy(
                                    profilePictureBase64 = base64String,
                                    profilePhotoUrl = ""
                                )
                                Log.d("AuthViewModel", "Profile picture updated successfully")
                                onResult(true)
                            } else {
                                Log.e("AuthViewModel", "Failed to convert base64 back to bitmap")
                                onResult(false)
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error updating UI with new profile picture: ${e.message}")
                            onResult(false)
                        }
                    }
                } else {
                    // Document doesn't exist, create it
                    Log.d("AuthViewModel", "Creating new user document")
                    val newUser = User(
                        uid = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "",
                        profilePictureBase64 = base64String,
                        profilePhotoUrl = ""
                    )
                    firestore.collection("users").document(user.uid)
                        .set(newUser)
                        .await()
                    
                    withContext(Dispatchers.Main) {
                        try {
                            val bitmap = base64ToBitmap(base64String)
                            if (bitmap != null) {
                                _profilePicture.value = bitmap
                                _currentUserProfile.value = newUser
                                Log.d("AuthViewModel", "New user document created with profile picture")
                                onResult(true)
                            } else {
                                Log.e("AuthViewModel", "Failed to convert base64 back to bitmap for new user")
                                onResult(false)
                            }
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error updating UI with new user profile picture: ${e.message}")
                            onResult(false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error uploading profile picture: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    // Retrieve Base64 string from Firestore
    fun getProfilePictureBase64(onResult: (String?) -> Unit) {
        val user = auth.currentUser ?: return onResult(null)
        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString("profilePictureBase64"))
            }
            .addOnFailureListener { onResult(null) }
    }

    private fun listenToUserProfile(uid: String?) {
        userProfileListener?.remove()
        if (uid == null) {
            _currentUserProfile.value = null
            return
        }
        userProfileListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _currentUserProfile.value = snapshot.toObject(User::class.java)
                    // Optionally update profile picture state
                    val base64 = snapshot.getString("profilePictureBase64")
                    _profilePicture.value = base64?.let { base64ToBitmap(it) }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove()
    }

    fun removeProfilePicture(onResult: (Boolean) -> Unit = {}) {
        val user = auth.currentUser ?: return onResult(false)
        
        viewModelScope.launch {
            try {
                firestore.collection("users").document(user.uid)
                    .update(mapOf(
                        "profilePictureBase64" to null,
                        "profilePhotoUrl" to null
                    ))
                    .await()
                
                withContext(Dispatchers.Main) {
                    _profilePicture.value = null
                    // Update the current user profile to reflect the change
                    _currentUserProfile.value = _currentUserProfile.value?.copy(
                        profilePictureBase64 = null,
                        profilePhotoUrl = ""
                    )
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error removing profile picture: ${e.message}")
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

}