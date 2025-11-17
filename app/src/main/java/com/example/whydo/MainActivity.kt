package com.example.whydo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.whydo.ui.chat.ChatRoute // [수정] ChatScreen 대신 ChatRoute 임포트
import com.example.whydo.ui.theme.WhyDoTheme
import androidx.compose.ui.Modifier // [수정] Modifier 임포트 추가

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 스플래시 스크린 설치 (반드시 super.onCreate보다 먼저!)
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            WhyDoTheme {
                // 배경색 설정을 위한 Surface 추가
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 2. [수정] ChatScreen() 대신 ChatRoute() 호출
                    // (ViewModel 연결 및 로직 처리는 ChatRoute가 담당함)
                    ChatRoute()
                }
            }
        }
    }
}