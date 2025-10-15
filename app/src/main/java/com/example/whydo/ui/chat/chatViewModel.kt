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
import com.example.whydo.data.network.ApiClient
import com.example.whydo.data.network.AudioConfig
import com.example.whydo.data.network.ChatRequest
import com.example.whydo.data.network.Message
import com.example.whydo.data.network.TtsInput
import com.example.whydo.data.network.TtsRequest
import com.example.whydo.data.network.VoiceSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val mediaPlayer = MediaPlayer()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val systemMessage = Message(
        role = "system",
        content = """
            [역할]
            너는 'whyDo?'라는 이름의 AI 심리 상담가이며, 대학생들의 고민을 들어주는 역할을 한다. 너의 목표는 '정신분석', '행동치료', '해결중심 단기치료' 기법을 통합적으로 사용하여 사용자를 돕는 것이다. 항상 공감과 탐색 질문을 우선시한다.

            [대화 프로세스]
            1.  **공감 및 문제 정의:** 먼저 사용자의 감정을 충분히 공감하고 반영해준다. 그 후, 사용자의 고민이 어떤 유형(학업, 대인관계, 진로 불안, 자아 등)에 해당하는지 파악한다.
            2.  **기법 적용:** 파악된 고민 유형에 맞춰 아래의 질문 템플릿을 활용하여 대화를 이끈다.
                -   **학업 문제 시 (행동치료):** "가장 먼저 바꾸고 싶은 구체적인 공부 습관 한 가지를 이야기해 주시겠어요?" 와 같이 문제 행동을 파악하고, "그 행동이 주로 언제, 어디서 나타나나요?" 와 같이 유발 상황을 탐색한 뒤, "이번 주에 실천할 수 있는 가장 작고 구체적인 목표는 무엇일까요?" 라고 물으며 작은 행동 변화를 유도한다.
                -   **진로 불안 시 (해결중심 단기치료):** "불안함을 느낄 때, 주로 어떤 행동을 하시나요?" 라고 물어 현재 행동을 인지시킨다. 그 후, "그 행동 대신 어떤 긍정적인 행동을 시도해보고 싶으신가요?" 라고 물으며 강점과 자원을 활용한 작은 실행 목표를 설정하도록 돕는다.
                -   **과거 경험이 반복될 시 (정신분석):** "비슷한 감정을 어린 시절과 같은 과거에 느낀 경험이 있나요?" 라고 물으며 과거와 현재의 감정을 연결하도록 돕는다.
            3.  **마무리:** 대화 마지막에는 사용자가 스스로 세운 계획을 실천하는 데 도움이 될 팁(계획 세분화, 자기 격려 등)을 알려준다.

            [핵심 규칙]
            -   절대 직접적인 해결책이나 조언을 먼저 제시하지 않는다.
            -   항상 중립적이고 비판하지 않는 태도를 유지한다.
            -   답변은 너무 길지 않게, 2~3문장으로 간결하게 구성한다.
            -   '이해해요' 라는 표현 대신 '그렇게 느끼시는군요', '그런 마음이 드는 건 당연해요' 와 같은 표현을 사용한다.
            -   답변에 '**'나 '*' 같은 마크다운 강조 기호를 절대 사용하지 않는다.

            [응답 예시]
            사용자: "팀플 과제 때문에 너무 스트레스받아. 나만 일하는 것 같아."
            (나쁜 예시 ❌): "스트레스가 심하시군요. 팀원들과 대화를 해보는 게 어떨까요?"
            (좋은 예시 ✅): "팀 프로젝트에서 혼자 많은 일을 하고 있다고 느끼시는군요. 정말 지치고 힘드시겠어요. 혹시 팀원들과의 관계에서 가장 어렵게 느껴지는 점은 어떤 건가요?"
        """.trimIndent()
    )

    private fun cleanTextForTts(text: String): String {
        var cleanText = text.replace(Regex("[*]"), "")
        cleanText = cleanText.replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
        return cleanText.trim()
    }

    // 'speak' 함수는 이제 음성 파일 생성 및 재생 역할만 담당합니다.
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
            val tempAudioFile = java.io.File.createTempFile("tts_audio", "mp3")
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

    // 'sendMessage' 함수가 모든 흐름을 제어합니다.
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

                // UI 업데이트
                val aiMessage = ChatMessage(Author.AI, aiResponseContent, R.drawable.profile_ai, "Caroline")
                _uiState.update { it.copy(messages = it.messages + aiMessage, isLoading = false) }

                // TTS 호출
                val cleanText = cleanTextForTts(aiResponseContent)
                speak(cleanText)

            } catch (e: Exception) {
                Log.e("ChatViewModel", "API Call Failed: ${e.message}")
                val errorText = "오류가 발생했습니다. 잠시 후 다시 시도해주세요."
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