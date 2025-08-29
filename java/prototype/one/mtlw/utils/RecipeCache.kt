package prototype.one.mtlw.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import prototype.one.mtlw.models.Recipe
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

class RecipeCache(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("recipe_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cacheDuration = TimeUnit.HOURS.toMillis(24) // Cache for 24 hours

    data class CacheEntry(
        val recipes: List<Recipe>,
        val timestamp: Long
    )

    fun saveRecipes(query: String, recipes: List<Recipe>) {
        val entry = CacheEntry(recipes, System.currentTimeMillis())
        sharedPreferences.edit { putString(query, gson.toJson(entry)) }
    }

    fun getRecipes(query: String): List<Recipe>? {
        val json = sharedPreferences.getString(query, null) ?: return null
        val entryType = object : TypeToken<CacheEntry>() {}.type
        val entry = gson.fromJson<CacheEntry>(json, entryType)
        
        // Check if cache is expired
        if (System.currentTimeMillis() - entry.timestamp > cacheDuration) {
            sharedPreferences.edit { remove(query) }
            return null
        }
        
        return entry.recipes
    }

    fun clearCache() {
        sharedPreferences.edit { clear() }
    }
} 