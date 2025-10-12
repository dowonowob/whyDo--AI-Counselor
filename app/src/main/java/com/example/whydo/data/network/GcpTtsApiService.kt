// /data/network/GcpTtsApiService.kt

package com.example.whydo.data.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

// Google TTS API에 보낼 요청 형식
data class TtsRequest(
    val input: TtsInput,
    val voice: VoiceSelection,
    val audioConfig: AudioConfig
)

data class TtsInput(val text: String)
data class VoiceSelection(val languageCode: String, val name: String)
data class AudioConfig(val audioEncoding: String = "MP3")

// Google TTS API로부터 받을 응답 형식
data class TtsResponse(
    val audioContent: String // Base64로 인코딩된 오디오 데이터
)

// API 명세서
interface GcpTtsApiService {
    @POST("v1/text:synthesize")
    suspend fun synthesize(
        @Query("key") apiKey: String, // API 키를 쿼리 파라미터로 전송
        @Body request: TtsRequest
    ): TtsResponse
}