package prototype.one.mtlw.viewmodels

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import prototype.one.mtlw.models.CalorieEntry
import prototype.one.mtlw.models.CookedRecipe
import prototype.one.mtlw.models.User
import prototype.one.mtlw.models.Recipe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar

class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // State for calorie range selection
    private val _startDate = MutableStateFlow(System.currentTimeMillis())
    val startDate: StateFlow<Long> = _startDate

    private val _endDate = MutableStateFlow(System.currentTimeMillis())
    val endDate: StateFlow<Long> = _endDate

    // State for calculated calories in the selected range
    private val _caloriesForRange = MutableStateFlow(0)
    val caloriesForRange: StateFlow<Int> = _caloriesForRange

    init {
        loadUser()
        // Initial calculation of calories for the default range
        updateCaloriesForRange(_startDate.value, _endDate.value)
    }

    fun loadUser() {
        val currentUser = auth.currentUser ?: return
        _isLoading.value = true
        _error.value = null
        
        usersCollection.document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                try {
                    val user = doc.toObject(User::class.java)
                    _user.value = user ?: User(
                        uid = currentUser.uid,
                        displayName = currentUser.displayName ?: "",
                        email = currentUser.email ?: "",
                        profilePhotoUrl = currentUser.photoUrl?.toString() ?: "",
                        cookedRecipes = emptyList(),
                        savedRecipes = emptyList(),
                        calorieHistory = emptyList(),
                        forumPostCount = 0,
                        recipesGenerated = 0,
                        recipesCooked = 0
                    )
                    Log.d("UserViewModel", "User data loaded successfully")
                } catch (e: Exception) {
                    Log.e("UserViewModel", "Error parsing user data: ${e.message}", e)
                    _error.value = "Failed to parse user data: ${e.message}"
                    // Create a default user if parsing fails
                    _user.value = User(
                        uid = currentUser.uid,
                        displayName = currentUser.displayName ?: "",
                        email = currentUser.email ?: "",
                        profilePhotoUrl = currentUser.photoUrl?.toString() ?: "",
                        cookedRecipes = emptyList(),
                        savedRecipes = emptyList(),
                        calorieHistory = emptyList(),
                        forumPostCount = 0,
                        recipesGenerated = 0,
                        recipesCooked = 0
                    )
                } finally {
                    _isLoading.value = false
                    // Recalculate calories after user data is loaded
                    updateCaloriesForRange(_startDate.value, _endDate.value)
                }
            }
            .addOnFailureListener { e ->
                Log.e("UserViewModel", "Error loading user: ${e.message}", e)
                _error.value = "Failed to load user data: ${e.message}"
                // Create a default user if loading fails
                _user.value = User(
                    uid = currentUser.uid,
                    displayName = currentUser.displayName ?: "",
                    email = currentUser.email ?: "",
                    profilePhotoUrl = currentUser.photoUrl?.toString() ?: "",
                    cookedRecipes = emptyList(),
                    savedRecipes = emptyList(),
                    calorieHistory = emptyList(),
                    forumPostCount = 0,
                    recipesGenerated = 0,
                    recipesCooked = 0
                )
                _isLoading.value = false
                // Recalculate calories even if loading fails to show 0
                updateCaloriesForRange(_startDate.value, _endDate.value)
            }
    }

    fun addCalories(calories: Int, date: Long = getStartOfDay(), onSuccess: () -> Unit = {}) {
        val current = _user.value ?: return
        _isLoading.value = true
        _error.value = null
        
        try {
            val today = current.calorieHistory.find { isSameDay(it.date, date) }
            val updatedHistory = if (today != null) {
                current.calorieHistory.map {
                    if (isSameDay(it.date, date)) it.copy(calories = it.calories + calories) else it
                }
            } else {
                current.calorieHistory + CalorieEntry(date = date, calories = calories)
            }
            val updated = current.copy(calorieHistory = updatedHistory)
            Log.d("UserViewModel", "Adding $calories calories for date $date")
            saveUser(updated) {
                Log.d("UserViewModel", "Calories added successfully")
                updateCaloriesForRange(_startDate.value, _endDate.value)
                _isLoading.value = false
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error adding calories: ${e.message}", e)
            _error.value = "Failed to add calories: ${e.message}"
            _isLoading.value = false
        }
    }

    private fun getStartOfDay(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } else {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
    }

    // Function to update calories for a specific range
    private fun updateCaloriesForRange(start: Long, end: Long) {
        try {
            val current = _user.value ?: return
            val calories = current.calorieHistory
                .filter { it.date in start..end }
                .sumOf { it.calories }
            _caloriesForRange.value = calories
            Log.d("UserViewModel", "Updated calories for range: $calories")
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error updating calories for range: ${e.message}", e)
            _error.value = "Failed to update calories for range: ${e.message}"
        }
    }

    // Functions to set start and end dates for calorie range
    fun setCaloriesStartDate(date: Long) {
        try {
            _startDate.value = date
            updateCaloriesForRange(_startDate.value, _endDate.value)
            Log.d("UserViewModel", "Set calories start date: $date")
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error setting calories start date: ${e.message}", e)
            _error.value = "Failed to set calories start date: ${e.message}"
        }
    }

    fun setCaloriesEndDate(date: Long) {
        try {
            _endDate.value = date
            updateCaloriesForRange(_startDate.value, _endDate.value)
            Log.d("UserViewModel", "Set calories end date: $date")
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error setting calories end date: ${e.message}", e)
            _error.value = "Failed to set calories end date: ${e.message}"
        }
    }

    private fun saveUser(user: User, onSuccess: () -> Unit = {}) {
        val currentUser = auth.currentUser ?: return
        _isLoading.value = true
        _error.value = null
        
        try {
            // Convert user object to map to ensure proper serialization
            val userMap = mapOf(
                "uid" to user.uid,
                "displayName" to user.displayName,
                "email" to user.email,
                "profilePhotoUrl" to user.profilePhotoUrl,
                "profilePictureBase64" to user.profilePictureBase64,
                "cookedRecipes" to user.cookedRecipes.map { recipe ->
                    mapOf(
                        "name" to recipe.name,
                        "date" to recipe.date,
                        "calories" to recipe.calories,
                        "details" to recipe.details
                    )
                },
                "savedRecipes" to user.savedRecipes.map { recipe ->
                    mapOf(
                        "id" to recipe.id,
                        "title" to recipe.title,
                        "image" to recipe.image,
                        "readyInMinutes" to recipe.readyInMinutes,
                        "calories" to recipe.calories,
                        "ingredients" to recipe.ingredients,
                        "instructions" to recipe.instructions
                    )
                },
                "calorieHistory" to user.calorieHistory.map { entry ->
                    mapOf(
                        "date" to entry.date,
                        "calories" to entry.calories
                    )
                },
                "forumPostCount" to user.forumPostCount,
                "recipesGenerated" to user.recipesGenerated,
                "recipesCooked" to user.recipesCooked
            )

            // Use set with merge option to prevent overwriting
            usersCollection.document(currentUser.uid)
                .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    _user.value = user
                    _isLoading.value = false
                    Log.d("UserViewModel", "User data saved successfully")
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("UserViewModel", "Error saving user data: ${e.message}", e)
                    _error.value = "Failed to save user data: ${e.message}"
                    _isLoading.value = false
                    // Reload user data to ensure consistency
                    loadUser()
                }
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error preparing user data: ${e.message}", e)
            _error.value = "Failed to prepare user data: ${e.message}"
            _isLoading.value = false
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val localDate1 = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(date1),
                ZoneId.systemDefault()
            ).toLocalDate()
            val localDate2 = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(date2),
                ZoneId.systemDefault()
            ).toLocalDate()
            localDate1 == localDate2
        } else {
            val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
            val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }

    fun incrementRecipesGenerated() {
        val current = _user.value ?: return
        val updated = current.copy(recipesGenerated = current.recipesGenerated + 1)
        saveUser(updated)
    }

    /*
     * Increments the forum post count for the user.
     * Currently unused but may be needed for future features.
     */
    fun incrementForumPostCount() {
        val current = _user.value ?: return
        val updated = current.copy(forumPostCount = current.forumPostCount + 1)
        saveUser(updated)
    }

    fun saveRecipe(recipe: Recipe, onSuccess: () -> Unit = {}) {
        val current = _user.value ?: return
        _isLoading.value = true
        _error.value = null
        
        try {
            // Check if recipe is already saved
            if (current.savedRecipes.any { it.id == recipe.id }) {
                Log.d("UserViewModel", "Recipe already saved: ${recipe.title}")
                _isLoading.value = false
                return // Recipe already saved
            }
            
            // Create a new list with the new recipe
            val updatedSavedRecipes = current.savedRecipes.toMutableList().apply {
                add(recipe)
            }
            
            val updated = current.copy(
                savedRecipes = updatedSavedRecipes
            )
            
            Log.d("UserViewModel", "Saving recipe: ${recipe.title}")
            saveUser(updated) {
                Log.d("UserViewModel", "Recipe saved successfully: ${recipe.title}")
                _isLoading.value = false
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error saving recipe: ${e.message}", e)
            _error.value = "Failed to save recipe: ${e.message}"
            _isLoading.value = false
        }
    }

    fun unsaveRecipe(recipeId: Int, onSuccess: () -> Unit = {}) {
        val current = _user.value ?: return
        _isLoading.value = true
        _error.value = null
        
        try {
            val updated = current.copy(
                savedRecipes = current.savedRecipes.filter { it.id != recipeId }
            )
            Log.d("UserViewModel", "Unsaving recipe with ID: $recipeId")
            saveUser(updated) {
                Log.d("UserViewModel", "Recipe unsaved successfully")
                _isLoading.value = false
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error unsaving recipe: ${e.message}", e)
            _error.value = "Failed to unsave recipe: ${e.message}"
            _isLoading.value = false
        }
    }

    fun cookRecipe(recipe: Recipe, onSuccess: () -> Unit = {}) {
        val current = _user.value ?: return
        _isLoading.value = true
        _error.value = null
        
        try {
            // Remove from saved if present
            val newSaved = current.savedRecipes.filter { it.id != recipe.id }
            
            // Create a new CookedRecipe
            val cooked = CookedRecipe(
                name = recipe.title,
                date = System.currentTimeMillis(),
                calories = recipe.calories,
                details = recipe.instructions.joinToString("\n")
            )
            
            // Check for duplicates
            if (current.cookedRecipes.any { 
                it.name.trim().equals(recipe.title.trim(), ignoreCase = true) && 
                it.calories == recipe.calories 
            }) {
                Log.d("UserViewModel", "Recipe already cooked: ${recipe.title}")
                _isLoading.value = false
                return
            }
            
            // Create a new list with the new cooked recipe
            val updatedCookedRecipes = current.cookedRecipes.toMutableList().apply {
                add(cooked)
            }
            
            val updated = current.copy(
                savedRecipes = newSaved,
                cookedRecipes = updatedCookedRecipes,
                recipesCooked = current.recipesCooked + 1
            )
            
            Log.d("UserViewModel", "Cooking recipe: ${recipe.title}")
            saveUser(updated) {
                Log.d("UserViewModel", "Recipe cooked successfully: ${recipe.title}")
                // Add calories to history when a recipe is cooked
                addCalories(recipe.calories)
                _isLoading.value = false
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error cooking recipe: ${e.message}", e)
            _error.value = "Failed to cook recipe: ${e.message}"
            _isLoading.value = false
        }
    }

    fun cookRecipeAgain(recipe: CookedRecipe, onSuccess: () -> Unit = {}) {
        val current = _user.value ?: return
        _isLoading.value = true
        _error.value = null
        
        try {
            // Create a new CookedRecipe with current timestamp
            val newCooked = recipe.copy(date = System.currentTimeMillis())
            
            // Create a new list with the new cooked recipe
            val updatedCookedRecipes = current.cookedRecipes.toMutableList().apply {
                add(newCooked)
            }
            
            val updated = current.copy(
                cookedRecipes = updatedCookedRecipes,
                recipesCooked = current.recipesCooked + 1
            )
            
            Log.d("UserViewModel", "Cooking recipe again: ${recipe.name}")
            saveUser(updated) {
                Log.d("UserViewModel", "Recipe cooked again successfully: ${recipe.name}")
                // Add calories to history when a recipe is cooked
                addCalories(recipe.calories)
                _isLoading.value = false
                onSuccess()
            }
        } catch (e: Exception) {
            Log.e("UserViewModel", "Error cooking recipe again: ${e.message}", e)
            _error.value = "Failed to cook recipe again: ${e.message}"
            _isLoading.value = false
        }
    }
}