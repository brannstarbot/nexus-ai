package com.nexus.ai.di

import android.content.Context
import com.nexus.ai.BuildConfig
import com.nexus.ai.data.config.ProviderConfig
import com.nexus.ai.data.remote.NexusApiFactory
import com.nexus.ai.data.remote.NexusChatApi
import com.nexus.ai.data.repository.ChatRepository
import com.nexus.ai.data.security.ApiKeyStore
import com.nexus.ai.data.settings.AppSettingsStore
import kotlinx.coroutines.flow.first

/**
 * Dependency container manual (tanpa framework DI).
 *
 * Catatan: base URL router dipakai langsung dari perangkat (lihat README, penyimpangan
 * untuk pemakaian pribadi). BACKEND_BASE_URL dibiarkan untuk jalur backend proxy nanti.
 */
class AppContainer(private val context: Context) {

    val defaultBackendUrl: String = BuildConfig.BACKEND_BASE_URL

    val appContext: Context = context.applicationContext

    val apiKeyStore: ApiKeyStore by lazy { ApiKeyStore(context) }
    val settingsStore: AppSettingsStore by lazy { AppSettingsStore(context) }

    private val providerConfig = ProviderConfig(
        baseUrl = ROUTER_BASE_URL,
        chatPath = "chat/completions",
        timeoutSeconds = 90,
    )

    val chatApi: NexusChatApi by lazy {
        NexusApiFactory.createChatApi(providerConfig, apiKeyStore, debug = BuildConfig.DEBUG)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(chatApi) { settingsStore.settings.first() }
    }

    private companion object {
        const val ROUTER_BASE_URL = "https://router.bangunweb.com/v1/"
    }
}
