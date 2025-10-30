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
    val isLoading: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val mediaPlayer = MediaPlayer()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // --- [Gemini 수정] 세션 ID를 변수로 관리 ---
    // (지금은 하드코딩, 나중에 닉네임 입력 UI에서 이 값을 받아오도록 수정)
    private val currentSessionId = "도도" // "도도", "은도" 등 닉네임으로 변경 가능
    private val currentUserId = "default_user"

    // --- [Gemini 수정] ViewModel이 생성될 때 대화를 시작하도록 init 블록 추가 ---
    init {
        startConversation()
    }

    /**
     * [Gemini 추가] 앱 시작 시, 또는 새 세션 시작 시 자기소개 메시지를 받아옵니다.
     */
    private fun startConversation() {
        // 이미 대화가 시작되었다면 실행하지 않음
        if (_uiState.value.messages.isNotEmpty()) return

        _uiState.update { it.copy(isLoading = true) } // 로딩 시작

        viewModelScope.launch {
            try {
                // "__INIT__"이라는 특수 메시지를 보내 서버에 대화 시작을 알림
                val request = ServerChatRequest(
                    message = "__INIT__",
                    userId = currentUserId,
                    sessionId = currentSessionId
                )

                // 서버 호출
                val response = ApiClient.whyDoApiService.postChatMessage(request)
                val aiResponseContent = response.response

                // 자기소개 메시지를 화면에 추가
                val aiMessage = ChatMessage(Author.AI, aiResponseContent, R.drawable.profile_ai, "Caroline")
                _uiState.update { it.copy(isLoading = false, messages = it.messages + aiMessage) }

                // 자기소개 메시지 음성 재생
                val cleanText = cleanTextForTts(aiResponseContent)
                speak(cleanText)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to start conversation: ${e.message}")
                val errorText = "연결에 실패했습니다. 서버를 확인해주세요."
                val errorMessage = ChatMessage(Author.AI, errorText, R.drawable.profile_ai, "Caroline")
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
                val request = TtsRequest(
                    input = TtsInput(text),
                    voice = VoiceSelection(languageCode = "ko-KR", name = "ko-KR-Standard-B"),
                    audioConfig = AudioConfig(audioEncoding = "MP3")
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
        if (_uiState.value.isLoading || userMessageText.isBlank()) return

        val userMessage = ChatMessage(Author.USER, userMessageText, R.drawable.profile_user, "John")
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }

        viewModelScope.launch {
            try {
                // [Gemini 수정] 세션 ID를 변수에서 가져오도록 수정
                val request = ServerChatRequest(
                    message = userMessageText,
                    userId = currentUserId,
                    sessionId = currentSessionId
                )

                val response = ApiClient.whyDoApiService.postChatMessage(request)
                val aiResponseContent = response.response

                val chunks = splitResponseIntoChunks(aiResponseContent)
                _uiState.update { it.copy(isLoading = false) }

                chunks.forEach { chunk ->
                    val aiMessage = ChatMessage(Author.AI, chunk, R.drawable.profile_ai, "Caroline")
                    _uiState.update { it.copy(messages = it.messages + aiMessage) }
                    delay(500)
                }

                val cleanText = cleanTextForTts(aiResponseContent)
                speak(cleanText)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "API Call Failed: ${e.message}")
                val errorText = "오류가 발생했습니다. 서버가 켜져있는지 확인해주세요."
                speak(errorText)
                val errorMessage = ChatMessage(Author.AI, errorText, R.drawable.profile_ai, "Caroline")
                _uiState.update { it.copy(messages = it.messages + errorMessage, isLoading = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
    }
}
