// /data/network/WhyDoApiService.kt

package com.example.whydo.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 우리 Python 서버와 통신하기 위한 DTO
 */

// 서버로 보낼 데이터 (main.py에서 요구하는 형식과 일치)
data class ServerChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("session_id") val sessionId: String
)

// 서버로부터 받을 데이터 (main.py의 `return schemas.ChatResponse(response=...)`와 일치)
data class ServerChatResponse(
    @SerializedName("response") val response: String
)

/**
 * 우리 whyDo Python 서버와 통신하는 방법을 정의한 인터페이스
 */
interface WhyDoApiService {
    @POST("chat")
    suspend fun postChatMessage(
        @Body request: ServerChatRequest
    ): ServerChatResponse
}