package com.example.corr

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.corr.ui.theme.CorrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

import android.content.Context

class LoginActivity : ComponentActivity() {
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
                    LoginScreen(
                        onLoginSuccess = { user ->
                            userViewModel.setUser (user.id, user.username)
                            Log.d("Login", "User  ID saved: ${user.id}, Username: ${user.username}")

                            // Сохранение ID пользователя в SharedPreferences
                            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putInt("user_id", user.id)
                                apply()
                            }

                            startActivity(Intent(this, MenuActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (User) -> Unit) {
    var username by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок
        Text(
            text = "Авторизация",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Поле для имени пользователя
        Text(
            text = "Имя пользователя",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )

        TextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),

            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле для пароля
        Text(
            text = "Пароль",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
        )

        TextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),

            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка входа
        Button(
            onClick = {
                if (username.text.isBlank() || password.text.isBlank()) {
                    errorMessage = "Заполните все поля"
                } else {
                    isLoading = true
                    errorMessage = null
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Войти")
        }

        // Индикатор загрузки
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        // Отображение ошибки
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Обработка авторизации
        LaunchedEffect(isLoading) {
            if (isLoading) {
                try {
                    val result = withContext(Dispatchers.IO) {
                        authenticateUser(username.text, password.text)
                    }

                    if (result.success) {
                        result.user?.let { onLoginSuccess(it) }
                    } else {
                        errorMessage = result.message ?: "Ошибка авторизации"
                    }
                } catch (e: Exception) {
                    errorMessage = "Ошибка соединения: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
            }
        }
    }
}

private suspend fun authenticateUser (
    username: String,
    password: String
): AuthResult {
    val url = URL("http://nkj1100.beget.tech/auth.php")
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    conn.doOutput = true

    val postData = """
        {
            "username": "$username",
            "password": "$password"
        }
    """.trimIndent()

    conn.outputStream.use { os ->
        os.write(postData.toByteArray())
        os.flush()
    }

    return if (conn.responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
            val response = reader.readText()
            Log.d("AuthResponse", response) // Логирование ответа
            parseAuthResponse(response)
        }
    } else {
        Log.e("AuthError", "HTTP error: ${conn.responseCode}") // Логирование ошибки
        AuthResult(success = false, message = "HTTP error: ${conn.responseCode}")
    }
}

private fun parseAuthResponse(json: String): AuthResult {
    return try {
        // Простой парсинг JSON (в реальном приложении используйте библиотеку)
        if (json.contains("\"success\":true")) {
            val idStart = json.indexOf("\"id\":") + 5
            val idEnd = json.indexOf(",", idStart)
            val id = json.substring(idStart, idEnd).trim().toInt()

            val usernameStart = json.indexOf("\"username\":\"") + 12
            val usernameEnd = json.indexOf("\"", usernameStart)
            val username = json.substring(usernameStart, usernameEnd)

            AuthResult(success = true, user = User(id, username))
        } else {
            val messageStart = json.indexOf("\"message\":\"") + 11
            val messageEnd = json.indexOf("\"", messageStart)
            val message = if (messageStart >= 11 && messageEnd > messageStart) {
                json.substring(messageStart, messageEnd)
            } else {
                "Неизвестная ошибка"
            }
            AuthResult(success = false, message = message)
        }
    } catch (e: Exception) {
        Log.e("ParseError", "Ошибка обработки ответа: ${e.localizedMessage}") // Логирование ошибки
        AuthResult(success = false, message = "Ошибка обработки ответа")
    }
}

data class AuthResult(
    val success: Boolean,
    val message: String? = null,
    val user: User? = null
)

data class User(
    val id: Int,
    val username: String
)