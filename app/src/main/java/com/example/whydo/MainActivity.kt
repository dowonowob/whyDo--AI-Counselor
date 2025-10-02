// MainActivity.kt

package com.example.whydo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.whydo.ui.chat.ChatScreen // ChatScreen 임포트 확인
import com.example.whydo.ui.theme.WhyDoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhyDoTheme {
                // 이제 ChatScreen은 매개변수가 필요 없습니다.
                // ViewModel은 ChatScreen 내부에서 스스로 만들어 사용합니다.
                ChatScreen()
            }
        }
    }
}