// /ui/chat/ChatViewModel.kt

package com.example.whydo.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whydo.BuildConfig
import com.example.whydo.R
import com.example.whydo.data.model.Author
import com.example.whydo.data.model.ChatMessage
import com.example.whydo.data.network.ApiClient
import com.example.whydo.data.network.ChatRequest
import com.example.whydo.data.network.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 화면의 모든 상태를 담는 데이터 클래스
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

class ChatViewModel : ViewModel() {

    // 1. UI 상태를 위한 StateFlow 선언
    // _uiState: ViewModel 내부에서만 수정 가능한 '비공개' 상태
    private val _uiState = MutableStateFlow(ChatUiState())
    // uiState: UI에서는 읽기만 가능한 '공개' 상태
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // 2. 시스템 프롬프트 정의 (AI의 역할 부여)
    private val systemMessage = Message(
        role = "system",
        content = """
            너는 'whyDo?'라는 이름의 공감 능력이 뛰어난 대학생 심리 상담가야. 
            항상 해결중심 단기치료(SFBT) 원칙에 따라 대화해야 해.
            절대 사용자를 비난하거나 섣불리 충고하지 마.
            사용자의 감정을 먼저 인정하고, 그들이 스스로의 강점과 해결책을 찾도록 긍정적인 질문을 던져줘.
            답변은 항상 따뜻하고 친근한 말투의 한국어 존댓말로 해줘.
        """.trimIndent()
    )

    // 3. 사용자가 메시지를 보냈을 때 호출될 함수
    fun sendMessage(userMessageText: String) {
        // 로딩 중이거나 입력이 비어있으면 아무것도 하지 않음
        if (_uiState.value.isLoading || userMessageText.isBlank()) {
            return
        }

        // 4. 사용자 메시지를 UI에 바로 추가하고, 로딩 상태로 변경
        val userMessage = ChatMessage(
            author = Author.USER,
            content = userMessageText,
            authorImageResId = R.drawable.profile_user, // 실제 이미지 리소스 사용
            authorName = "John"
        )
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage,
                isLoading = true
            )
        }

        // 5. API 호출
        viewModelScope.launch {
            try {
                // 전체 대화 기록을 API에 보낼 형식으로 변환
                val history = (_uiState.value.messages).map {
                    Message(
                        role = if (it.author == Author.USER) "user" else "assistant",
                        content = it.content
                    )
                }

                val request = ChatRequest(messages = listOf(systemMessage) + history)
                val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"

                val response = ApiClient.openAiApiService.getChatCompletion(apiKey, request)
                val aiResponseContent = response.choices.firstOrNull()?.message?.content ?: "죄송해요, 답변을 생성할 수 없어요."

                // 6. AI 응답을 UI에 추가하고, 로딩 상태 해제
                val aiMessage = ChatMessage(
                    author = Author.AI,
                    content = aiResponseContent,
                    authorImageResId = R.drawable.profile_ai, // 실제 이미지 리소스 사용
                    authorName = "Caroline"
                )
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + aiMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "API Call Failed: ${e.message}")
                // 7. 에러 발생 시 UI에 에러 메시지 표시하고, 로딩 상태 해제
                val errorMessage = ChatMessage(
                    author = Author.AI,
                    content = "오류가 발생했습니다: ${e.message}",
                    authorImageResId = R.drawable.profile_ai,
                    authorName = "Caroline"
                )
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + errorMessage,
                        isLoading = false
                    )
                }
            }
        }
    }
}