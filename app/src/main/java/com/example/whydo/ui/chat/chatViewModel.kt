// /ui/chat/ChatViewModel.kt

package com.example.whydo.ui.chat

import android.content.Context
import android.speech.tts.TextToSpeech
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
import java.util.Locale

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

class ChatViewModel : ViewModel(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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

    fun initTts(context: Context) {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language is not supported!")
            }
        } else {
            Log.e("TTS", "TTS Initialization Failed!")
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    fun sendMessage(userMessageText: String) {
        if (_uiState.value.isLoading || userMessageText.isBlank()) return

        val userMessage = ChatMessage(Author.USER, userMessageText, R.drawable.profile_user, "John")
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }

        viewModelScope.launch {
            try {
                val history = _uiState.value.messages.map { Message(if (it.author == Author.USER) "user" else "assistant", it.content) }
                val request = ChatRequest(messages = listOf(systemMessage) + history)
                val apiKey = "Bearer ${BuildConfig.OPENAI_API_KEY}"
                val response = ApiClient.openAiApiService.getChatCompletion(apiKey, request)
                val aiResponseContent = response.choices.firstOrNull()?.message?.content ?: "죄송해요, 답변을 생성할 수 없어요."

                speak(aiResponseContent) // AI 답변 음성 출력

                val aiMessage = ChatMessage(Author.AI, aiResponseContent, R.drawable.profile_ai, "Caroline")
                _uiState.update { it.copy(messages = it.messages + aiMessage, isLoading = false) }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "API Call Failed: ${e.message}")
                val errorText = "오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                speak(errorText) // 에러 메시지 음성 출력
                val errorMessage = ChatMessage(Author.AI, errorText, R.drawable.profile_ai, "Caroline")
                _uiState.update { it.copy(messages = it.messages + errorMessage, isLoading = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}