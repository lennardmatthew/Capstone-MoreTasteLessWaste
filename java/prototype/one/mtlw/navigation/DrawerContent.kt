package prototype.one.mtlw.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import prototype.one.mtlw.auth.AuthViewModel
import prototype.one.mtlw.viewmodels.UserViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DrawerContent(
    navController: NavHostController,
    onCloseDrawer: () -> Unit,
    authViewModel: AuthViewModel,
    expiryDateViewModel: prototype.one.mtlw.viewmodels.ExpiryDateViewModel,
    userViewModel: UserViewModel
) {
    // Listen for auth state changes to trigger recomposition
    val userProfile by authViewModel.currentUserProfile.collectAsState()
    val profileBitmap by authViewModel.profilePicture.collectAsState()
    val displayName = userProfile?.getFormattedDisplayName() ?: "User"
    val user by userViewModel.user.collectAsState()

    val expiryDateUiState by expiryDateViewModel.uiState.collectAsState()
    var pendingUnsaveId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight(),
        drawerContainerColor = Color.White,
        drawerContentColor = Color(0xFF222222)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Picture
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFF222222)
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap!!.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .padding(12.dp)
                                .size(40.dp),
                            tint = Color(0xFF444444)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // User Name
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFF222222)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Drawer Menu Items
            DrawerMenuItem(
                icon = Icons.Default.Person,
                label = "Profile",
                onClick = {
                    navController.navigate(Screen.Profile.route)
                    onCloseDrawer()
                }
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Restaurant,
                label = "Cooked Recipes",
                onClick = {
                    navController.navigate("cooked_recipes")
                    onCloseDrawer()
                }
            )

            // Add Saved Recipes menu item
            DrawerMenuItem(
                icon = Icons.Default.Favorite,
                label = "Saved Recipes",
                onClick = {
                    navController.navigate("saved_recipes")
                    onCloseDrawer()
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            // Saved Dates Section in a Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F6F6)),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Saved Dates",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1E824C),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    when (expiryDateUiState) {
                        is prototype.one.mtlw.viewmodels.ExpiryDateUiState.Loading -> {
                            Text("Loading...", color = Color.Gray)
                        }
                        is prototype.one.mtlw.viewmodels.ExpiryDateUiState.Error -> {
                            Text((expiryDateUiState as prototype.one.mtlw.viewmodels.ExpiryDateUiState.Error).message, color = Color.Red)
                        }
                        is prototype.one.mtlw.viewmodels.ExpiryDateUiState.Success -> {
                            val expiryDates = (expiryDateUiState as prototype.one.mtlw.viewmodels.ExpiryDateUiState.Success).expiryDates
                            if (expiryDates.isEmpty()) {
                                Text("No saved dates.", color = Color.Gray)
                            } else {
                                expiryDates.forEach { date ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = date.itemName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF222222)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = try {
                                                val localDate = date.expiryDate.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                                                localDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                                            } catch (_: Exception) { "Invalid date" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1E824C)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(
                                            onClick = { expiryDateViewModel.deleteExpiryDate(date.id, context) }
                                        ) {
                                            Text("Untrack", color = Color.Red)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0xFF222222)
            )
            
            // Sign Out Option
            DrawerMenuItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "Sign Out",
                onClick = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                    onCloseDrawer()
                }
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF444444)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
} 