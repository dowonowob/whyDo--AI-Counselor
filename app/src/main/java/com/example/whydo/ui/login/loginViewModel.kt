// /ui/login/LoginViewModel.kt

package com.example.whydo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whydo.data.local.TokenManager
import com.example.whydo.data.network.ApiClient
import com.example.whydo.data.network.AuthRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String, onLoginSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            try {
                val request = AuthRequest(username, password)
                val response = ApiClient.whyDoApiService.login(request)

                TokenManager.saveToken(response.accessToken)

                _uiState.value = LoginUiState(isLoading = false)
                onLoginSuccess(username)

            } catch (e: HttpException) {
                val errorMsg = if (e.code() == 401) "아이디 또는 비밀번호가 틀렸습니다." else "로그인 실패: ${e.message}"
                _uiState.value = LoginUiState(isLoading = false, errorMessage = errorMsg)
            } catch (e: Exception) {
                _uiState.value = LoginUiState(isLoading = false, errorMessage = "연결 오류: ${e.message}")
            }
        }
    }

    // [수정] 회원가입 성공 시 실행할 행동(onSignupSuccess)을 매개변수로 받음
    fun signup(username: String, password: String, onSignupSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            try {
                val request = AuthRequest(username, password)
                ApiClient.whyDoApiService.signup(request)

                _uiState.value = LoginUiState(isLoading = false)
                // 가입 성공 시 콜백 실행 (화면 이동 등)
                onSignupSuccess()

            } catch (e: HttpException) {
                val errorMsg = if (e.code() == 400) "이미 존재하는 아이디입니다." else "가입 실패: ${e.message}"
                _uiState.value = LoginUiState(isLoading = false, errorMessage = errorMsg)
            } catch (e: Exception) {
                _uiState.value = LoginUiState(isLoading = false, errorMessage = "연결 오류: ${e.message}")
            }
        }
    }

    // 에러 메시지 초기화용 (화면 이동 시 잔여 에러 제거)
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)