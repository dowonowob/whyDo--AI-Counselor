// /ui/login/SignupScreen.kt

package com.example.whydo.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit, // 가입 성공 시 로그인 화면으로 이동
    onBackClick: () -> Unit,     // 뒤로가기 버튼 클릭 시
    viewModel: LoginViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf(false) } // 비밀번호 불일치 여부

    val uiState by viewModel.uiState.collectAsState()

    // 화면 진입 시 기존 에러 메시지 초기화
    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("회원가입") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "새로운 계정 만들기",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "아이디와 비밀번호를 입력해주세요.",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 1. 아이디 입력
            SimpleTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "아이디",
                icon = Icons.Default.Email
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 2. 비밀번호 입력
            SimpleTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = (it != confirmPassword && confirmPassword.isNotEmpty())
                },
                placeholder = "비밀번호",
                icon = Icons.Default.Lock,
                isPassword = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 3. 비밀번호 확인 입력
            SimpleTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordError = (password != it)
                },
                placeholder = "비밀번호 확인",
                icon = Icons.Default.Check,
                isPassword = true
            )

            // 비밀번호 불일치 에러 메시지
            if (passwordError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "비밀번호가 일치하지 않습니다.",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            // 서버 에러 메시지
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 4. 가입하기 버튼
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank() && !passwordError) {
                        viewModel.signup(username, password, onSignupSuccess)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                enabled = !uiState.isLoading && username.isNotBlank() && password.isNotBlank() && !passwordError
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("가입하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}