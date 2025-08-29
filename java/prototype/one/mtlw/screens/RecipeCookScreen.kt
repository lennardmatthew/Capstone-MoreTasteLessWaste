import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import prototype.one.mtlw.models.Recipe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCookScreen(recipe: Recipe, navController: NavController) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(scrollState)
    ) {
        TopAppBar(
            title = { Text(recipe.title, color = Color.Black) },
            navigationIcon = {
                IconButton(onClick = {
                    navController.previousBackStackEntry?.savedStateHandle?.set("showInput", true)
                    navController.popBackStack("generator", false)
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ingredients", style = MaterialTheme.typography.titleLarge, color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        recipe.ingredients.forEach { ingredient ->
            Text("• $ingredient", color = Color.Black, modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Instructions", style = MaterialTheme.typography.titleLarge, color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))
        if (recipe.instructions.isEmpty()) {
            Text("No instructions available.", color = Color.Gray, modifier = Modifier.padding(horizontal = 24.dp))
        } else {
            recipe.instructions.forEachIndexed { idx, step ->
                Text("${idx + 1}. $step", color = Color.Black, modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp))
            }
        }
    }
} 