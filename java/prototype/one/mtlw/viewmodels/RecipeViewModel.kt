package prototype.one.mtlw.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import prototype.one.mtlw.models.Recipe
import prototype.one.mtlw.utils.RecipeCache
import android.content.Context

class RecipeViewModel : ViewModel() {
    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe

    // Persistent search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<Recipe>>(emptyList())
    val searchResults: StateFlow<List<Recipe>> = _searchResults

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchResults(results: List<Recipe>) {
        _searchResults.value = results
    }

    fun setRecipes(list: List<Recipe>) {
        _recipes.value = list
    }

    fun selectRecipe(recipe: Recipe) {
        _selectedRecipe.value = recipe
    }

    fun clearRecipeCache(context: Context) {
        RecipeCache(context).clearCache()
    }
} 