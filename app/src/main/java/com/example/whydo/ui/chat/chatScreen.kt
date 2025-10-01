package com.example.whydo.ui.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.whydo.R // R 클래스를 임포트해야 drawable 리소스를 찾을 수 있습니다.
import com.example.whydo.data.model.Author
import com.example.whydo.data.model.ChatMessage

// --- 채팅 화면 전체 구조 ---
@Composable
fun ChatScreen(messages: List<ChatMessage>) {
    var textState by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("whyDo? 대화하기") }) },
        bottomBar = {
            // 하단 채팅 입력창
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("메시지를 입력하세요") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { /* TODO: 메시지 전송 로직 */ }) {
                    Icon(Icons.Filled.Send, contentDescription = "Send message")
                }
            }
        }
    ) { innerPadding ->
        // 메시지 리스트
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            reverseLayout = true // 새로운 메시지가 항상 아래에 오도록 리스트를 뒤집음
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// --- 메시지 말풍선 컴포넌트 ---
@Composable
fun MessageBubble(message: ChatMessage) {
    val horizontalArrangement = if (message.author == Author.USER) Arrangement.End else Arrangement.Start
    val bubbleColor = if (message.author == Author.USER) Color(0xFFE1F5FE) else Color(0xFFE8F5E9)

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


// --- 프리뷰를 위한 샘플 데이터 및 실행 ---
// 중요: 이 코드가 정상적으로 보이려면, res/drawable 폴더에
// profile_ai.png와 profile_user.png 이미지가 있어야 합니다.
// 없다면 임시로 R.drawable.ic_launcher_foreground 로 대체해서 테스트할 수 있습니다.
val sampleMessages = listOf(
    ChatMessage(Author.AI, "안녕하세요! 어떤 고민이 있으신가요?", R.drawable.ic_launcher_foreground, "Caroline"),
    ChatMessage(Author.USER, "요즘 진로 때문에 고민이 많아요.", R.drawable.ic_launcher_foreground, "John"),
    ChatMessage(Author.AI, "진로 고민이 많으시군요. 조금 더 자세히 이야기해주실 수 있나요?", R.drawable.ic_launcher_foreground, "Caroline"),
    ChatMessage(Author.USER, "네, 제가 뭘 잘하는지, 뭘 하고 싶은지 잘 모르겠어요. 너무 막막하게 느껴져요.", R.drawable.ic_launcher_foreground, "John")
)

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ChatScreenPreview() {
    ChatScreen(messages = sampleMessages)
}