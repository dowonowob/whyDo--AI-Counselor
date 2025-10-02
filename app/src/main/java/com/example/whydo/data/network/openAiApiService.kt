// /data/network/OpenAiApiService.kt

package com.example.whydo.data.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// GPT에 보낼 메시지 형식
data class Message(
    val role: String,
    val content: String
)

// GPT에 보낼 요청 전체 형식
data class ChatRequest(
    val model: String = "gpt-4o",
    val messages: List<Message>
)

// GPT로부터 받을 응답 형식
data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

// API 명세서 (Interface)
interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): ChatResponse
}