// /ui/chat/ChatListViewModel.kt

package com.example.whydo.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whydo.data.network.ApiClient
import com.example.whydo.data.network.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListUiState(
    val sessions: List<SessionSummary> = emptyList(),
    val isLoading: Boolean = false
)

class ChatListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    // [수정] userId 매개변수 제거 (토큰 사용)
    fun loadSessions() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // [수정] 인자 없이 호출!
                val response = ApiClient.whyDoApiService.getSessions()

                _uiState.update {
                    it.copy(
                        sessions = response.sessions,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "목록 불러오기 실패: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}