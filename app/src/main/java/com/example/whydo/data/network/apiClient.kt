// /data/network/ApiClient.kt

package com.example.whydo.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    // 1. 서버 주소를 OpenAI가 아닌 우리 로컬 서버로 변경
    // 127.0.0.1 (localhost)는 에뮬레이터 자신을 가리킵니다.
    // 에뮬레이터에서 개발용 PC의 localhost에 접근하려면 10.0.2.2를 사용해야 합니다.
    private const val WHYDO_SERVER_BASE_URL = "http://10.0.2.2:8000/"

    // 2. 우리 서버와 통신할 Retrofit 인스턴스
    private val whyDoServerRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(WHYDO_SERVER_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // 3. 우리 서버를 위한 ApiService
    val whyDoApiService: WhyDoApiService by lazy {
        whyDoServerRetrofit.create(WhyDoApiService::class.java)
    }

    // --- Google Cloud TTS 설정 (이 부분은 그대로 유지합니다) ---
    private const val GCP_TTS_BASE_URL = "https://texttospeech.googleapis.com/"
    private val gcpTtsRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(GCP_TTS_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val gcpTtsApiService: GcpTtsApiService by lazy {
        gcpTtsRetrofit.create(GcpTtsApiService::class.java)
    }
}