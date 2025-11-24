// /data/network/WhyDoApiService.kt

package com.example.whydo.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// --- [인증] 로그인/회원가입용 데이터 모델 ---
data class AuthRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)

// --- [채팅] 메시지 전송용 데이터 모델 ---
data class ServerChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("category") val category: String? = null
)

data class ServerChatResponse(
    @SerializedName("response") val response: String
)

// --- [목록] 세션 목록 조회용 데이터 모델 ---
data class SessionListResponse(
    val sessions: List<SessionSummary>
)

data class SessionSummary(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("last_message") val lastMessage: String,
    @SerializedName("timestamp") val timestamp: String
)

// --- [기록] 채팅 기록 조회용 데이터 모델 ---
data class ChatHistoryResponse(
    val messages: List<ChatMessageItem>
)

data class ChatMessageItem(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String,
    @SerializedName("timestamp") val timestamp: String
)

// --- API 명세서 (메뉴판) ---
interface WhyDoApiService {
    // [인증] 회원가입
    @POST("signup")
    suspend fun signup(@Body request: AuthRequest): ServerChatResponse

    // [인증] 로그인
    @POST("login")
    suspend fun login(@Body request: AuthRequest): TokenResponse

    // [채팅] 메시지 보내기
    @POST("chat")
    suspend fun postChatMessage(
        @Body request: ServerChatRequest
    ): ServerChatResponse

    // [목록] 대화 목록 가져오기
    @GET("sessions")
    suspend fun getSessions(): SessionListResponse

    // [기록] 특정 채팅방 기록 가져오기
    @GET("history/{session_id}")
    suspend fun getChatHistory(
        @Path("session_id") sessionId: String
    ): ChatHistoryResponse
}