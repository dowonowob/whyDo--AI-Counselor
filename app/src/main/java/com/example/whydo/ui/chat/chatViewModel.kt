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
    val sessionId: String? = null // 👈 [Gemini 수정] 세션 ID를 UiState로 관리
)

class ChatViewModel : ViewModel() {

    private val mediaPlayer = MediaPlayer()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // ⛔️ [Gemini 수정] 하드코딩된 세션 ID 제거
    // private val currentSessionId = "도도"
    private val currentUserId = "default_user"

    // ⛔️ [Gemini 수정] init 블록에서 startConversation() 호출 제거
    // init { startConversation() }

    /**
     * [Gemini 수정] 닉네임(세션 ID)이 설정되면 대화를 시작합니다.
     */
    fun setSessionIdAndStart(sessionId: String) {
        if (sessionId.isBlank()) return // 닉네임이 비어있으면 무시
        _uiState.update { it.copy(sessionId = sessionId) }
        startConversation(sessionId) // 닉네임을 가지고 대화 시작
    }

    /**
     * [Gemini 수정] 세션 ID를 인자로 받도록 변경
     */
    private fun startConversation(sessionId: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val request = ServerChatRequest(
                    message = "__INIT__",
                    userId = currentUserId,
                    sessionId = sessionId // 👈 전달받은 sessionId 사용
                )
                val response = ApiClient.whyDoApiService.postChatMessage(request)
                val aiResponseContent = response.response
                val aiMessage = ChatMessage(Author.AI, aiResponseContent, R.drawable.profile_ai, "은도")
                _uiState.update { it.copy(isLoading = false, messages = it.messages + aiMessage) }
                val cleanText = cleanTextForTts(aiResponseContent)
                speak(cleanText)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to start conversation: ${e.message}")
                val errorText = "연결에 실패했습니다. 서버를 확인해주세요."
                val errorMessage = ChatMessage(Author.AI, errorText, R.drawable.profile_ai, "은도")
                _uiState.update { it.copy(isLoading = false, messages = it.messages + errorMessage) }
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

    fun sendMessage(userMessageText: String) {
        // [Gemini 수정] 세션 ID가 없으면 메시지 전송 불가
        val sessionId = _uiState.value.sessionId
        if (_uiState.value.isLoading || userMessageText.isBlank() || sessionId == null) return

        val userMessage = ChatMessage(Author.USER, userMessageText, R.drawable.profile_user, "나")
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }

        viewModelScope.launch {
            try {
                // [Gemini 수정] 세션 ID를 UiState에서 가져오기
                val request = ServerChatRequest(
                    message = userMessageText,
                    userId = currentUserId,
                    sessionId = sessionId
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

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
    }
}