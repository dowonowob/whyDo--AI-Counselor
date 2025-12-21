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
import com.example.whydo.data.local.TokenManager
import com.example.whydo.ui.chat.ChatListScreen
import com.example.whydo.ui.chat.ChatRoute
import com.example.whydo.ui.login.LoginScreen
import com.example.whydo.ui.login.SignupScreen
import com.example.whydo.ui.theme.WhyDoTheme

enum class Screen {
    Login, Signup, Home, Chat
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
                    var currentSessionTitle by remember { mutableStateOf("") }
                    var currentPersona by remember { mutableStateOf("friend") }

                    when (currentScreen) {
                        Screen.Login -> {
                            LoginScreen(
                                onLoginSuccess = { userId ->
                                    currentUserId = userId
                                    currentScreen = Screen.Home
                                },
                                onNavigateToSignup = { currentScreen = Screen.Signup }
                            )
                        }
                        Screen.Signup -> {
                            SignupScreen(
                                onSignupSuccess = { currentScreen = Screen.Login },
                                onBackClick = { currentScreen = Screen.Login }
                            )
                        }
                        Screen.Home -> {
                            ChatListScreen(
                                userId = currentUserId,
                                // [수정] title 파라미터 받기
                                onNavigateToChat = { sessionId, category, title, persona ->
                                    currentSessionId = sessionId
                                    selectedCategory = category
                                    currentSessionTitle = title
                                    currentPersona = persona
                                    currentScreen = Screen.Chat
                                },
                                onLogout = { currentScreen = Screen.Login }
                            )
                        }
                        Screen.Chat -> {
                            ChatRoute(
                                userId = currentUserId,
                                sessionId = currentSessionId,
                                sessionTitle = currentSessionTitle, // [추가] 제목 전달
                                category = selectedCategory,
                                persona = currentPersona,
                                onBackClick = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}