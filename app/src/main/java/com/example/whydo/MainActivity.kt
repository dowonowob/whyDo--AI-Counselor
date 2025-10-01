package com.example.whydo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.whydo.ui.theme.WhyDoTheme
import com.example.whydo.ui.chat.ChatScreen // ChatScreen을 임포트!
import com.example.whydo.ui.chat.sampleMessages // 샘플 데이터도 임포트!
import com.example.whydo.ui.theme.WhyDoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhyDoTheme {
                // 우리가 만든 ChatScreen을 여기서 호출합니다.
                ChatScreen(messages = sampleMessages)
            }
        }
    }
}