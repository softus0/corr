package com.example.corr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.corr.ui.theme.CorrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import com.example.corr.ui.theme.CustomFontFamily
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class FoodDetailActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val foodId = intent.getIntExtra("FOOD_ID", 0)
        val foodName = intent.getStringExtra("FOOD_NAME") ?: ""
        val foodImage = intent.getStringExtra("FOOD_IMAGE") ?: ""
        val foodIngredients = intent.getStringExtra("FOOD_INGREDIENTS") ?: ""

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getInt("user_id", -1)

        setContent {
            CorrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FoodDetailScreen(
                        userId = userId,
                        foodId = foodId,
                        foodName = foodName,
                        foodImage = foodImage,
                        foodIngredients = foodIngredients,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    userId: Int,
    foodId: Int,
    foodName: String,
    foodImage: String,
    foodIngredients: String,
    onBack: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Цвета для элементов
    val backgroundColor = Color.White
    val textColor = Color(0xFF000000)
    val dividerColor = Color(0xFFD9D9D9)
    val favoriteIconColor = if (isFavorite) Color.Red else textColor

    LaunchedEffect(Unit) {
        isFavorite = withContext(Dispatchers.IO) {
            checkFavorite(userId, foodId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = foodName,
                        color = textColor,
                        fontFamily = CustomFontFamily
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            if (isFavorite) {
                                removeFromFavorites(userId, foodId)
                            } else {
                                addToFavorites(userId, foodId)
                            }
                            isFavorite = !isFavorite
                        }
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = favoriteIconColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            // Изображение блюда
            AsyncImage(
                model = foodImage,
                contentDescription = foodName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )

            // Детали блюда
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = foodName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = textColor,
                    fontFamily = CustomFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Divider(
                    color = dividerColor,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "Ингредиенты:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = textColor,
                    fontFamily = CustomFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = foodIngredients,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontFamily = CustomFontFamily,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

suspend fun checkFavorite(userId: Int, foodId: Int): Boolean {
    val url = "http://nkj1100.beget.tech/favorites.php?user_id=$userId"
    val response = URL(url).readText()
    val json = JSONObject(response)
    if (json.getBoolean("success")) {
        val favorites = json.getJSONArray("favorites")
        for (i in 0 until favorites.length()) {
            if (favorites.getJSONObject(i).getInt("food_id") == foodId) {
                return true
            }
        }
    }
    return false
}

suspend fun addToFavorites(userId: Int, foodId: Int): Boolean {
    return withContext(Dispatchers.IO) {
        val url = URL("http://nkj1100.beget.tech/favorites.php")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        val requestBody = JSONObject().apply {
            put("user_id", userId)
            put("food_id", foodId)
        }.toString()
        connection.outputStream.use { os ->
            os.write(requestBody.toByteArray())
        }
        connection.responseCode == HttpURLConnection.HTTP_OK
    }
}

suspend fun removeFromFavorites(userId: Int, foodId: Int): Boolean {
    return withContext(Dispatchers.IO) {
        val url = URL("http://nkj1100.beget.tech/favorites.php")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.doOutput = true
        val requestBody = "user_id=$userId&food_id=$foodId"
        connection.outputStream.use { os ->
            os.write(requestBody.toByteArray())
        }
        connection.responseCode == HttpURLConnection.HTTP_OK
    }
}