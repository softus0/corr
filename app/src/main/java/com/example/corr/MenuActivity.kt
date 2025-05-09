package com.example.corr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.corr.ui.theme.CorrTheme
import org.json.JSONObject
import org.json.JSONArray
import android.util.Log
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import com.example.corr.ui.theme.CustomFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.getValue

class MenuActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//        enableEdgeToEdge()
        setContent {
            CorrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val userId = sharedPreferences.getInt("user_id", -1)
                    if (userId != -1) {
                        userViewModel.setUser (userId, userViewModel.username)
                    }

                    MenuScreen(userViewModel = userViewModel)
                }
            }
        }
    }
}

@Composable
fun MenuScreen(userViewModel: UserViewModel) {
    var menuCategories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Цвета для элементов
    val cardColor = Color(0xFFD9D9D9)
    val buttonColor = Color(0xFFD9D9D9)
    val backgroundColor = Color.White
    val textColor = Color(0xFF000000)

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val result = withContext(Dispatchers.IO) {
                fetchCategories()
            }
            if (result.success) {
                menuCategories = result.categories ?: emptyList()
            } else {
                errorMessage = result.message ?: "Ошибка загрузки категорий"
            }
        } catch (e: Exception) {
            errorMessage = "Ошибка соединения: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = textColor,
            fontFamily = CustomFontFamily,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, FavoritesActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = textColor
                )
            ) {
                Text(
                    text = "Избранное",
                    fontFamily = CustomFontFamily
                )
            }
        }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuCategories) { category ->
                    CategoryCard(
                        category = category,
                        cardColor = cardColor,
                        textColor = textColor,

                        onClick = {
                            val intent = Intent(context, FoodListActivity::class.java).apply {
                                putExtra("CATEGORY_ID", category.id)
                                putExtra("CATEGORY_NAME", category.name)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: Category,
    cardColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = textColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center // Выравнивание по центру
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                ),
                fontFamily = CustomFontFamily,
                textAlign = TextAlign.Center // Центрирование текста
            )
        }
    }
}

suspend fun fetchCategories(): ApiResult {
    val url = URL("http://nkj1100.beget.tech/get_categories.php")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "GET"

    return try {
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            Log.d("API", "Categories response: $response")
            parseCategoriesResponse(response)
        } else {
            Log.e("API", "Error fetching categories: ${conn.responseCode}")
            ApiResult(success = false, message = "Ошибка сервера: ${conn.responseCode}")
        }
    } catch (e: Exception) {
        Log.e("MenuActivity", "Ошибка соединения", e)
        ApiResult(success = false, message = "Ошибка соединения: ${e.localizedMessage ?: "Неизвестная ошибка"}")
    } finally {
        conn.disconnect() // Закрываем соединение
    }
}

fun parseCategoriesResponse(response: String): ApiResult {
    return try {
        val json = JSONObject(response)
        if (json.getBoolean("success")) {
            val categoriesArray = json.getJSONArray("categories")
            val categories = mutableListOf<Category>()
            for (i in 0 until categoriesArray.length()) {
                val item = categoriesArray.getJSONObject(i)
                categories.add(Category(
                    id = item.getInt("id"),
                    name = item.getString("name")
                ))
            }
            ApiResult(success = true, categories = categories)
        } else {
            ApiResult(success = false, message = json.getString("message"))
        }
    } catch (e: Exception) {
        Log.e("API", "Parsing error", e)
        ApiResult(success = false, message = "Ошибка обработки данных")
    }
}

data class Category(
    val id: Int,
    val name: String
)

data class ApiResult(
    val success: Boolean,
    val categories: List<Category>? = null,
    val message: String? = null
)