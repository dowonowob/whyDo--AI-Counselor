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

// 1. 화면 목록 정의
enum class Screen {
    Login, Signup, Home, Chat
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(applicationContext)

        setContent {
            WhyDoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.Login) }
                    var currentUserId by remember { mutableStateOf("") }
                    var currentSessionId by remember { mutableStateOf("") }
                    var selectedCategory by remember { mutableStateOf<String?>(null) }

                    // 2. 화면 전환 로직 (모든 Screen에 대한 처리가 필수!)
                    when (currentScreen) {
                        Screen.Login -> {
                            LoginScreen(
                                onLoginSuccess = { userId ->
                                    currentUserId = userId
                                    currentScreen = Screen.Home
                                },
                                onNavigateToSignup = {
                                    currentScreen = Screen.Signup
                                }
                            )
                        }

                        Screen.Signup -> {
                            SignupScreen(
                                onSignupSuccess = {
                                    currentScreen = Screen.Login
                                },
                                onBackClick = {
                                    currentScreen = Screen.Login
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
                                },
                                onLogout = {
                                    currentScreen = Screen.Login
                                }
                            )
                        }

                        // [이 부분이 빠져서 에러가 났던 것!]
                        Screen.Chat -> {
                            ChatRoute(
                                userId = currentUserId,
                                sessionId = currentSessionId,
                                category = selectedCategory,
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