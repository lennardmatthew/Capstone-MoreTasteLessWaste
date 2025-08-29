package prototype.one.mtlw.models

data class Recipe(
    val id: Int = 0,
    val title: String = "",
    val image: String = "",
    val readyInMinutes: Int = 0,
    val calories: Int = 0,
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList()
) 