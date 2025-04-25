package com.example.corr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.corr.ui.theme.CorrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.getValue

class FavoritesActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CorrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val userId = sharedPreferences.getInt("user_id", -1)

                    if (userId != -1) {
                        // Устанавливаем userId во ViewModel
                        userViewModel.setUser(userId, "")
                        FavoritesScreen(userViewModel = userViewModel)
                    } else {
                        // Показываем экран для неавторизованных пользователей
                        UnauthorizedView()
                    }
                }
            }
        }
    }
}

@Composable
fun UnauthorizedView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Для просмотра избранного необходимо авторизоваться")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { /* Переход на экран авторизации */ }
        ) {
            Text("Войти")
        }
    }
}


@Composable
fun FavoritesScreen(userViewModel: UserViewModel) {
    var favoriteFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            // Теперь мы уверены, что userId не null
            val userId = userViewModel.userId ?: return@LaunchedEffect

            val result = withContext(Dispatchers.IO) {
                fetchFavorites(userId)
            }

            if (result.success) {
                favoriteFoods = result.foodItems ?: emptyList()
            } else {
                errorMessage = result.message ?: "Ошибка загрузки избранного"
            }
        } catch (e: Exception) {
            errorMessage = "Ошибка соединения: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Избранное",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            modifier = Modifier
                .padding(16.dp)
                .statusBarsPadding()
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

            if (favoriteFoods.isEmpty()) {
                Text(
                    text = "У вас пока нет избранных блюд",
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favoriteFoods) { food ->
                        FoodItemCard(food = food)
                    }
                }
            }
        }
    }
}

@Composable
fun FoodItemCard(food: FoodItem) {
    val context = LocalContext.current
    val imageLoader = rememberImageLoader()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = {
            val intent = Intent(context, FoodDetailActivity::class.java).apply {
                putExtra("FOOD_ID", food.id)
                putExtra("FOOD_NAME", food.name)
                putExtra("FOOD_IMAGE", food.imageUrl)
                putExtra("FOOD_INGREDIENTS", food.ingredients)
            }
            context.startActivity(intent)
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Добавляем изображение блюда
            AsyncImage(
                model = food.imageUrl,
                contentDescription = food.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = food.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = food.ingredients,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Функция для создания ImageLoader
@Composable
fun rememberImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .crossfade(true)
            .okHttpClient {
                OkHttpClient.Builder()
                    .build()
            }
            .build()
    }
}

suspend fun fetchFavorites(userId: Int): FavoritesApiResult {
    val url = URL("http://nkj1100.beget.tech/get_favorites.php?user_id=$userId")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"

    return try {
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d("API", "Favorites response: $response")
            parseFavoritesResponse(response)
        } else {
            Log.e("API", "Error fetching favorites: ${conn.responseCode}")
            FavoritesApiResult(success = false, message = "Ошибка сервера: ${conn.responseCode}")
        }
    } catch (e: Exception) {
        Log.e("FavoritesActivity", "Ошибка соединения", e)
        FavoritesApiResult(success = false, message = "Ошибка соединения: ${e.localizedMessage ?: "Неизвестная ошибка"}")
    } finally {
        conn.disconnect()
    }
}

fun parseFavoritesResponse(response: String): FavoritesApiResult {
    return try {
        val json = JSONObject(response)
        if (json.getBoolean("success")) {
            val foodsArray = json.getJSONArray("foods")
            val foodItems = mutableListOf<FoodItem>()
            for (i in 0 until foodsArray.length()) {
                val item = foodsArray.getJSONObject(i)
                foodItems.add(FoodItem(
                    id = item.getInt("id"),
                    name = item.getString("name"),
                    imageUrl = item.getString("image_url"),
                    ingredients = item.getString("ingredients"),
                    categoryId = item.getInt("category_id")
                ))
            }
            FavoritesApiResult(success = true, foodItems = foodItems)
        } else {
            FavoritesApiResult(success = false, message = json.getString("message"))
        }
    } catch (e: Exception) {
        Log.e("API", "Parsing error", e)
        FavoritesApiResult(success = false, message = "Ошибка обработки данных")
    }
}

data class FoodItem(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val ingredients: String,
    val categoryId: Int
)

data class FavoritesApiResult(
    val success: Boolean,
    val foodItems: List<FoodItem>? = null,
    val message: String? = null
)