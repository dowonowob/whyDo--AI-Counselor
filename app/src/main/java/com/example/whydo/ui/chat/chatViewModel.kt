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
// 수정된 network import
import com.example.whydo.data.network.ApiClient
import com.example.whydo.data.network.AudioConfig
import com.example.whydo.data.network.ServerChatRequest
import com.example.whydo.data.network.TtsInput
import com.example.whydo.data.network.TtsRequest
import com.example.whydo.data.network.VoiceSelection
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

    // ⛔️ [삭제됨] ⛔️
    // 시스템 프롬프트는 이제 Python 서버가 관리합니다.
    // private val systemMessage = Message(...)

    /**
     * TTS로 읽기 전에 텍스트에서 불필요한 기호들을 제거합니다.
     */
    private fun cleanTextForTts(text: String): String {
        var cleanText = text.replace(Regex("[*]"), "")
        cleanText = cleanText.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
        return cleanText.trim()
    }

    /**
     * Google Cloud TTS API를 호출하여 음성을 재생합니다.
     */
    private fun speak(text: String) {
        viewModelScope.launch {
            try {
                val request = TtsRequest(
                    input = TtsInput(text),
                    voice = VoiceSelection(languageCode = "ko-KR", name = "ko-KR-Standard-B"),
                    audioConfig = AudioConfig(audioEncoding = "MP3")
                )
                // GCP API 키는 Google TTS를 위해 여전히 필요합니다.
                val response = ApiClient.gcpTtsApiService.synthesize(BuildConfig.GCP_API_KEY, request)
                val audioBytes = Base64.decode(response.audioContent, Base64.DEFAULT)
                playAudio(audioBytes)
            } catch (e: Exception) {
                Log.e("TTS", "Google TTS API Failed: ${e.message}")
            }
        }
    }

    /**
     * 전달받은 오디오 데이터를 MediaPlayer로 재생합니다.
     */
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

    /**
     * 긴 텍스트를 약 150자 근처의 문장 단위로 자릅니다.
     */
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

    /**
     * (수정됨) OpenAI가 아닌, 우리 Python 서버로 메시지를 전송합니다.
     */
    // /ui/chat/ChatViewModel.kt 의 sendMessage 함수

    fun sendMessage(userMessageText: String) {
        if (_uiState.value.isLoading || userMessageText.isBlank()) return

        val userMessage = ChatMessage(Author.USER, userMessageText, R.drawable.profile_user, "John")
        _uiState.update { it.copy(messages = it.messages + userMessage, isLoading = true) }

        viewModelScope.launch {
            try {
                // [수정됨] 서버에 보낼 요청 객체 생성 (새로운 형식)
                val request = ServerChatRequest(
                    message = userMessageText,
                    userId = "default_user", // 임시 사용자 ID
                    sessionId = "default_session" // 임시 세션 ID
                )

                // [수정됨] 우리 whyDoApiService 호출 (함수 자체는 동일)
                val response = ApiClient.whyDoApiService.postChatMessage(request)

                // [수정됨] 서버 응답에서 텍스트 추출 (새로운 형식)
                val aiResponseContent = response.response // .content가 아니라 .response

                // --- (이하 로직은 기존과 동일) ---

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