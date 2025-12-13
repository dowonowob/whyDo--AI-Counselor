// /ui/chat/ChatListViewModel.kt

package com.example.whydo.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whydo.data.local.TokenManager
import com.example.whydo.data.network.ApiClient
import com.example.whydo.data.network.DeleteSessionRequest
import com.example.whydo.data.network.SessionSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListUiState(
    val sessions: List<SessionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false, // [추가] 다중 선택 모드인지?
    val selectedSessions: Set<String> = emptySet() // [추가] 선택된 세션 ID들
)

class ChatListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    // 목록 불러오기 (기존과 동일)
    fun loadSessions() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val response = ApiClient.whyDoApiService.getSessions()
                _uiState.update {
                    it.copy(sessions = response.sessions, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Load failed: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // [추가] 로그아웃
    fun logout(onLogoutSuccess: () -> Unit) {
        TokenManager.clearToken() // 토큰 삭제
        onLogoutSuccess() // 화면 이동
    }

    // [추가] 롱클릭 시 선택 모드 진입
    fun toggleSelectionMode(sessionId: String) {
        val currentMode = _uiState.value.isSelectionMode
        if (!currentMode) {
            // 모드 시작하면서 해당 아이템 선택
            _uiState.update {
                it.copy(isSelectionMode = true, selectedSessions = setOf(sessionId))
            }
        } else {
            toggleSelection(sessionId)
        }
    }

    // [추가] 아이템 선택/해제 토글
    fun toggleSelection(sessionId: String) {
        _uiState.update { state ->
            val newSelection = state.selectedSessions.toMutableSet()
            if (newSelection.contains(sessionId)) {
                newSelection.remove(sessionId)
            } else {
                newSelection.add(sessionId)
            }

            // 선택된 게 하나도 없으면 선택 모드 종료
            val isStillSelectionMode = newSelection.isNotEmpty()
            state.copy(
                selectedSessions = newSelection,
                isSelectionMode = isStillSelectionMode
            )
        }
    }

    // [추가] 선택된 세션들 영구 삭제
    fun deleteSelectedSessions() {
        val targets = _uiState.value.selectedSessions.toList()
        if (targets.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // 서버에 삭제 요청
                ApiClient.whyDoApiService.deleteSessions(DeleteSessionRequest(targets))

                // 성공하면 선택 모드 끄고 목록 새로고침
                _uiState.update { it.copy(isSelectionMode = false, selectedSessions = emptySet()) }
                loadSessions()

            } catch (e: Exception) {
                Log.e("ChatListViewModel", "Delete failed: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // [추가] 선택 모드 취소
    fun clearSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedSessions = emptySet()) }
    }
}