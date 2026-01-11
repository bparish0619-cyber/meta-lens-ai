package com.metalens.app.conversation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide conversation state that can outlive the UI.
 *
 * The Foreground Service owns the actual session (audio + websocket) and updates this state.
 * UI and ViewModels observe this StateFlow.
 */
object ConversationRuntime {
    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    internal fun update(reducer: (ConversationUiState) -> ConversationUiState) {
        _uiState.value = reducer(_uiState.value)
    }

    internal fun reset() {
        _uiState.value = ConversationUiState()
    }
}

