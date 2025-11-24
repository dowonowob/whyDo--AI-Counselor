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

                // [수정] 받은 토큰을 안전하게 저장!
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

    fun signup(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            try {
                val request = AuthRequest(username, password)
                ApiClient.whyDoApiService.signup(request)
                _uiState.value = LoginUiState(isLoading = false, errorMessage = "가입 성공! 로그인해주세요.")
            } catch (e: HttpException) {
                val errorMsg = if (e.code() == 400) "이미 존재하는 아이디입니다." else "가입 실패: ${e.message}"
                _uiState.value = LoginUiState(isLoading = false, errorMessage = errorMsg)
            } catch (e: Exception) {
                _uiState.value = LoginUiState(isLoading = false, errorMessage = "연결 오류: ${e.message}")
            }
        }
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)