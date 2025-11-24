// /MainActivity.kt

package com.example.whydo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.whydo.ui.chat.ChatListScreen
import com.example.whydo.ui.chat.ChatRoute
import com.example.whydo.ui.login.LoginScreen
import com.example.whydo.ui.theme.WhyDoTheme
import com.example.whydo.data.local.TokenManager

enum class Screen {
    Login, Home, Chat
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.init(applicationContext)

        setContent {
            WhyDoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    var currentScreen by remember { mutableStateOf(Screen.Login) }
                    var currentUserId by remember { mutableStateOf("") }
                    var currentSessionId by remember { mutableStateOf("") }
                    var selectedCategory by remember { mutableStateOf<String?>(null) }

                    when (currentScreen) {
                        Screen.Login -> {
                            LoginScreen(
                                onLoginSuccess = { userId ->
                                    currentUserId = userId
                                    currentScreen = Screen.Home
                                }
                            )
                        }
                        Screen.Home -> {
                            ChatListScreen(
                                userId = currentUserId,
                                onNavigateToChat = { sessionId, category ->
                                    currentSessionId = sessionId
                                    selectedCategory = category
                                    currentScreen = Screen.Chat
                                }
                            )
                        }
                        Screen.Chat -> {
                            ChatRoute(
                                userId = currentUserId,
                                sessionId = currentSessionId,
                                category = selectedCategory,
                                // [수정] 뒤로가기 요청 시 Home 화면으로 상태 변경
                                onBackClick = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}