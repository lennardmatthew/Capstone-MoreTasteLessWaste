package prototype.one.mtlw.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.delay
import prototype.one.mtlw.auth.AuthViewModel
import prototype.one.mtlw.auth.ForgotPasswordScreen
import prototype.one.mtlw.auth.LoginScreen
import prototype.one.mtlw.auth.NewPasswordScreen
import prototype.one.mtlw.auth.PasswordChangeSuccessScreen
import prototype.one.mtlw.auth.SignupScreen
import prototype.one.mtlw.screens.GeneratorScreen
import prototype.one.mtlw.screens.HelpScreen
import prototype.one.mtlw.screens.HomeScreen
import prototype.one.mtlw.screens.ProfileScreen
import prototype.one.mtlw.screens.TrackerScreen
import prototype.one.mtlw.ui.screens.LoadingScreen
import prototype.one.mtlw.viewmodels.PostViewModel
import prototype.one.mtlw.viewmodels.UserViewModel
import prototype.one.mtlw.screens.RecipeDetailScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Text
import prototype.one.mtlw.screens.SavedRecipesScreen
import prototype.one.mtlw.screens.CookedRecipesScreen
import prototype.one.mtlw.screens.CookedRecipeDetailScreen
import prototype.one.mtlw.screens.RecipeCookScreen

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Loading : Screen("loading", Icons.Default.Home, "Loading")
    object Generator : Screen("generator", Icons.Default.Build, "Generator")
    object Home : Screen("home", Icons.Default.Home, "Home")
    object Tracker : Screen("tracker", Icons.Default.Info, "Tracker")
    object Profile : Screen("profile", Icons.Default.Person, "Profile")
    object Settings : Screen("settings", Icons.Default.Settings, "Settings")
    object Help : Screen("help", Icons.AutoMirrored.Filled.Help, "Help")
    object About : Screen("about", Icons.Default.Info, "About")
    object Login : Screen("login", Icons.AutoMirrored.Filled.Login, "Login")
    object Signup : Screen("signup", Icons.Default.PersonAdd, "Signup")
    object ForgotPassword : Screen("forgot_password", Icons.Default.Lock, "Forgot Password")
    object NewPassword : Screen("new_password", Icons.Default.Lock, "New Password")
    object PasswordChangeSuccess : Screen("password_change_success", Icons.Default.Lock, "Password Changed")
    // Removed ScanResult screen; results are shown in-bubble on the camera screen
    object IngredientScan : Screen("ingredient_scan", Icons.Default.CenterFocusStrong, "Ingredient Scan")
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    postViewModel: PostViewModel,
    expiryDateViewModel: prototype.one.mtlw.viewmodels.ExpiryDateViewModel,
    onGoogleSignInClick: () -> Unit,
    userViewModel: UserViewModel,
    recipeViewModel: prototype.one.mtlw.viewmodels.RecipeViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Loading.route,
        modifier = modifier
    ) {
        composable(
            route = Screen.Loading.route
        ) {
            LoadingScreen()
            LaunchedEffect(Unit) {
                delay(1000) // 1 second delay
                val destination = if (authViewModel.isUserSignedIn()) {
                    Screen.Home.route
                } else {
                    Screen.Login.route
                }
                navController.navigate(destination) {
                    popUpTo(Screen.Loading.route) { inclusive = true }
                }
            }
        }
        composable(
            route = Screen.Generator.route
        ) {
            GeneratorScreen(navController = navController, userViewModel = userViewModel, recipeViewModel = recipeViewModel)
        }
        composable(Screen.Home.route) {
            HomeScreen(viewModel = postViewModel, userViewModel = userViewModel)
        }
        composable(
            route = Screen.Tracker.route
        ) {
            TrackerScreen(
                expiryDateViewModel = expiryDateViewModel,
                authViewModel = authViewModel,
                navController = navController
            )
        }
        composable(
            route = Screen.Profile.route
        ) {
            ProfileScreen(authViewModel = authViewModel, userViewModel = userViewModel)
        }
        composable(
            route = Screen.Help.route
        ) {
            HelpScreen()
        }
        composable(
            route = Screen.Login.route
        ) {
            LoginScreen(
                navController = navController,
                onGoogleSignInClick = onGoogleSignInClick,
                onLoginClick = { email, password, rememberMe ->
                    authViewModel.signInWithEmailAndPassword(email, password, rememberMe)
                }
            )
        }
        composable(
            route = Screen.Signup.route
        ) {
            SignupScreen(
                navController = navController,
                onGoogleSignInClick = onGoogleSignInClick,
                onSignupClick = { name, email, password ->
                    authViewModel.signUpWithEmailAndPassword(name, email, password)
                }
            )
        }
        composable(
            route = Screen.ForgotPassword.route
        ) {
            ForgotPasswordScreen(
                navController = navController,
                onResetClick = { email ->
                    authViewModel.resetPassword(email)
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.NewPassword.route
        ) {
            NewPasswordScreen(
                navController = navController,
                onPasswordReset = { newPassword ->
                    // Handle password reset
                    navController.navigate(Screen.PasswordChangeSuccess.route)
                }
            )
        }
        composable(
            route = Screen.PasswordChangeSuccess.route
        ) {
            PasswordChangeSuccessScreen(navController = navController)
        }
        // ScanResult removed; saving happens from camera bubble or manual entry
        composable(
            route = Screen.IngredientScan.route
        ) {
            prototype.one.mtlw.screens.IngredientScanScreen(
                navController = navController
            )
        }
        composable(
            route = "cook_recipe/{id}"
        ) { backStackEntry ->
            val recipe by recipeViewModel.selectedRecipe.collectAsState()
            if (recipe != null) {
                RecipeCookScreen(recipe = recipe!!, navController = navController)
            } else {
                Text("Recipe not found.")
            }
        }
        composable(
            route = "recipe_detail/{id}"
        ) { backStackEntry ->
            val recipe by recipeViewModel.selectedRecipe.collectAsState()
            if (recipe != null) {
                RecipeDetailScreen(
                    recipe = recipe!!,
                    navController = navController,
                    userViewModel = userViewModel
                )
            } else {
                Text("Recipe not found.")
            }
        }
        composable(
            route = "saved_recipes"
        ) {
            SavedRecipesScreen(
                userViewModel = userViewModel,
                authViewModel = authViewModel,
                expiryDateViewModel = expiryDateViewModel
            )
        }
        composable(
            route = "cooked_recipes"
        ) {
            CookedRecipesScreen(
                userViewModel = userViewModel,
                authViewModel = authViewModel,
                expiryDateViewModel = expiryDateViewModel
            )
        }
        composable(
            route = "cooked_recipe_detail/{name}"
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val user by userViewModel.user.collectAsState()
            val recipe = user?.cookedRecipes?.find { it.name == name }
            if (recipe != null) {
                CookedRecipeDetailScreen(recipe = recipe, navController = navController)
            } else {
                Text("Cooked recipe not found.")
            }
        }
    }
}