package com.metalens.app.conversation

import java.util.UUID

data class ConversationUiState(
    val status: ConversationStatus = ConversationStatus.Idle,
    val isUserSpeaking: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val recentError: String? = null,
)

enum class ChatRole {
    User,
    Ai,
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
)

enum class ConversationStatus {
    Idle,
    Connecting,
    Listening,
    Speaking,
    Error,
}

