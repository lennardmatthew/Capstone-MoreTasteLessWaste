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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import prototype.one.mtlw.models.Recipe
import prototype.one.mtlw.ui.components.InfoChip
import prototype.one.mtlw.viewmodels.UserViewModel

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    navController: NavController,
    userViewModel: UserViewModel
) {
    var showCookDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    val user by userViewModel.user.collectAsState()
    val isRecipeSaved = user?.savedRecipes?.any { it.id == recipe.id } == true
    
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        item {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Back Button
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .size(40.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                // Recipe Title
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF222222)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Recipe Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        icon = "⏱️",
                        text = "${recipe.readyInMinutes} min"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Calories Info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF5F5F5),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Calories per serving",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF666666)
                        )
                        Text(
                            text = "${recipe.calories} kcal",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF1E824C)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Ingredients Section
                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF222222)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        recipe.ingredients.forEach { ingredient ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "•",
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = ingredient,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Instructions Section
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF222222)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (recipe.instructions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Text(
                            text = "No instructions available for this recipe. Try using the ingredients list to create your own version!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    val steps = if (recipe.instructions.size == 1 && recipe.instructions[0].trim().startsWith("[") && recipe.instructions[0].trim().endsWith("]")) {
                        recipe.instructions[0]
                            .removePrefix("[")
                            .removeSuffix("]")
                            .split(",")
                            .map { it.trim().removePrefix("'").removeSuffix("'") }
                            .filter { it.isNotEmpty() }
                    } else {
                        recipe.instructions
                    }
                    Column {
                        steps.forEachIndexed { index, instruction ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1E824C),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Text(
                                        text = instruction,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                        color = Color(0xFF222222),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Save Recipe Button
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecipeSaved) Color(0xFF1E824C) else Color(0xFF666666)
                        )
                    ) {
                        Text(
                            text = if (isRecipeSaved) "Saved" else "Save Recipe",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    
                    // Cook This Recipe Button
                    Button(
                        onClick = { showCookDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E824C)
                        )
                    ) {
                        Text(
                            text = "Cook Recipe",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Save Recipe Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Recipe", color = Color.Black) },
            text = { 
                Column {
                    Text(
                        if (isRecipeSaved) 
                            "This recipe is already saved. Would you like to unsave it?" 
                        else 
                            "Would you like to save this recipe for later?",
                        color = Color.Black
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isRecipeSaved) {
                            userViewModel.unsaveRecipe(recipe.id) {
                                showSaveDialog = false
                            }
                        } else {
                            userViewModel.saveRecipe(recipe) {
                                showSaveDialog = false
                            }
                        }
                    }
                ) {
                    Text(if (isRecipeSaved) "Unsave" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = Color.Black)
                }
            },
            containerColor = Color.White
        )
    }
    
    // Cook Recipe Dialog
    if (showCookDialog) {
        AlertDialog(
            onDismissRequest = { showCookDialog = false },
            title = { Text("Cook Recipe", color = Color.Black) },
            text = { 
                Column {
                    Text("Are you sure you want to cook this recipe?", color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This will add ${recipe.calories} calories to your daily calorie history.",
                        color = Color.Black
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userViewModel.cookRecipe(recipe) {
                            showCookDialog = false
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Cook")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCookDialog = false }) {
                    Text("Cancel", color = Color.Black)
                }
            },
            containerColor = Color.White
        )
    }
} 