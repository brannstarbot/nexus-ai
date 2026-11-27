package com.nexus.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nexus.ai.di.AppContainer
import com.nexus.ai.data.repository.ChatResult
import com.nexus.ai.data.remote.dto.ChatMessageDto
import com.nexus.ai.data.settings.AppSettingsStore
import com.nexus.ai.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val hasKey: Boolean = false,
    val maskedKey: String? = null,
    val keyInput: String = "",
    val model: String = AppSettingsStore.DEFAULT_MODEL,
    val maxOutputTokens: Int = 1024,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val testing: Boolean = false,
    val testResult: String? = null,
    val message: String? = null,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        refreshKeyInfo()
        viewModelScope.launch {
            container.settingsStore.settings.collect { s ->
                _state.update {
                    it.copy(
                        model = s.model,
                        maxOutputTokens = s.maxOutputTokens,
                        themeMode = s.themeMode,
                    )
                }
            }
        }
    }

    private fun refreshKeyInfo() {
        val store = container.apiKeyStore
        _state.update { it.copy(hasKey = store.hasKey(), maskedKey = store.maskedKey()) }
    }

    fun onKeyInputChanged(value: String) = _state.update { it.copy(keyInput = value) }

    fun saveKey() {
        val input = _state.value.keyInput
        if (input.isBlank()) {
            _state.update { it.copy(message = "Key tidak boleh kosong.") }
            return
        }
        container.apiKeyStore.saveKey(input)
        _state.update { it.copy(keyInput = "", message = "Key disimpan terenkripsi di perangkat.") }
        refreshKeyInfo()
    }

    fun clearKey() {
        container.apiKeyStore.clearKey()
        _state.update { it.copy(message = "Key dihapus.", testResult = null) }
        refreshKeyInfo()
    }

    fun onModelChanged(model: String) {
        viewModelScope.launch { container.settingsStore.setModel(model) }
    }

    fun onMaxTokensChanged(tokens: Int) {
        viewModelScope.launch { container.settingsStore.setMaxOutputTokens(tokens) }
    }

    fun onThemeChanged(mode: ThemeMode) {
        viewModelScope.launch { container.settingsStore.setThemeMode(mode) }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    /** Tes koneksi: kirim pesan singkat dan tampilkan hasilnya. */
    fun testConnection() {
        if (!container.apiKeyStore.hasKey()) {
            _state.update { it.copy(testResult = "Masukkan API key dulu.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(testing = true, testResult = null) }
            val result = container.chatRepository.complete(
                listOf(ChatMessageDto(role = "user", content = "Balas dengan satu kata: OK"))
            )
            val text = when (result) {
                is ChatResult.Success -> "Berhasil: ${result.text.take(120)}"
                is ChatResult.Failure -> "Gagal: ${result.message}"
            }
            _state.update { it.copy(testing = false, testResult = text) }
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
