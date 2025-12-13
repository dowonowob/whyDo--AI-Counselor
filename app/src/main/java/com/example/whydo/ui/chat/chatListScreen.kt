package com.example.whydo.ui.chat

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    userId: String,
    onNavigateToChat: (String, String?) -> Unit,
    onLogout: () -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 팝업 상태 관리
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // 새 대화 입력 상태
    var newTopic by remember { mutableStateOf("") }
    // [추가] 입력 에러 메시지 상태
    var inputError by remember { mutableStateOf<String?>(null) }

    val categories = listOf(
        "학업/시험", "진로/미래", "대인관계", "자존감/자아", "생활습관", "기타"
    )
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var expanded by remember { mutableStateOf(false) }

    // 화면 진입 시 목록 로드
    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }

    // 뒤로가기 버튼 처리 로직
    BackHandler(enabled = true) {
        if (uiState.isSelectionMode) {
            viewModel.clearSelectionMode()
        } else {
            showExitDialog = true
        }
    }

    // 앱 종료 확인 팝업
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("앱 종료") },
            text = { Text("정말 앱을 종료하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = { (context as? Activity)?.finish() }
                ) {
                    Text("종료", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("취소") }
            }
        )
    }

    // 새 대화 시작 팝업
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewChatDialog = false
                newTopic = ""
                inputError = null // 닫을 때 에러 초기화
            },
            title = { Text("새 대화 시작") },
            text = {
                Column {
                    Text("대화의 주제를 입력해주세요.\n(예: 진로, 연애, 학업)")
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newTopic,
                        onValueChange = {
                            newTopic = it
                            // [추가] 금지된 문자 검사 로직
                            val forbiddenChars = listOf('/', '?', '&', '#', '\\')
                            if (it.any { char -> char in forbiddenChars }) {
                                inputError = "특수문자(/, ?, &, #, \\)는 사용할 수 없습니다."
                            } else {
                                inputError = null
                            }
                        },
                        placeholder = { Text("주제 입력") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = inputError != null, // 에러 시 빨간 테두리
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                            autoCorrectEnabled = false
                        )
                    )

                    // [추가] 에러 메시지 출력
                    if (inputError != null) {
                        Text(
                            text = inputError!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("가장 가까운 고민 유형을 골라주세요.")
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedCategory)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        selectedCategory = category
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTopic.isNotBlank() && inputError == null) {
                            showNewChatDialog = false
                            onNavigateToChat(newTopic, selectedCategory)
                            newTopic = ""
                            selectedCategory = categories[0]
                        }
                    },
                    // [추가] 에러가 있거나 비어있으면 버튼 비활성화
                    enabled = newTopic.isNotBlank() && inputError == null
                ) { Text("시작") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewChatDialog = false
                    newTopic = ""
                    inputError = null
                }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isSelectionMode) {
                        Text("${uiState.selectedSessions.size}개 선택됨", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("${userId}님, 안녕하세요", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.deleteSelectedSessions() }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Red)
                        }
                        IconButton(onClick = { viewModel.clearSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "취소")
                        }
                    } else {
                        IconButton(onClick = { viewModel.logout(onLogout) }) {
                            Icon(
                                // AutoMirrored 안에 있는 아이콘을 사용
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "로그아웃"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = { showNewChatDialog = true },
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "새 대화")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.sessions.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("아직 대화 기록이 없어요.", color = Color.Gray)
                    Text("오른쪽 아래 + 버튼을 눌러 시작해보세요!", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "최근 대화",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(uiState.sessions) { session ->
                        val isSelected = uiState.selectedSessions.contains(session.sessionId)

                        ChatListItem(
                            sessionId = session.sessionId,
                            lastMessage = session.lastMessage,
                            date = formatTime(session.timestamp),
                            isSelected = isSelected,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleSelection(session.sessionId)
                                } else {
                                    // [수정] session.category를 넘겨줍니다! (기존엔 null이었음)
                                    onNavigateToChat(session.sessionId, session.category)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSelectionMode(session.sessionId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    sessionId: String,
    lastMessage: String,
    date: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF6200EE) else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    val backgroundColor = if (isSelected) Color(0xFFF3E5F5) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF6200EE) else Color(0xFFE8EAF6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color(0xFF3F51B5)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = sessionId,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Text(
                        text = date,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastMessage,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

fun formatTime(timestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formatter = SimpleDateFormat("MM/dd", Locale.getDefault())
        val date = parser.parse(timestamp)
        formatter.format(date ?: return timestamp)
    } catch (e: Exception) {
        timestamp
    }
}