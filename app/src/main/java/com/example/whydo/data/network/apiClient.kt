// /data/network/ApiClient.kt

package com.example.whydo.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://api.openai.com/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val openAiApiService: OpenAiApiService by lazy {
        retrofit.create(OpenAiApiService::class.java)
    }
}