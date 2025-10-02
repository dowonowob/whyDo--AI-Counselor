// /ui/chat/ChatScreen.kt

package com.example.whydo.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whydo.R
import com.example.whydo.data.model.Author
import com.example.whydo.data.model.ChatMessage
import com.example.whydo.ui.theme.WhyDoTheme
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    // 1. ViewModel의 UI 상태를 구독
    val uiState by viewModel.uiState.collectAsState()
    var textState by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("whyDo? 대화하기") }) },
        bottomBar = {
            // 하단 채팅 입력창
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("메시지를 입력하세요") },
                    enabled = !uiState.isLoading // 로딩 중일 때는 입력 비활성화
                )
                Spacer(modifier = Modifier.width(8.dp))
                // 2. 전송 버튼 클릭 시 ViewModel의 sendMessage 함수 호출
                IconButton(
                    onClick = {
                        if (textState.isNotBlank()) {
                            viewModel.sendMessage(textState)
                            textState = "" // 입력창 비우기
                        }
                    },
                    enabled = !uiState.isLoading && textState.isNotBlank()
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send message")
                }
            }
        }
    ) { innerPadding ->
        // 3. ViewModel의 messages 리스트를 화면에 표시
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
        ) {
            items(uiState.messages) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
            // 4. 로딩 중일 때 로딩 인디케이터 표시
            if (uiState.isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // 메시지가 추가될 때마다 자동으로 스크롤을 맨 아래로 이동
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }
}

// --- 아래 UI 컴포넌트 함수들이 누락되었습니다 ---

@Composable
fun MessageBubble(message: ChatMessage) {
    val horizontalArrangement = if (message.author == Author.USER) Arrangement.End else Arrangement.Start
    val bubbleColor = if (message.author == Author.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.Bottom
    ) {
        if (message.author == Author.USER) {
            MessageBox(bubbleColor, message)
            Spacer(modifier = Modifier.width(8.dp))
            ProfileImage(message)
        } else {
            ProfileImage(message)
            Spacer(modifier = Modifier.width(8.dp))
            MessageBox(bubbleColor, message)
        }
    }
}

@Composable
fun ProfileImage(message: ChatMessage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = message.authorImageResId),
            contentDescription = "${message.authorName} profile picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Text(text = message.authorName, fontSize = 12.sp)
    }
}

@Composable
fun MessageBox(bubbleColor: Color, message: ChatMessage) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bubbleColor,
        modifier = Modifier.widthIn(max = 280.dp)
    ) {
        Text(
            text = message.content,
            modifier = Modifier.padding(12.dp),
            fontSize = 16.sp
        )
    }
}

// --- Preview 코드 ---

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ChatScreenPreview() {
    // Preview는 이제 ViewModel을 직접 생성해서 테스트합니다.
    WhyDoTheme {
        ChatScreen(viewModel = ChatViewModel())
    }
}