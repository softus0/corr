package com.example.corr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.corr.ui.theme.CorrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FoodListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val categoryId = intent.getIntExtra("CATEGORY_ID", 0)
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: ""

        setContent {
            CorrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FoodListScreen(categoryId, categoryName)
                }
            }
        }
    }
}

@Composable
fun FoodListScreen(categoryId: Int, categoryName: String) {
    var foods by remember { mutableStateOf<List<Food>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(categoryId) {
        isLoading = true
        try {
            val result = withContext(Dispatchers.IO) {
                fetchFoodsByCategory(categoryId)
            }
            if (result.success) {
                foods = result.foods ?: emptyList()
            } else {
                errorMessage = result.message ?: "Ошибка загрузки блюд"
            }
        } catch (e: Exception) {
            errorMessage = "Ошибка соединения: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = categoryName,
//            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(foods) { food ->
                    FoodItem(food = food) {
                        val intent = Intent(context, FoodDetailActivity::class.java).apply {
                            putExtra("FOOD_ID", food.id)
                            putExtra("FOOD_NAME", food.name)
                            putExtra("FOOD_IMAGE", food.imageUrl)
                            putExtra("FOOD_INGREDIENTS", food.ingredients)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun FoodItem(food: Food, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Изображение блюда (слева)
            AsyncImage(
                model = food.imageUrl,
                contentDescription = food.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Название и ингредиенты (справа)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = food.ingredients,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

suspend fun fetchFoodsByCategory(categoryId: Int): FoodApiResult {
    val url = URL("http://nkj1100.beget.tech/get_foods.php?category_id=$categoryId")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"

    return try {
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d("API", "Foods response: $response") // Логирование ответа
            parseFoodsResponse(response)
        } else {
            Log.e("API", "Error fetching foods: ${conn.responseCode}")
            FoodApiResult(success = false, message = "Ошибка сервера: ${conn.responseCode}")
        }
    } catch (e: Exception) {
        Log.e("API", "Ошибка соединения", e)
        FoodApiResult(success = false, message = "Ошибка соединения: ${e.localizedMessage ?: "Неизвестная ошибка"}")
    }
}

fun parseFoodsResponse(response: String): FoodApiResult {
    return try {
        val json = JSONObject(response)
        if (json.getBoolean("success")) {
            val foodsArray = json.getJSONArray("foods")
            val foods = mutableListOf<Food>()
            for (i in 0 until foodsArray.length()) {
                val item = foodsArray.getJSONObject(i)
                foods.add(Food(
                    id = item.getInt("id"),
                    name = item.getString("name"),
                    imageUrl = item.getString("image_url"),
                    ingredients = item.getString("ingredients"),
                    categoryId = item.getInt("category_id")
                ))
            }
            FoodApiResult(success = true, foods = foods)
        } else {
            FoodApiResult(success = false , message = json.getString("message"))
        }
    } catch (e: Exception) {
        FoodApiResult(success = false, message = "Ошибка обработки данных: ${e.localizedMessage}")
    }
}

data class Food(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val ingredients: String,
    val categoryId: Int
)

data class FoodApiResult(
    val success: Boolean,
    val foods: List<Food>? = null,
    val message: String? = null
)