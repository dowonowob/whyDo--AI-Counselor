// /ui/chat/ChatViewModel.kt

package com.example.whydo.ui.chat

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whydo.BuildConfig
import com.example.whydo.R
import com.example.whydo.data.model.Author
import com.example.whydo.data.model.ChatMessage
import com.example.whydo.data.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val sessionId: String? = null
)

class ChatViewModel : ViewModel() {

    private val mediaPlayer = MediaPlayer()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentUserId = ""
    private var currentCategory: String? = null

    fun setInfoAndStart(userId: String, sessionId: String, category: String? = null) {
        if (this.currentUserId == userId && _uiState.value.sessionId == sessionId && _uiState.value.messages.isNotEmpty()) return

        this.currentUserId = userId
        this.currentCategory = category

        _uiState.update { it.copy(sessionId = sessionId, messages = emptyList()) } // 메시지 초기화
        loadHistoryOrStart(sessionId)
    }

    /**
     * 기록이 있으면 불러오고, 없으면 새로 시작
     */
    private fun loadHistoryOrStart(sessionId: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // [수정] userId 인자 제거! (토큰이 있으니까)
                val historyResponse = ApiClient.whyDoApiService.getChatHistory(sessionId)

                if (historyResponse.messages.isNotEmpty()) {
                    // 2. 기록이 있으면 화면에 표시
                    val chatMessages = historyResponse.messages.map { item ->
                        val author = if (item.role == "user") Author.USER else Author.AI
                        val profile = if (author == Author.USER) R.drawable.profile_user else R.drawable.profile_ai
                        val name = if (author == Author.USER) "나" else "은도"
                        ChatMessage(author, item.content, profile, name)
                    }
                    _uiState.update { it.copy(isLoading = false, messages = chatMessages) }
                } else {
                    // 3. 기록이 없으면(새 대화면) __INIT__ 전송
                    startNewConversation(sessionId)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "History load failed: ${e.message}")
                // 에러 나면 그냥 새 대화처럼 시작 시도
                startNewConversation(sessionId)
            }
        }
    }

    private suspend fun startNewConversation(sessionId: String) {
        try {
            val request = ServerChatRequest(
                message = "__INIT__",
                userId = currentUserId,
                sessionId = sessionId,
                category = currentCategory
            )
            val response = ApiClient.whyDoApiService.postChatMessage(request)
            val aiResponseContent = response.response

            val aiMessage = ChatMessage(Author.AI, aiResponseContent, R.drawable.profile_ai, "은도")
            _uiState.update { it.copy(isLoading = false, messages = it.messages + aiMessage) }

            val cleanText = cleanTextForTts(aiResponseContent)
            speak(cleanText)

        } catch (e: Exception) {
            Log.e("ChatViewModel", "Start conversation failed: ${e.message}")
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun sendMessage(userMessageText: String) {
        val sessionId = _uiState.value.sessionId
        if (_uiState.value.isLoading || userMessageText.isBlank() || sessionId == null) return

        val userMessage = ChatMessage(Author.USER, userMessageText, R.drawable.profile_user, "나")
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }

        viewModelScope.launch {
            try {
                val request = ServerChatRequest(
                    message = userMessageText,
                    userId = currentUserId,
                    sessionId = sessionId,
                    category = currentCategory
                )

                val response = ApiClient.whyDoApiService.postChatMessage(request)
                val aiResponseContent = response.response

                val chunks = splitResponseIntoChunks(aiResponseContent)
                _uiState.update { it.copy(isLoading = false) }

                chunks.forEach { chunk ->
                    val aiMessage = ChatMessage(Author.AI, chunk, R.drawable.profile_ai, "은도")
                    _uiState.update { it.copy(messages = it.messages + aiMessage) }
                    delay(500)
                }

                val cleanText = cleanTextForTts(aiResponseContent)
                speak(cleanText)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "API Call Failed: ${e.message}")
                val errorText = "오류가 발생했습니다. 서버가 켜져있는지 확인해주세요."
                speak(errorText)
                val errorMessage = ChatMessage(Author.AI, errorText, R.drawable.profile_ai, "은도")
                _uiState.update { it.copy(messages = it.messages + errorMessage, isLoading = false) }
            }
        }
    }

    private fun cleanTextForTts(text: String): String {
        var cleanText = text.replace(Regex("[*]"), "")
        cleanText = cleanText.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
        return cleanText.trim()
    }

    private fun speak(text: String) {
        viewModelScope.launch {
            try {
                var ssmlText = text.replace(Regex("([.?!])\\s*")) {
                    "${it.groupValues[1]} <break time='600ms'/> "
                }
                ssmlText = "<speak>$ssmlText</speak>"

                val request = TtsRequest(
                    input = TtsInput(ssml = ssmlText),
                    voice = VoiceSelection(languageCode = "ko-KR", name = "ko-KR-Standard-B"),
                    audioConfig = AudioConfig(
                        audioEncoding = "MP3",
                        speakingRate = 1.25
                    )
                )

                val response = ApiClient.gcpTtsApiService.synthesize(BuildConfig.GCP_API_KEY, request)
                val audioBytes = Base64.decode(response.audioContent, Base64.DEFAULT)
                playAudio(audioBytes)
            } catch (e: Exception) {
                Log.e("TTS", "Google TTS API Failed: ${e.message}")
            }
        }
    }

    private fun playAudio(audioBytes: ByteArray) {
        try {
            val tempAudioFile = File.createTempFile("tts_audio", "mp3")
            tempAudioFile.writeBytes(audioBytes)

            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.reset()
            mediaPlayer.setDataSource(tempAudioFile.absolutePath)
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            mediaPlayer.prepare()
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e("MediaPlayer", "Failed to play audio: ${e.message}")
        }
    }

    private fun splitResponseIntoChunks(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var remainingText = text
        val maxLength = 150
        while (remainingText.length > maxLength) {
            var splitIndex = remainingText.lastIndexOf('.', startIndex = maxLength)
            if (splitIndex == -1) splitIndex = remainingText.lastIndexOf('?', startIndex = maxLength)
            if (splitIndex == -1) splitIndex = remainingText.lastIndexOf('!', startIndex = maxLength)
            if (splitIndex == -1) splitIndex = maxLength

            if (splitIndex + 1 >= remainingText.length) {
                chunks.add(remainingText.trim())
                remainingText = ""
                break
            }
            chunks.add(remainingText.substring(0, splitIndex + 1).trim())
            remainingText = remainingText.substring(splitIndex + 1).trim()
        }
        if (remainingText.isNotBlank()) {
            chunks.add(remainingText)
        }
        return chunks
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
    }
}