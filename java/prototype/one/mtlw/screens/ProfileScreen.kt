package prototype.one.mtlw.screens

//noinspection UsingMaterialAndMaterial3Libraries
// Switched to Material2 for snackbar support due to Material3 compatibility issue
//noinspection UsingMaterialAndMaterial3Libraries

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import prototype.one.mtlw.auth.AuthViewModel
import prototype.one.mtlw.components.RequestPermission
import prototype.one.mtlw.viewmodels.UserViewModel
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.window.Dialog
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val profileBitmap by authViewModel.profilePicture.collectAsState()
    var hasStoragePermission by remember { mutableStateOf(false) }
    val userProfile by authViewModel.currentUserProfile.collectAsState()
    val user by userViewModel.user.collectAsState()
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDate by userViewModel.startDate.collectAsState()
    val endDate by userViewModel.endDate.collectAsState()
    val calories by userViewModel.caloriesForRange.collectAsState()
    val scrollState = rememberScrollState()

    RequestPermission(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        },
        rationaleMessage = "This app needs storage access to select a profile picture. Please grant storage permission.",
        deniedMessage = "Storage permission is required to select a profile picture. Please enable it in your device settings.",
        onGranted = { hasStoragePermission = true }
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                // Convert URI to Bitmap
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    val base64String = authViewModel.bitmapToBase64(bitmap)
                    if (base64String.length > 1_000_000) {
                        message = "Please select a profile picture smaller than 1MB."
                    } else {
                        authViewModel.uploadProfilePictureBase64(base64String) { success ->
                            message = if (success) {
                                "Profile picture updated successfully."
                            } else {
                                "Failed to upload profile picture."
                            }
                        }
                    }
                } else {
                    message = "Failed to process selected image."
                }
                isLoading = false
            }
        }
    }

    // Auto-clear message after 3 seconds
    LaunchedEffect(message) {
        if (message != null) {
            delay(3000)
            message = null
        }
    }

    // Date format helper
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Card
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier
                    .width(340.dp)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6FFF6)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(110.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFFE0F7FA)
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap!!.asImageBitmap(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .padding(24.dp)
                                    .size(60.dp),
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { launcher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Upload", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                authViewModel.removeProfilePicture { success ->
                                    Toast.makeText(context, if (success) "Profile picture removed" else "Failed to remove profile picture", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Remove", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val displayName = userProfile?.getFormattedDisplayName() ?: "No Name Set"
                    val email = userProfile?.email ?: ""
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF1E824C)
                    )
                    if (email.isNotBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Cards (with icons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCard(
                icon = Icons.Filled.Restaurant,
                label = "Recipes Generated",
                value = user?.recipesGenerated ?: 0,
                color = Color(0xFF4CAF50)
            )
            StatCard(
                icon = Icons.Filled.LocalFireDepartment,
                label = "Recipes Cooked",
                value = user?.recipesCooked ?: 0,
                color = Color(0xFFFF9800)
            )
            StatCard(
                icon = Icons.Filled.Forum,
                label = "Forum Posts",
                value = user?.forumPostCount ?: 0,
                color = Color(0xFF2196F3)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Calorie Tracking Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = "Calorie Tracking",
                        tint = Color(0xFF43A047),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Calorie Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF43A047)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Date pickers for custom range
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                dateFormat.format(Date(startDate)),
                                color = Color(0xFF1E824C),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { showStartDatePicker = true },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E824C)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Start Date",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start", color = Color.White)
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                dateFormat.format(Date(endDate)),
                                color = Color(0xFF1E824C),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { showEndDatePicker = true },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E824C)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "End Date",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("End", color = Color.White)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Calories: $calories",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1E824C)
                )
            }
        }
    }

    // Custom DatePicker Dialogs
    if (showStartDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startDate)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            userViewModel.setCaloriesStartDate(it)
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("Confirm", color = Color(0xFF1E824C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = Color(0xFF222222))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF222222),
                headlineContentColor = Color(0xFF222222),
                weekdayContentColor = Color(0xFF222222),
                dayContentColor = Color(0xFF222222),
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = Color(0xFF1E824C),
                todayContentColor = Color(0xFF1E824C),
                todayDateBorderColor = Color(0xFF1E824C),
                navigationContentColor = Color(0xFF1E824C),
                yearContentColor = Color(0xFF222222),
                disabledYearContentColor = Color(0xFFBBBBBB),
                currentYearContentColor = Color(0xFF1E824C),
                selectedYearContentColor = Color.White,
                disabledSelectedYearContentColor = Color.White,
                selectedYearContainerColor = Color(0xFF1E824C),
                disabledSelectedYearContainerColor = Color(0xFFBBBBBB),
                disabledDayContentColor = Color(0xFFBBBBBB),
                disabledSelectedDayContainerColor = Color(0xFFBBBBBB),
                dayInSelectionRangeContentColor = Color(0xFF222222),
                dayInSelectionRangeContainerColor = Color(0xFF1E824C),
                dividerColor = Color(0xFF1E824C)
            ),
            content = {
                DatePicker(
                    state = state,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF222222),
                        headlineContentColor = Color(0xFF222222),
                        weekdayContentColor = Color(0xFF222222),
                        dayContentColor = Color(0xFF222222),
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = Color(0xFF1E824C),
                        todayContentColor = Color(0xFF1E824C),
                        todayDateBorderColor = Color(0xFF1E824C),
                        navigationContentColor = Color(0xFF1E824C),
                        yearContentColor = Color(0xFF222222),
                        disabledYearContentColor = Color(0xFFBBBBBB),
                        currentYearContentColor = Color(0xFF1E824C),
                        selectedYearContentColor = Color.White,
                        disabledSelectedYearContentColor = Color.White,
                        selectedYearContainerColor = Color(0xFF1E824C),
                        disabledSelectedYearContainerColor = Color(0xFFBBBBBB),
                        disabledDayContentColor = Color(0xFFBBBBBB),
                        disabledSelectedDayContainerColor = Color(0xFFBBBBBB),
                        dayInSelectionRangeContentColor = Color(0xFF222222),
                        dayInSelectionRangeContainerColor = Color(0xFF1E824C),
                        dividerColor = Color(0xFF1E824C)
                    )
                )
            }
        )
    }
    if (showEndDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = endDate)
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            userViewModel.setCaloriesEndDate(it)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("Confirm", color = Color(0xFF1E824C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", color = Color(0xFF222222))
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF222222),
                headlineContentColor = Color(0xFF222222),
                weekdayContentColor = Color(0xFF222222),
                dayContentColor = Color(0xFF222222),
                selectedDayContentColor = Color.White,
                selectedDayContainerColor = Color(0xFF1E824C),
                todayContentColor = Color(0xFF1E824C),
                todayDateBorderColor = Color(0xFF1E824C),
                navigationContentColor = Color(0xFF1E824C),
                yearContentColor = Color(0xFF222222),
                disabledYearContentColor = Color(0xFFBBBBBB),
                currentYearContentColor = Color(0xFF1E824C),
                selectedYearContentColor = Color.White,
                disabledSelectedYearContentColor = Color.White,
                selectedYearContainerColor = Color(0xFF1E824C),
                disabledSelectedYearContainerColor = Color(0xFFBBBBBB),
                disabledDayContentColor = Color(0xFFBBBBBB),
                disabledSelectedDayContainerColor = Color(0xFFBBBBBB),
                dayInSelectionRangeContentColor = Color(0xFF222222),
                dayInSelectionRangeContainerColor = Color(0xFF1E824C),
                dividerColor = Color(0xFF1E824C)
            ),
            content = {
                DatePicker(
                    state = state,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.White,
                        titleContentColor = Color(0xFF222222),
                        headlineContentColor = Color(0xFF222222),
                        weekdayContentColor = Color(0xFF222222),
                        dayContentColor = Color(0xFF222222),
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = Color(0xFF1E824C),
                        todayContentColor = Color(0xFF1E824C),
                        todayDateBorderColor = Color(0xFF1E824C),
                        navigationContentColor = Color(0xFF1E824C),
                        yearContentColor = Color(0xFF222222),
                        disabledYearContentColor = Color(0xFFBBBBBB),
                        currentYearContentColor = Color(0xFF1E824C),
                        selectedYearContentColor = Color.White,
                        disabledSelectedYearContentColor = Color.White,
                        selectedYearContainerColor = Color(0xFF1E824C),
                        disabledSelectedYearContainerColor = Color(0xFFBBBBBB),
                        disabledDayContentColor = Color(0xFFBBBBBB),
                        disabledSelectedDayContainerColor = Color(0xFFBBBBBB),
                        dayInSelectionRangeContentColor = Color(0xFF222222),
                        dayInSelectionRangeContainerColor = Color(0xFF1E824C),
                        dividerColor = Color(0xFF1E824C)
                    )
                )
            }
        )
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: Int, color: Color) {
    Card(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF222222),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF444444),
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}





