package com.nexus.ai.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexus.ai.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_settings")

data class AppSettings(
    val model: String,
    val maxOutputTokens: Int,
    val themeMode: ThemeMode,
)

/** Pengaturan non-rahasia. API key TIDAK disimpan di sini (lihat ApiKeyStore). */
class AppSettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            model = p[KEY_MODEL] ?: DEFAULT_MODEL,
            maxOutputTokens = p[KEY_MAX_TOKENS] ?: DEFAULT_MAX_TOKENS,
            themeMode = runCatching { ThemeMode.valueOf(p[KEY_THEME] ?: "") }
                .getOrDefault(ThemeMode.DARK),
        )
    }

    suspend fun setModel(model: String) {
        context.dataStore.edit { it[KEY_MODEL] = model.trim() }
    }

    suspend fun setMaxOutputTokens(tokens: Int) {
        context.dataStore.edit { it[KEY_MAX_TOKENS] = tokens.coerceIn(64, 8192) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    companion object {
        const val DEFAULT_MODEL = "deepseek-v4-flash"
        const val DEFAULT_MAX_TOKENS = 1024
        private val KEY_MODEL = stringPreferencesKey("model")
        private val KEY_MAX_TOKENS = intPreferencesKey("max_output_tokens")
        private val KEY_THEME = stringPreferencesKey("theme_mode")
    }
}
