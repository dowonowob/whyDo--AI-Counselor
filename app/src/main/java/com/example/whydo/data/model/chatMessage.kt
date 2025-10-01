package com.example.whydo.data.model

import androidx.annotation.DrawableRes

/**
 * 화자를 구분하기 위한 Enum 클래스
 */
enum class Author {
    USER, AI
}

/**
 * 하나의 메시지를 표현하는 데이터 클래스
 * @param author 메시지를 보낸 화자 (USER 또는 AI)
 * @param content 메시지 내용
 * @param authorImageResId 화자의 프로필 이미지에 대한 drawable 리소스 ID
 * @param authorName 화자의 이름
 */
data class ChatMessage(
    val author: Author,
    val content: String,
    @DrawableRes val authorImageResId: Int,
    val authorName: String
)