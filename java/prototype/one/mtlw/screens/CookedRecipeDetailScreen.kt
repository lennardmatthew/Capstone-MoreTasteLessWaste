package prototype.one.mtlw.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import prototype.one.mtlw.models.CookedRecipe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CookedRecipeDetailScreen(recipe: CookedRecipe, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Button
        IconButton(onClick = { navController.popBackStack() }) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = Color.Black.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(recipe.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Calories: ${recipe.calories} kcal", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        Text("Cooked on: ${dateFormat.format(Date(recipe.date))}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Instructions", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (recipe.details.isBlank()) {
            Text("No instructions available.", color = Color.Gray)
        } else {
            recipe.details.split("\n").forEachIndexed { idx, step ->
                Text("${idx + 1}. $step", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
    }
} 