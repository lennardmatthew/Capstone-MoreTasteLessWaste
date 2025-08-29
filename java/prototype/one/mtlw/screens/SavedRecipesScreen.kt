package prototype.one.mtlw.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import prototype.one.mtlw.auth.AuthViewModel
import prototype.one.mtlw.models.Recipe
import prototype.one.mtlw.ui.components.InfoChip
import prototype.one.mtlw.viewmodels.ExpiryDateViewModel
import prototype.one.mtlw.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRecipesScreen(
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
    expiryDateViewModel: ExpiryDateViewModel
) {
    val user by userViewModel.user.collectAsState()
    val savedRecipes = user?.savedRecipes ?: emptyList()
    val pendingUnsaveId = remember { mutableStateOf<Int?>(null) }
    val pendingCookRecipe = remember { mutableStateOf<Recipe?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Saved Recipes",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF222222)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your collection of favorite recipes",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (savedRecipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "No saved recipes",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No saved recipes yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Save your favorite recipes to cook them later",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF999999)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(savedRecipes) { recipe ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Recipe Title
                            Text(
                                text = recipe.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF222222)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Recipe Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                InfoChip(
                                    icon = "⏱️",
                                    text = "${recipe.readyInMinutes} min"
                                )
                                InfoChip(
                                    icon = "🔥",
                                    text = "${recipe.calories} kcal"
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { pendingUnsaveId.value = recipe.id },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF666666)
                                    )
                                ) {
                                    Text("Unsave")
                                }
                                
                                Button(
                                    onClick = { pendingCookRecipe.value = recipe },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E824C)
                                    )
                                ) {
                                    Text("Cook")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Unsave Confirmation Dialog
    if (pendingUnsaveId.value != null) {
        AlertDialog(
            onDismissRequest = { pendingUnsaveId.value = null },
            title = { Text("Unsave Recipe", color = Color.Black) },
            text = { Text("Are you sure you want to remove this recipe from your saved recipes?", color = Color.Black) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { pendingUnsaveId.value = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            userViewModel.unsaveRecipe(pendingUnsaveId.value!!) {
                                pendingUnsaveId.value = null
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF666666)
                        )
                    ) {
                        Text(
                            "Unsave",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            containerColor = Color.White
        )
    }
    
    // Cook Recipe Dialog
    if (pendingCookRecipe.value != null) {
        AlertDialog(
            onDismissRequest = { pendingCookRecipe.value = null },
            title = { Text("Cook Recipe", color = Color.Black) },
            text = { 
                Column {
                    Text("Are you sure you want to cook this recipe?", color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will add ${pendingCookRecipe.value?.calories} calories to your daily calorie history.",
                        color = Color.Black
                    )
                }
            },
            confirmButton = {
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                 ) {
                    TextButton(
                        onClick = { pendingCookRecipe.value = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            pendingCookRecipe.value?.let { userViewModel.cookRecipe(it) {
                                pendingCookRecipe.value = null
                            } }
                        },
                         modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E824C)
                        )
                    ) {
                        Text(
                            "Cook",
                             modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            containerColor = Color.White
        )
    }
} 