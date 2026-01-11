package com.metalens.app.conversation

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.core.content.ContextCompat

class ConversationViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val uiState: StateFlow<ConversationUiState> = ConversationRuntime.uiState

    fun start() {
        val intent =
            Intent(getApplication(), ConversationForegroundService::class.java).apply {
                action = ConversationForegroundService.ACTION_START
            }
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun stop() {
        val intent =
            Intent(getApplication(), ConversationForegroundService::class.java).apply {
                action = ConversationForegroundService.ACTION_STOP
            }
        getApplication<Application>().startService(intent)
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        val intent =
            Intent(getApplication(), ConversationForegroundService::class.java).apply {
                action = ConversationForegroundService.ACTION_SEND_TEXT
                putExtra(ConversationForegroundService.EXTRA_TEXT, trimmed)
            }
        getApplication<Application>().startService(intent)
    }
}

