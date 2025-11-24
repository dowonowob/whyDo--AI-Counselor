// /ui/login/LoginScreen.kt

package com.example.whydo.ui.login

import androidx.compose.foundation.background
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
    viewModel: LoginViewModel = viewModel() // 뷰모델 연결
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 뷰모델의 상태(로딩, 에러 메시지 등)를 구독
    val uiState by viewModel.uiState.collectAsState()

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

            // 2. 헤더 텍스트
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

            // 3. 입력 필드 (아이디)
            SimpleTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = "아이디",
                icon = Icons.Default.Email
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. 입력 필드 (비밀번호)
            SimpleTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호",
                icon = Icons.Default.Lock,
                isPassword = true
            )

            // [에러 메시지 표시]
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

            // 6. 회원가입 버튼 (텍스트 버튼)
            TextButton(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        viewModel.signup(username, password)
                    } else {
                        // 아이디/비번 입력 안하고 누르면 안내 메시지 표시 (임시로 에러메시지 필드 활용)
                        // 실제로는 Toast 메시지나 별도 상태로 처리하는 게 더 좋음
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("계정이 없나요? 입력한 정보로 회원가입 하기", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. "--- 또는 ---" 분리선
            OrDivider()

            Spacer(modifier = Modifier.height(24.dp))

            // 8. 소셜 로그인 버튼 (디자인용)
            SocialLoginButton(
                text = "Google 계정으로 계속하기",
                iconColor = Color.Red
            )
            Spacer(modifier = Modifier.height(12.dp))
            SocialLoginButton(
                text = "Apple 계정으로 계속하기",
                iconColor = Color.Black
            )

            Spacer(modifier = Modifier.weight(1f)) // 하단 여백

            // 9. 하단 약관
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

// --- 재사용 컴포넌트들 ---

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
            focusedLabelColor = Color.Black
        ),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Next,
            autoCorrect = false
        )
    )
}

@Composable
fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFEEEEEE), thickness = 1.dp)
        Text(
            text = "또는",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFEEEEEE), thickness = 1.dp)
    }
}

@Composable
fun SocialLoginButton(
    text: String,
    iconColor: Color
) {
    Button(
        onClick = { /* 소셜 로그인 로직 (미구현) */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5F5F5)),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(iconColor, shape = RoundedCornerShape(50))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}