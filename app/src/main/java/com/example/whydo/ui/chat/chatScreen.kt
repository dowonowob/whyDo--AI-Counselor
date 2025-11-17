// /ui/chat/ChatScreen.kt

package com.example.whydo.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
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

// --- [1. ChatRoute] ---
// ViewModel과 UI를 연결해주는 껍데기 (실제 앱에서 호출됨)
@Composable
fun ChatRoute(
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ChatScreen(
        uiState = uiState,
        onSetSessionId = viewModel::setSessionIdAndStart,
        onSendMessage = viewModel::sendMessage
    )
}

// --- [2. ChatScreen] ---
// 순수하게 UI만 그리는 함수 (ViewModel 없음 -> 프리뷰 가능!)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onSetSessionId: (String) -> Unit,
    onSendMessage: (String) -> Unit
) {
    // 1. 세션 ID가 없으면 닉네임 입력 팝업
    if (uiState.sessionId == null) {
        NicknameEntryDialog(onConfirm = onSetSessionId)
    } else {
        // 2. 세션 ID가 있으면 채팅 화면 표시
        ChatContent(
            uiState = uiState,
            onSendMessage = onSendMessage
        )
    }
}

// --- [3. NicknameEntryDialog] ---
@Composable
fun NicknameEntryDialog(onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* 닫기 방지 */ },
        title = { Text("대화 시작하기") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("닉네임(대화 ID)을 입력하세요") }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("시작")
            }
        }
    )
}

// --- [4. ChatContent] ---
// 실제 채팅방 내부 UI
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar용
@Composable
fun ChatContent(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit
) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var textState by remember { mutableStateOf("") }

    // --- 음성 인식 및 권한 요청 로직 ---
    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                onSendMessage(results[0])
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀해주세요...")
            }
            speechRecognitionLauncher.launch(intent)
        } else {
            Log.d("Permission", "RECORD_AUDIO permission denied.")
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Chat (${uiState.sessionId})") }) },
        bottomBar = {
            // 하이브리드 입력창
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("메시지를 입력하거나 마이크를 누르세요") },
                    enabled = !uiState.isLoading
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (textState.isNotBlank()) {
                    IconButton(
                        onClick = {
                            onSendMessage(textState)
                            textState = ""
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send text message")
                    }
                } else {
                    IconButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "Start voice input")
                    }
                }
            }
        }
    ) { innerPadding ->
        // 스크롤 레이아웃
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
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
    }

    // 자동 스크롤
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }
}

// --- [5. MessageBubble & ProfileImage & MessageBox] ---
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
            modifier = Modifier.size(40.dp).clip(CircleShape)
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

// --- [6. Previews] ---
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ChatScreenPreview_NoSession() {
    WhyDoTheme {
        // 세션 ID가 없는 상태 (팝업이 뜨는 상태)
        ChatScreen(
            uiState = ChatUiState(sessionId = null), // 가짜 데이터 (세션 없음)
            onSetSessionId = {},
            onSendMessage = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ChatScreenPreview_WithSession() {
    WhyDoTheme {
        // 세션 ID가 있는 상태 (채팅 화면)
        ChatScreen(
            uiState = ChatUiState(
                sessionId = "TestUser", // 가짜 데이터 (세션 있음)
                messages = listOf(
                    ChatMessage(Author.AI, "안녕하세요!", R.drawable.profile_ai, "AI"),
                    ChatMessage(Author.USER, "반가워요", R.drawable.profile_user, "Me")
                )
            ),
            onSetSessionId = {},
            onSendMessage = {}
        )
    }
}