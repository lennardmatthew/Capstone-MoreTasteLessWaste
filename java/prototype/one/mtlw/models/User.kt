package prototype.one.mtlw.models

// Represents a recipe the user has cooked
data class CookedRecipe(
    val name: String = "",
    val date: Long = System.currentTimeMillis(), // Unix timestamp
    val calories: Int = 0,
    val details: String = "" // Optional: ingredients, steps, etc.
)

// Represents a calorie entry for a specific day
data class CalorieEntry(
    val date: Long = System.currentTimeMillis(), // Unix timestamp (start of day)
    val calories: Int = 0
)

// User profile and stats
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val profilePhotoUrl: String? = null,
    val profilePictureBase64: String? = null,
    val cookedRecipes: List<CookedRecipe> = emptyList(),
    val savedRecipes: List<Recipe> = emptyList(),
    val calorieHistory: List<CalorieEntry> = emptyList(),
    val forumPostCount: Int = 0,
    val recipesGenerated: Int = 0,
    val recipesCooked: Int = 0
) {
    // Helper function to safely get display name
    fun getFormattedDisplayName(): String {
        return when {
            displayName.isNotBlank() -> displayName
            email.isNotBlank() -> email.split("@").firstOrNull() ?: "User"
            else -> "User"
        }
    }

    // Helper function to safely get profile picture
    fun getProfilePicture(): String {
        return when {
            profilePhotoUrl?.isNotBlank() == true -> profilePhotoUrl
            profilePictureBase64?.isNotBlank() == true -> profilePictureBase64
            else -> ""
        }
    }
}
