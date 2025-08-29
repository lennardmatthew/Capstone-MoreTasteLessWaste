package prototype.one.mtlw.utils

import android.content.Context
import com.opencsv.CSVReader
import prototype.one.mtlw.models.Recipe
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecipeCsvHelper(private val context: Context) {
    private var cachedRecipes: List<Recipe>? = null
    private var cachedIngredients: Set<String>? = null
    private val mutex = Mutex()

    suspend fun getRecipes(): List<Recipe> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedRecipes ?: loadRecipes().also { cachedRecipes = it }
        }
    }

    suspend fun getIngredients(): Set<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cachedIngredients ?: loadIngredients().also { cachedIngredients = it }
        }
    }

    private fun loadRecipes(): List<Recipe> {
        val recipes = mutableListOf<Recipe>()
        try {
            context.assets.open("recipes.csv").use { inputStream ->
                CSVReader(InputStreamReader(inputStream)).use { reader ->
                    val header = reader.readNext()?.toList() ?: emptyList()
                    val nameIdx = header.indexOf("Name")
                    val stepsIdx = header.indexOf("steps")
                    val ingredientsIdx = header.indexOf("ingredients")
                    val minutesIdx = header.indexOf("minutes")
                    val nutritionIdx = header.indexOf("nutrition")

                    // Debug log for header and indices
                    android.util.Log.d("RecipeCsvHelper", "Header: $header")
                    android.util.Log.d("RecipeCsvHelper", "Indices: Name=$nameIdx, Steps=$stepsIdx, Ingredients=$ingredientsIdx, Minutes=$minutesIdx, Nutrition=$nutritionIdx")

                    var id = 1
                    var lineArr = reader.readNext()
                    while (lineArr != null && id <= 2000) {  // Limit to 2000 recipes
                        val parts = lineArr.toList()
                        if (id <= 10) {
                            android.util.Log.d("RecipeCsvHelper", "Line $id: $parts")
                        }
                        if (parts.size > maxOf(nameIdx, stepsIdx, ingredientsIdx, minutesIdx, nutritionIdx)) {
                            val title = parts[nameIdx].trim().removeSurrounding("\"")
                            val rawIngredients = parts[ingredientsIdx].trim().removeSurrounding("\"").removePrefix("[").removeSuffix("]")
                            val ingredients = rawIngredients.split(",")
                                .map { it.replace("'", "").trim() }
                                .filter { it.isNotEmpty() }
                            val instructions = parts[stepsIdx].trim().removeSurrounding("\"").split(";").map { it.trim() }.filter { it.isNotEmpty() }
                            val readyInMinutes = parts.getOrNull(minutesIdx)?.trim()?.toIntOrNull() ?: 0
                            val nutritionString = parts.getOrNull(nutritionIdx)?.trim() ?: ""
                            val calories = nutritionString.removePrefix("[").removeSuffix("]").split(",")[0].trim().toDoubleOrNull()?.toInt() ?: 0
                            recipes.add(
                                Recipe(
                                    id = id++,
                                    title = title,
                                    image = "",
                                    readyInMinutes = readyInMinutes,
                                    calories = calories,
                                    ingredients = ingredients,
                                    instructions = instructions
                                )
                            )
                        }
                        // Clear references to help GC
                        lineArr = null
                        lineArr = reader.readNext()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return recipes
    }

    private fun loadIngredients(): Set<String> {
        val ingredients = mutableSetOf<String>()
        try {
            context.assets.open("recipes.csv").use { inputStream ->
                CSVReader(InputStreamReader(inputStream)).use { reader ->
                    val header = reader.readNext()?.toList() ?: emptyList()
                    val ingredientsIdx = header.indexOf("ingredients")
                    
                    var lineArr = reader.readNext()
                    while (lineArr != null) {
                        val parts = lineArr.toList()
                        if (parts.size > ingredientsIdx) {
                            val rawIngredients = parts[ingredientsIdx].trim().removeSurrounding("\"").removePrefix("[").removeSuffix("]")
                            rawIngredients.split(",")
                                .map { it.replace("'", "").trim() }
                                .filter { it.isNotEmpty() }
                                .forEach { ingredients.add(it.lowercase()) }
                        }
                        // Clear references to help GC
                        lineArr = null
                        lineArr = reader.readNext()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ingredients
    }

    fun clearCache() {
        cachedRecipes = null
        cachedIngredients = null
    }
}
