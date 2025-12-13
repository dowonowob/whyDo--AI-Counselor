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
    val sessionId: String? = null,
    val isMuted: Boolean = false // [추가] 음소거 상태
)

class ChatViewModel : ViewModel() {

    private val mediaPlayer = MediaPlayer()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentUserId = ""
    private var currentCategory: String? = null

    // [추가] 음소거 토글 함수
    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }

        // 음소거를 켰는데 이미 재생 중이라면 즉시 중단
        if (_uiState.value.isMuted && mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset() // 다음 재생을 위해 리셋
        }
    }

    fun setInfoAndStart(userId: String, sessionId: String, category: String? = null) {
        this.currentUserId = userId
        this.currentCategory = category

        _uiState.update { it.copy(sessionId = sessionId, messages = emptyList()) }
        loadHistoryOrStart(sessionId)
    }

    private fun loadHistoryOrStart(sessionId: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val historyResponse = ApiClient.whyDoApiService.getChatHistory(sessionId)

                if (historyResponse.messages.isNotEmpty()) {
                    val chatMessages = historyResponse.messages.map { item ->
                        val author = if (item.role == "user") Author.USER else Author.AI
                        val profile = if (author == Author.USER) R.drawable.profile_user else R.drawable.profile_ai
                        val name = if (author == Author.USER) "나" else "은도"
                        ChatMessage(author, item.content, profile, name)
                    }
                    _uiState.update { it.copy(isLoading = false, messages = chatMessages) }
                } else {
                    startNewConversation(sessionId)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "History load failed: ${e.message}")
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

    // [수정된 로직] 안내 멘트가 바뀌어도 유연하게 대처
    private fun cleanTextForTts(text: String): String {
        // 1. 마크다운 및 이모지 제거
        var cleanText = text.replace(Regex("[*]"), "")
        cleanText = cleanText.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")

        // 2. 긴 안내 메시지 필터링
        // (팁, 안내, 도움말 등의 단어가 포함된 긴 문장이면 필터링 발동)
        val guideKeywords = listOf("팁을 드릴게요", "안내를 드릴게요", "도움말을 준비했어요", "더 도움이 되는 대화를 위해")

        for (keyword in guideKeywords) {
            if (cleanText.contains(keyword)) {
                // [전략] 키워드가 포함된 문단 앞까지(인사) + 맨 마지막 문단(질문)만 추출

                // 1) 인사말: 키워드가 나오기 전의 마지막 줄바꿈(\n) 앞까지 자름
                val introPart = cleanText.substringBefore(keyword)
                val introIndex = introPart.lastIndexOf('\n')
                val intro = if (introIndex != -1) introPart.substring(0, introIndex).trim() else introPart.trim()

                // 2) 맺음말: 전체 텍스트의 마지막 줄바꿈(\n) 뒤 (보통 마지막 질문임)
                val outroIndex = cleanText.lastIndexOf('\n')
                val outro = if (outroIndex != -1) cleanText.substring(outroIndex + 1).trim() else ""

                return "$intro $outro"
            }
        }

        return cleanText.trim()
    }

    private fun speak(text: String) {
        // [수정] 음소거 상태면 재생하지 않음
        if (_uiState.value.isMuted) return

        viewModelScope.launch {
            try {
                // API 호출 전 한 번 더 체크 (그 사이에 눌렀을 수도 있으니까)
                if (_uiState.value.isMuted) return@launch

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

                // 재생 직전 최후의 체크
                if (!_uiState.value.isMuted) {
                    playAudio(audioBytes)
                }
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