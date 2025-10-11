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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.whydo.data.model.Author
import com.example.whydo.data.model.ChatMessage
import com.example.whydo.ui.theme.WhyDoTheme
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val logTag = "VoiceDebug" // 디버깅용 로그 태그

    LaunchedEffect(key1 = Unit) {
        viewModel.initTts(context)
    }

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(logTag, "5. Speech recognizer result received. Code: ${result.resultCode}") // 로그 추가
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                Log.d(logTag, "6. Spoken text extracted: $spokenText") // 로그 추가
                viewModel.sendMessage(spokenText)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(logTag, "3. Permission result received. Granted: $isGranted") // 로그 추가
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀해주세요...")
            }
            Log.d(logTag, "4. Permission GRANTED. Launching speech recognizer...") // 로그 추가
            speechRecognitionLauncher.launch(intent)
        } else {
            Log.d(logTag, "4. Permission DENIED.") // 로그 추가
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("whyDo? 대화하기") }) },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        Log.d(logTag, "1. Mic button clicked.") // 로그 추가
                        Log.d(logTag, "2. Launching permission request...") // 로그 추가
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.size(60.dp),
                    enabled = !uiState.isLoading
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Speak", modifier = Modifier.fillMaxSize())
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 8.dp),
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

// ... MessageBubble 등 나머지 함수들은 그대로 유지 ...
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ChatScreenPreview() {
    WhyDoTheme {
        @Suppress("ViewModelConstructorInComposable")
        ChatScreen(viewModel = ChatViewModel())
    }
}