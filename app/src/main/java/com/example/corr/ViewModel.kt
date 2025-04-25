package com.example.corr

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private suspend fun authenticateUser(
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
            Log.d("AuthResponse", response)
            parseAuthResponse(response)
        }
    } else {
        Log.e("AuthError", "HTTP error: ${conn.responseCode}")
        AuthResult(success = false, message = "HTTP error: ${conn.responseCode}")
    }
}

private fun parseAuthResponse(json: String): AuthResult {
    return try {
        val jsonObject = JSONObject(json)
        if (jsonObject.getBoolean("success")) {
            val userObject = jsonObject.getJSONObject("user")
            val id = userObject.getInt("id")
            val username = userObject.getString("username")

            AuthResult(
                success = true,
                user = User(id, username)
            )
        } else {
            val message = if (jsonObject.has("message")) {
                jsonObject.getString("message")
            } else {
                "Неизвестная ошибка"
            }
            AuthResult(success = false, message = message)
        }
    } catch (e: Exception) {
        Log.e("ParseError", "Ошибка обработки ответа", e)
        AuthResult(success = false, message = "Ошибка обработки ответа")
    }
}

class UserViewModel : ViewModel() {
    var userId by mutableStateOf<Int?>(null)
        private set

    var username by mutableStateOf("")
        private set

    fun setUser(id: Int, name: String) {
        userId = id
        username = name
    }
}