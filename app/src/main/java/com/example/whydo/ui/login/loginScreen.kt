package com.example.whydo.ui.login

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // 화면 진입 시 에러 초기화
    LaunchedEffect(Unit) {
        viewModel.clearError()
    }

    // 뒤로가기 버튼 감지 -> 팝업 띄우기
    BackHandler {
        showExitDialog = true
    }

    // 종료 확인 팝업 UI
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("앱 종료") },
            text = { Text("정말 앱을 종료하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text("종료", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f)) // 상단 여백

            // 1. 앱 이름
            Text(
                text = "whyDo?",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(48.dp))

            // 2. 헤더
            Text(
                text = "계정 로그인",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "서비스를 이용하려면 로그인이 필요합니다",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 3. 아이디 입력
            SimpleTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "아이디",
                icon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. 비밀번호 입력
            SimpleTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호",
                icon = Icons.Default.Lock,
                isPassword = true
            )

            // 에러 메시지
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage!!,
                    color = if (uiState.errorMessage!!.contains("성공")) Color.Blue else Color.Red,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. 로그인 버튼
            Button(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(username, password, onLoginSuccess)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("로그인", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. 회원가입 버튼
            TextButton(
                onClick = onNavigateToSignup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("계정이 없나요? 회원가입 하기", color = Color.Gray)
            }

            // 소셜 로그인 및 구분선 제거됨

            Spacer(modifier = Modifier.weight(1f)) // 하단 여백

            // 7. 하단 약관
            Text(
                text = "계속을 클릭하면 당사의 서비스 이용 약관 및 개인정보 처리방침에\n동의하는 것으로 간주됩니다.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.LightGray) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.LightGray,
            cursorColor = Color.Black,
            focusedLabelColor = Color.Black,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Next,
            autoCorrectEnabled = false
        )
    )
}