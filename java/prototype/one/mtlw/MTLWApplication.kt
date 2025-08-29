package prototype.one.mtlw

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Suppress("DEPRECATION")
class MTLWApplication : Application() {
    companion object {
        private const val TAG = "MTLWApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Google Play Services first
            initializeGooglePlayServices()
            
            // Then initialize Firebase
            initializeFirebase()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing app: ${e.message}", e)
            Toast.makeText(
                this,
                "Error initializing app. Please try again later.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun initializeGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services is not available (status=$resultCode)")
            
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                Log.w(TAG, "Google Play Services error is user resolvable")
                // The MainActivity will handle showing the resolution dialog
            } else {
                Log.e(TAG, "Google Play Services error is not user resolvable")
                Toast.makeText(
                    this,
                    "This device doesn't support the required Google Play Services",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Log.d(TAG, "Google Play Services is available and up to date")
            
            // Get Google Play Services version
            try {
                val packageInfo = packageManager.getPackageInfo("com.google.android.gms", 0)
                Log.d(TAG, "Google Play Services version: ${packageInfo.versionName}")
            } catch (e: Exception) {
                Log.e(TAG, "Could not get Google Play Services version: ${e.message}")
            }
        }
    }

    private fun initializeFirebase() {
        try {
            // Configure Firestore with offline persistence
            val db = Firebase.firestore
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            db.firestoreSettings = settings
            
            Log.d(TAG, "Firestore configured with offline persistence")
            
            // Test Firestore connection with retry mechanism
            CoroutineScope(Dispatchers.IO).launch {
                var retryCount = 0
                val maxRetries = 3
                
                while (retryCount < maxRetries) {
                    try {
                        db.collection("test_connection")
                            .document("test")
                            .set(mapOf("test" to true))
                            .await()
                        
                        Log.d(TAG, "Firestore connection test successful")
                        break
                    } catch (e: Exception) {
                        retryCount++
                        Log.e(TAG, "Firestore connection test failed (attempt $retryCount): ${e.message}")
                        
                        if (retryCount < maxRetries) {
                            // Wait before retrying (exponential backoff)
                            kotlinx.coroutines.delay(1000L * (1 shl retryCount))
                        } else {
                            Log.e(TAG, "Firestore connection test failed after $maxRetries attempts")
                        }
                    }
                }
            }

            // Initialize Firebase Auth
            val auth = Firebase.auth
            Log.d(TAG, "Firebase Auth initialized")

            // Monitor auth state changes
            auth.addAuthStateListener { firebaseAuth ->
                val user = firebaseAuth.currentUser
                if (user != null) {
                    Log.d(TAG, "User is signed in: ${user.uid}")
                    
                    // Force token refresh to fix potential permission issues
                    user.getIdToken(true).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Firebase Auth token refreshed successfully")
                        } else {
                            Log.e(TAG, "Failed to refresh Firebase Auth token: ${task.exception?.message}")
                        }
                    }
                } else {
                    Log.d(TAG, "User is signed out")
                }
            }

            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
            throw e // Re-throw to be caught by the outer try-catch
        }
    }
}