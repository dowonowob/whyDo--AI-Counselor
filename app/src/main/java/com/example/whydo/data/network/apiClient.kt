// /data/network/ApiClient.kt

package com.example.whydo.data.network

import com.example.whydo.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val WHYDO_SERVER_BASE_URL = "http://10.0.2.2:8000/"

    // [추가] 모든 요청에 토큰을 자동으로 붙여주는 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        // 토큰이 있으면 헤더에 추가
        TokenManager.getToken()?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }

        chain.proceed(builder.build())
    }

    // [추가] 인터셉터가 장착된 OkHttpClient
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    // 우리 서버용 Retrofit (client 적용)
    private val whyDoServerRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(WHYDO_SERVER_BASE_URL)
        .client(client) // 👈 클라이언트 장착!
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val whyDoApiService: WhyDoApiService by lazy {
        whyDoServerRetrofit.create(WhyDoApiService::class.java)
    }

    // --- Google Cloud TTS 설정 ---
    private const val GCP_TTS_BASE_URL = "https://texttospeech.googleapis.com/"
    private val gcpTtsRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(GCP_TTS_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val gcpTtsApiService: GcpTtsApiService by lazy {
        gcpTtsRetrofit.create(GcpTtsApiService::class.java)
    }
}