package com.nexus.ai.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexus.ai.data.remote.dto.ChatMessageDto
import com.nexus.ai.data.repository.ChatResult
import com.nexus.ai.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class Role { USER, ASSISTANT }

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val isError: Boolean = false,
)

data class ChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val input: String = "",
    val loading: Boolean = false,
)

class ChatViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun onInputChanged(value: String) = _state.update { it.copy(input = value) }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.loading) return

        val userMsg = UiMessage(role = Role.USER, text = text)
        _state.update {
            it.copy(messages = it.messages + userMsg, input = "", loading = true)
        }

        viewModelScope.launch {
            // Riwayat dikirim tanpa pesan error agar tidak ikut ke konteks model.
            val history = _state.value.messages
                .filter { !it.isError }
                .map { ChatMessageDto(role = if (it.role == Role.USER) "user" else "assistant", content = it.text) }

            val reply = when (val result = container.chatRepository.complete(history)) {
                is ChatResult.Success -> UiMessage(role = Role.ASSISTANT, text = result.text)
                is ChatResult.Failure -> UiMessage(role = Role.ASSISTANT, text = result.message, isError = true)
            }
            _state.update { it.copy(messages = it.messages + reply, loading = false) }
        }
    }

    /** Mulai percakapan baru. Riwayat lama belum disimpan (menunggu implementasi Room). */
    fun newChat() {
        if (_state.value.loading) return
        _state.update { ChatUiState() }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { ChatViewModel(container) }
        }
    }
}
