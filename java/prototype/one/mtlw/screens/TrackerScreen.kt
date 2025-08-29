package prototype.one.mtlw.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.TextFields

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import prototype.one.mtlw.auth.AuthViewModel
import prototype.one.mtlw.components.RequestPermission
import prototype.one.mtlw.components.SimpleErrorDialog
import prototype.one.mtlw.viewmodels.ExpiryDateViewModel

@Composable
fun TrackerScreen(
    expiryDateViewModel: ExpiryDateViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController
) {
    LaunchedEffect(Unit) {
        expiryDateViewModel.listenToExpiryDatesRealtime()
    }
    var showCamera by remember { mutableStateOf(false) }
    var showManualEntry by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var notificationEnabled by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Request notification permission (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RequestPermission(
            permission = Manifest.permission.POST_NOTIFICATIONS,
            rationaleMessage = "This app needs notification access to send you reminders and updates. Please grant notification permission.",
            deniedMessage = "Notification permission is required to send reminders. Please enable it in your device settings.",
            onGranted = { notificationEnabled = true }
        )
    }

    // Listen for saved date from ScanResultScreen
    val savedDate = navController.currentBackStackEntry?.savedStateHandle?.get<java.time.LocalDate>("savedDate")
    LaunchedEffect(savedDate) {
        savedDate?.let { date ->
            isSaving = true
            try {
                expiryDateViewModel.saveExpiryDate("", date, context = context)
                // Show success message
                errorMessage = "Expiry date saved successfully!"
                showError = true
            } catch (e: Exception) {
                errorMessage = "Failed to save expiry date: ${e.message}"
                showError = true
            } finally {
                isSaving = false
                // Clear the saved date from navigation state
                navController.currentBackStackEntry?.savedStateHandle?.remove<java.time.LocalDate>("savedDate")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Expiry Tracker",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose a method to track expiry dates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Scan expiry date card (primary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCamera = true },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Scan Expiry Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp).padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Scan expiry date",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Use your camera to scan expiry labels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Manual entry card (secondary)
        val manualEnabled = !showCamera
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (manualEnabled) Modifier.clickable { showManualEntry = true } else Modifier)
                .alpha(if (manualEnabled) 1f else 0.4f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Manual Entry",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp).padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Manual entry",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Manually enter expiry date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Motivational card (bottom)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.RestaurantMenu,
                    contentDescription = "Food Icon",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Keep your food fresh!",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                    color = Color.White
                )
            }
        }

        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Saving expiry date...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (showError) {
            SimpleErrorDialog(
                title = "Notification",
                message = errorMessage,
                onDismiss = { showError = false }
            )
        }
    }

    if (showCamera) {
        CameraScreen(
            navController = navController,
            onError = { message ->
                errorMessage = message
                showError = true
                showCamera = false
            },
            onBack = {
                showCamera = false
            }
        )
    }

    if (showManualEntry) {
        ManualEntryScreen(
            onSave = { itemName, date ->
                expiryDateViewModel.saveExpiryDate(itemName, date, context = context)
                showManualEntry = false
            }
        )
    }
}
