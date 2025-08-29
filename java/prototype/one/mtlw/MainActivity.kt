@file:Suppress("DEPRECATION")

package prototype.one.mtlw

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import prototype.one.mtlw.auth.AuthViewModel
import prototype.one.mtlw.components.RequestPermission
import prototype.one.mtlw.navigation.AppNavigation
import prototype.one.mtlw.navigation.BottomNavigationBar
import prototype.one.mtlw.navigation.DrawerContent
import prototype.one.mtlw.navigation.TopAppBar
import prototype.one.mtlw.ui.theme.MTLWTheme
import prototype.one.mtlw.viewmodels.PostViewModel
import prototype.one.mtlw.viewmodels.RecipeViewModel
import prototype.one.mtlw.viewmodels.UserViewModel
import java.security.MessageDigest

// Add at the top level, outside of MainActivity class
sealed class PermissionRequest {
    object Camera : PermissionRequest()
    object Storage : PermissionRequest()
    object Notification : PermissionRequest()
    object None : PermissionRequest()
}

@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(application) as T
            }
        }
    }
    
    private val postViewModel: PostViewModel by viewModels()
    private val expiryDateViewModel: prototype.one.mtlw.viewmodels.ExpiryDateViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rcSignIn = 9001
    private val recipeViewModel: RecipeViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val prefs = getSharedPreferences("permissions", MODE_PRIVATE)

        // Initialize Credential Manager with version check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                credentialManager = CredentialManager.create(this)
                Log.d("MainActivity", "CredentialManager initialized successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing CredentialManager: ${e.message}")
            }
        }

        // Check Google Play Services availability and initialize Google Sign-In
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("MainActivity", "Google Play Services is not available (status=$resultCode)")
            
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                // Show error dialog to user
                googleApiAvailability.getErrorDialog(this, resultCode, 1001)?.show()
            } else {
                // Non-resolvable error
                Toast.makeText(
                    this,
                    "This device doesn't support the required Google Play Services",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            // Initialize Google Sign-In with proper error handling
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                googleSignInClient = GoogleSignIn.getClient(this, gso)
                Log.d("MainActivity", "Google Sign-In initialized successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing Google Sign-In: ${e.message}")
                Toast.makeText(
                    this,
                    "Error initializing Google Sign-In. Please try again later.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Print SHA-1 for Firebase Console
        try {
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            info.signingInfo.apkContentsSigners.forEach { signature ->
                val md = MessageDigest.getInstance("SHA-1")
                val bytes = md.digest(signature.toByteArray())
                val hex = bytes.joinToString("") { "%02x".format(it) }
                Log.d("MainActivity", "SHA1: $hex")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting SHA-1", e)
        }

        enableEdgeToEdge()
        setContent {
            var currentPermissionRequest by remember { 
                mutableStateOf<PermissionRequest>(
                    when {
                        !prefs.getBoolean("asked_camera", false) -> PermissionRequest.Camera
                        !prefs.getBoolean("asked_storage", false) -> PermissionRequest.Storage
                        !prefs.getBoolean("asked_notification", false) -> PermissionRequest.Notification
                        else -> PermissionRequest.None
                    }
                )
            }

            fun openAppSettings() {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }

            fun onPermissionGranted() {
                when (currentPermissionRequest) {
                    is PermissionRequest.Camera -> {
                        prefs.edit { putBoolean("asked_camera", true) }
                        currentPermissionRequest = PermissionRequest.Storage
                    }
                    is PermissionRequest.Storage -> {
                        prefs.edit { putBoolean("asked_storage", true) }
                        currentPermissionRequest = PermissionRequest.Notification
                    }
                    is PermissionRequest.Notification -> {
                        prefs.edit { putBoolean("asked_notification", true) }
                        currentPermissionRequest = PermissionRequest.None
                    }
                    PermissionRequest.None -> {
                        // All permissions handled, do nothing
                    }
                }
            }

            when (currentPermissionRequest) {
                is PermissionRequest.Camera -> {
                    RequestPermission(
                        permission = Manifest.permission.CAMERA,
                        rationaleMessage = "This app needs camera access to scan expiry dates. Please grant camera permission.",
                        deniedMessage = "Camera permission is required to scan expiry dates. Please enable it in your device settings.",
                        onGranted = { onPermissionGranted() },
                        onDenied = { openAppSettings() }
                    )
                }
                is PermissionRequest.Storage -> {
                    RequestPermission(
                        permission =
                            Manifest.permission.READ_MEDIA_IMAGES,
                        rationaleMessage = "This app needs storage access to save and load images. Please grant storage permission.",
                        deniedMessage = "Storage permission is required to save and load images. Please enable it in your device settings.",
                        onGranted = { onPermissionGranted() },
                        onDenied = { openAppSettings() }
                    )
                }
                is PermissionRequest.Notification -> {
                    RequestPermission(
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                        rationaleMessage = "This app needs notification access to send you reminders and updates. Please grant notification permission.",
                        deniedMessage = "Notification permission is required to send reminders. Please enable it in your device settings.",
                        onGranted = { onPermissionGranted() },
                        onDenied = { openAppSettings() }
                    )
                }
                PermissionRequest.None -> {
                    // All permissions handled, do nothing
                }
            }

            MTLWTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val authState by authViewModel.authState.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val mainAppRoutes = setOf(
                    prototype.one.mtlw.navigation.Screen.Home.route,
                    prototype.one.mtlw.navigation.Screen.Generator.route,
                    prototype.one.mtlw.navigation.Screen.Tracker.route,
                    prototype.one.mtlw.navigation.Screen.Profile.route,
                    prototype.one.mtlw.navigation.Screen.Settings.route,
                    prototype.one.mtlw.navigation.Screen.Help.route,
                    prototype.one.mtlw.navigation.Screen.About.route,
                    "saved_recipes",
                    "cooked_recipes"
                )

                // Ensure user is navigated to Home after login/signup
                LaunchedEffect(authState) {
                    if (authState is prototype.one.mtlw.auth.AuthState.Success && currentRoute !in mainAppRoutes) {
                        navController.navigate(prototype.one.mtlw.navigation.Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                // Function to handle Google Sign-In
                val handleGoogleSignIn: () -> Unit = {
                    lifecycleScope.launch(Dispatchers.Main) {
                        try {
                            // Check if Google Play Services is available and up to date
                            val googleApiAvailability = GoogleApiAvailability.getInstance()
                            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this@MainActivity)
                            
                            if (resultCode != ConnectionResult.SUCCESS) {
                                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                                    googleApiAvailability.getErrorDialog(this@MainActivity, resultCode, 2404)?.show()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Google Play Services is not available on this device",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                return@launch
                            }

                            try {
                                // Try Credential Manager first
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(getString(R.string.default_web_client_id))
                                    .setAutoSelectEnabled(true)
                                    .build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = this@MainActivity
                                )
                                val credential = result.credential
                                
                                if (credential is CustomCredential && 
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    try {
                                        val googleIdTokenCredential = GoogleIdTokenCredential
                                            .createFrom(credential.data)
                                        authViewModel.signInWithGoogleToken(googleIdTokenCredential.idToken)
                                    } catch (e: GoogleIdTokenParsingException) {
                                        Log.e("MainActivity", "Failed to parse Google ID token", e)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Failed to parse Google credentials",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    Log.e("MainActivity", "Unexpected credential type")
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Unexpected credential type",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: GetCredentialException) {
                                when (e) {
                                    is NoCredentialException -> {
                                        Log.e("MainActivity", "No credentials available, falling back to traditional sign-in", e)
                                        // Fallback to traditional Google Sign-In
                                        startActivityForResult(googleSignInClient.signInIntent, rcSignIn)
                                    }
                                    else -> {
                                        Log.e("MainActivity", "Failed to get credentials", e)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Google Sign-In failed: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error setting up Google Sign-In", e)
                            Toast.makeText(
                                this@MainActivity,
                                "Error setting up Google Sign-In: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    // No need to handle auth state changes here
                }

                val noDrawerRoutes = setOf(
                    prototype.one.mtlw.navigation.Screen.Login.route,
                    prototype.one.mtlw.navigation.Screen.Signup.route,
                    prototype.one.mtlw.navigation.Screen.ForgotPassword.route,
                    prototype.one.mtlw.navigation.Screen.NewPassword.route,
                    prototype.one.mtlw.navigation.Screen.PasswordChangeSuccess.route,
                    prototype.one.mtlw.navigation.Screen.Loading.route // Also hide on loading
                )

                if (currentRoute !in noDrawerRoutes) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            DrawerContent(
                                navController = navController,
                                onCloseDrawer = { scope.launch { drawerState.close() } },
                                authViewModel = authViewModel,
                                expiryDateViewModel = expiryDateViewModel,
                                userViewModel = userViewModel
                            )
                        }
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                if (authState is prototype.one.mtlw.auth.AuthState.Success && currentRoute in mainAppRoutes) {
                                    TopAppBar(
                                        onMenuClick = { scope.launch { drawerState.open() } },
                                        authViewModel = authViewModel
                                    )
                                }
                            },
                            bottomBar = {
                                if (authState is prototype.one.mtlw.auth.AuthState.Success && currentRoute in mainAppRoutes) {
                                    BottomNavigationBar(navController = navController)
                                }
                            }
                        ) { paddingValues ->
                            AppNavigation(
                                navController = navController,
                                authViewModel = authViewModel,
                                postViewModel = postViewModel,
                                expiryDateViewModel = expiryDateViewModel,
                                onGoogleSignInClick = handleGoogleSignIn,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                userViewModel = userViewModel,
                                recipeViewModel = recipeViewModel
                            )
                        }
                    }
                } else {
                    // For screens where drawer is not needed, just show the content
                    Scaffold(
                        topBar = { 
                            // Optionally add a limited app bar for auth screens
                         }
                    ) { paddingValues ->
                         AppNavigation(
                            navController = navController,
                            modifier = Modifier.padding(paddingValues),
                            authViewModel = authViewModel,
                            postViewModel = postViewModel,
                            expiryDateViewModel = expiryDateViewModel,
                            onGoogleSignInClick = { onGoogleSignInClickFromUI() },
                            userViewModel = userViewModel,
                            recipeViewModel = recipeViewModel
                         )
                    }
                }
            }
        }
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, rcSignIn)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == rcSignIn) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            authViewModel.signInWithGoogleToken(account.idToken!!)
        } catch (e: ApiException) {
            // Sign in failed, update UI appropriately.
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this, "Google sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun googleSignInWithCredentialManager() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Fallback to startActivityForResult for older APIs if needed, or just disable
            Log.d("CredentialManager", "Credential Manager not supported on this API level")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(this@MainActivity, request)
                val credential = result.credential

                if (credential is GoogleIdTokenCredential) {
                    val idToken = credential.idToken
                    // Authenticate with Firebase
                    authViewModel.signInWithGoogleToken(idToken)
                } else if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                     // Handle cases where it might come back as a CustomCredential
                     try {
                          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                          val idToken = googleIdTokenCredential.idToken
                         authViewModel.signInWithGoogleToken(idToken)
                     } catch (e: GoogleIdTokenParsingException) {
                         Log.e("CredentialManager", "Error parsing GoogleIdTokenCredential: ${e.message}")
                         Toast.makeText(this@MainActivity, "Failed to sign in with Google. Please try again.", Toast.LENGTH_SHORT).show()
                     }
                } else {
                    Log.e("CredentialManager", "Unexpected credential type: ${credential.type}")
                    Toast.makeText(this@MainActivity, "Failed to sign in with Google. Unexpected credential type.", Toast.LENGTH_SHORT).show()
                }

            } catch (e: GetCredentialException) {
                Log.e("CredentialManager", "Error getting credential: ${e.message}")
                // Handle specific errors
                when (e) {
                    is NoCredentialException -> {
                         // No Google accounts found, prompt user to add one or use traditional sign-in
                         Toast.makeText(this@MainActivity, "No Google accounts found on device.", Toast.LENGTH_SHORT).show()
                    }
                     else -> {
                        Toast.makeText(this@MainActivity, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                     }
                }
            } catch (e: Exception) {
                 Log.e("CredentialManager", "An unexpected error occurred: ${e.message}", e)
                 Toast.makeText(this@MainActivity, "An unexpected error occurred during Google sign in.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // This is the function called by your UI button
    fun onGoogleSignInClickFromUI() {
        // Decide which Google Sign-In flow to use
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            googleSignInWithCredentialManager()
        } else {
            googleSignIn()
        }
    }
}