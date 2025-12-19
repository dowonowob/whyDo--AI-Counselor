// /ui/chat/ChatScreen.kt

package com.example.whydo.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whydo.data.model.Author
import com.example.whydo.data.model.ChatMessage
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(
    userId: String,
    sessionId: String,
    sessionTitle: String, // [추가] 제목 받기
    category: String? = null,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onBackClick)

    LaunchedEffect(userId, sessionId) {
        viewModel.setInfoAndStart(userId, sessionId, category)
    }

    ChatScreen(
        uiState = uiState,
        title = sessionTitle, // [수정] sessionId 대신 sessionTitle 사용!
        onSendMessage = { viewModel.sendMessage(it) },
        onBackClick = onBackClick,
        onToggleMute = { viewModel.toggleMute() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    title: String,
    onSendMessage: (String) -> Unit,
    onBackClick: () -> Unit,
    onToggleMute: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var textState by remember { mutableStateOf("") }

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
                putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀해주세요...")
            }
            try {
                speechRecognitionLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e("ChatScreen", "STT 실행 실패: ${e.message}")
            }
        } else {
            Log.d("Permission", "RECORD_AUDIO permission denied.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로 가기"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (uiState.isMuted)
                                Icons.AutoMirrored.Filled.VolumeOff
                            else
                                Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (uiState.isMuted) "소리 켜기" else "소리 끄기",
                            tint = if (uiState.isMuted) Color.Gray else Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
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
                    enabled = !uiState.isLoading,

                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send,
                        autoCorrectEnabled = false
                    ),
                    visualTransformation = VisualTransformation.None
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                } else {
                    IconButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice")
                    }
                }
            }
        }
    ) { innerPadding ->
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

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }
}

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
            contentDescription = "${message.authorName} profile",
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