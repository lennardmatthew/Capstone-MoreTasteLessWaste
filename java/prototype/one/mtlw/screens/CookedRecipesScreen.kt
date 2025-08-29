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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import prototype.one.mtlw.ui.components.InfoChip
import prototype.one.mtlw.viewmodels.ExpiryDateViewModel
import prototype.one.mtlw.viewmodels.UserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookedRecipesScreen(
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
    expiryDateViewModel: ExpiryDateViewModel
) {
    val user by userViewModel.user.collectAsState()
    val cookedRecipes = user?.cookedRecipes ?: emptyList()
    val pendingViewRecipe = remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Cooked Recipes",
            style = MaterialTheme.typography.headlineMedium,
            color = Color(0xFF222222)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your cooking history",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (cookedRecipes.isEmpty()) {
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
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "No cooked recipes",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No cooked recipes yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cook some recipes to see them here",
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
                items(cookedRecipes) { recipe ->
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
                                text = recipe.name,
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
                                    icon = "🔥",
                                    text = "${recipe.calories} kcal"
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // View Details Button
                            Button(
                                onClick = { pendingViewRecipe.value = recipe.name },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1E824C)
                                )
                            ) {
                                Text("View Details")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // View Recipe Dialog
    if (pendingViewRecipe.value != null) {
        val recipe = cookedRecipes.find { it.name == pendingViewRecipe.value }
        if (recipe != null) {
            AlertDialog(
                onDismissRequest = { pendingViewRecipe.value = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                title = { 
                    Column {
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoChip(
                            icon = "🔥",
                            text = "${recipe.calories} kcal"
                        )
                    }
                },
                text = { 
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Instructions",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF222222),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Split instructions and format each step
                        val instructionSteps = recipe.details.split(",") // Split by comma
                            .map { step ->
                                step.trim()
                                    .removePrefix("['") // Remove leading ['
                                    .removePrefix("'") // Remove leading '
                                    .removeSuffix("]'") // Remove trailing ]'
                                    .removeSuffix("'") // Remove trailing '
                                    .removeSuffix("]") // Remove trailing ]
                            } // Trim and remove various prefixes/suffixes
                            .filter { it.isNotBlank() && it != "[]" } // Filter out empty or placeholder steps

                        instructionSteps.forEachIndexed { index, step ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Step number badge
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = Color(0xFF1E824C),
                                            shape = MaterialTheme.shapes.small
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                // Step text
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF444444),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { pendingViewRecipe.value = null },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF666666)
                            )
                        ) {
                            Text(
                                "Close",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        Button(
                            onClick = {
                                userViewModel.cookRecipeAgain(recipe)
                                pendingViewRecipe.value = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E824C)
                            )
                        ) {
                            Text(
                                "Cook Again",
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
} 