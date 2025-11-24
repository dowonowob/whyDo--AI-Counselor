// /data/local/TokenManager.kt

package com.example.whydo.data.local

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREF_NAME = "auth_prefs"
    private const val KEY_TOKEN = "access_token"
    private lateinit var prefs: SharedPreferences

    // 앱 시작 시 초기화
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // 토큰 저장
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    // 토큰 가져오기
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // 토큰 삭제 (로그아웃 시)
    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }
}