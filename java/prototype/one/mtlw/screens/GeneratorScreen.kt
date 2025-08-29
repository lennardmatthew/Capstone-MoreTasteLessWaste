package prototype.one.mtlw.screens

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import prototype.one.mtlw.models.Recipe
import prototype.one.mtlw.utils.RecipeCache
import prototype.one.mtlw.utils.RecipeCsvHelper
import prototype.one.mtlw.viewmodels.RecipeViewModel
import prototype.one.mtlw.viewmodels.UserViewModel

@Composable
fun GeneratorScreen(navController: NavController, userViewModel: UserViewModel, recipeViewModel: RecipeViewModel) {
    val context = LocalContext.current
    val searchQuery by recipeViewModel.searchQuery.collectAsState()
    val searchResults by recipeViewModel.searchResults.collectAsState()
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    var screenState by remember { mutableStateOf(savedStateHandle?.get<GeneratorScreenState>("screenState") ?: GeneratorScreenState.MENU) }
    fun setScreenState(newState: GeneratorScreenState) {
        screenState = newState
        savedStateHandle?.set("screenState", newState)
    }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Check for scanned ingredient from navigation state
    val ingredientName = savedStateHandle?.get<String>("ingredientName")
    
    val recipeCache = remember { RecipeCache(context) }
    val csvHelper = remember { RecipeCsvHelper(context) }

    val allIngredients = remember {
        mutableSetOf<String>()
    }
    var ingredientsLoaded by remember { mutableStateOf(false) }

    val showInput = savedStateHandle?.get<Boolean>("showInput") == true

    LaunchedEffect(Unit) {
        if (!ingredientsLoaded) {
            try {
                val ingredients = csvHelper.getIngredients()
                allIngredients.addAll(ingredients)
                ingredientsLoaded = true
            } catch (e: Exception) {
                error = "Failed to load ingredients: ${e.message}"
            }
        }
    }

    LaunchedEffect(ingredientName) {
        if (ingredientName != null) {
            recipeViewModel.setSearchQuery(ingredientName)
            setScreenState(GeneratorScreenState.INPUT)
            savedStateHandle.remove<String>("ingredientName")
        }
    }

    LaunchedEffect(showInput) {
        if (showInput) {
            setScreenState(GeneratorScreenState.INPUT)
            savedStateHandle.remove<Boolean>("showInput")
        }
    }

    fun searchRecipes(query: String) {
        if (query.isBlank()) {
            recipeViewModel.setSearchResults(emptyList())
            recipeViewModel.setSearchQuery("")
            return
        }
        val tokens = query.split(" ").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        val invalidTokens = tokens.filter { token ->
            token.length < 3 || !token.all { ch -> ch.isLetter() }
        }
        if (invalidTokens.isNotEmpty()) {
            error = "Please enter valid ingredients (at least 3 letters, no numbers or symbols).\nTry common ingredients like: chicken, rice, tomato, etc."
            recipeViewModel.setSearchResults(emptyList())
            return
        }
        if (FirebaseAuth.getInstance().currentUser != null) {
            userViewModel.incrementRecipesGenerated()
        }
        isLoading = true
        error = null
        coroutineScope.launch {
            try {
                val allRecipes = csvHelper.getRecipes()
                val filtered = allRecipes.asSequence()
                    .filter { recipe ->
                        val title = recipe.title.lowercase()
                        val ingredients = recipe.ingredients.map { it.lowercase() }
                        tokens.any { token ->
                            title.contains(token) || ingredients.any { it.contains(token) }
                        }
                    }
                    .take(20)
                    .toList()
                recipeViewModel.setSearchResults(filtered)
                recipeViewModel.setSearchQuery(query)
                recipeViewModel.setRecipes(filtered)
                isLoading = false
                if (filtered.isEmpty()) {
                    error = "No recipes found. Try searching for a main ingredient (e.g., 'chicken', 'rice', 'tomato')."
                }
            } catch (e: Exception) {
                error = "Failed to load recipes: ${e.message}"
                isLoading = false
            }
        }
    }

    when (screenState) {
        GeneratorScreenState.MENU -> GeneratorMenuScreen(
            onInputIngredients = { setScreenState(GeneratorScreenState.INPUT) },
            onScanIngredients = {
                navController.navigate(prototype.one.mtlw.navigation.Screen.IngredientScan.route)
            }
        )
        GeneratorScreenState.INPUT -> IngredientInputScreen(
            userInput = searchQuery,
            onInputChange = { recipeViewModel.setSearchQuery(it) },
            onSubmit = { searchRecipes(searchQuery) },
            recipeResults = searchResults,
            onBack = { setScreenState(GeneratorScreenState.MENU) },
            userViewModel = userViewModel,
            recipeViewModel = recipeViewModel,
            navController = navController,
            isLoading = isLoading,
            error = error
        )
    }
}

enum class GeneratorScreenState { MENU, INPUT }

@Composable
fun GeneratorMenuScreen(onInputIngredients: () -> Unit, onScanIngredients: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Recipe Generator",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose a method for recipe generation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Scan ingredients card (primary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onScanIngredients() },
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
                        contentDescription = "Scan Ingredients",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp).padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Scan ingredients",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Use your camera to scan ingredient labels.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input ingredients card (secondary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onInputIngredients() },
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
                        imageVector = Icons.Default.Search,
                        contentDescription = "Input Ingredients",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp).padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Input ingredients",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Manually enter ingredients.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Motivational card (bottom, matches TrackerScreen notification card)
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
                    contentDescription = "Meal Icon",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You deserve a nice meal!",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun IngredientInputScreen(
    userInput: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    recipeResults: List<Recipe>,
    onBack: () -> Unit,
    userViewModel: UserViewModel,
    recipeViewModel: RecipeViewModel,
    navController: NavController,
    isLoading: Boolean = false,
    error: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Arrow back button under the top app bar
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF4CAF50) // Material Green 500
                )
            }
            Text(
                text = "Input ingredient",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = "Input ingredient to generate a recipe.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = userInput,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            placeholder = {
                Text(
                    "Enter ingredients",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = MaterialTheme.shapes.medium,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSubmit,
            shape = MaterialTheme.shapes.large,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.8f)
                .height(52.dp)
        ) {
            Text(
                "Find Recipes",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Red
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                items(recipeResults) { recipe: Recipe ->
                    RecipeCard(
                        recipe = recipe,
                        onClick = {
                            recipeViewModel.selectRecipe(recipe)
                            navController.navigate("recipe_detail/${recipe.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF222222),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onClick,
                modifier = Modifier
                    .height(36.dp)
            ) {
                Text("Cook", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: String,
    text: String
) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeCookScreen(recipe: Recipe, navController: NavController) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        TopAppBar(
            title = { Text(recipe.title, color = Color.Black) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )
        
        // Make the content scrollable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recipe Info Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoChip("⏱️", "Ready in ${recipe.readyInMinutes} minutes")
                    if (recipe.calories > 0) {
                        InfoChip("🔥", "${recipe.calories} calories")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Ingredients Section
            Text(
                "Ingredients",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    recipe.ingredients.forEach { ingredient ->
                        Text(
                            "• $ingredient",
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Instructions Section
            Text(
                "Instructions",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (recipe.instructions.isEmpty()) {
                        Text(
                            "No instructions available.",
                            color = Color.Gray
                        )
                    } else {
                        recipe.instructions.forEachIndexed { idx, step ->
                            Text(
                                "${idx + 1}. $step",
                                color = Color.Black,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
