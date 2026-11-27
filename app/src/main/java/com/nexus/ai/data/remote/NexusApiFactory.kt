package com.nexus.ai.data.remote

import com.nexus.ai.data.config.ProviderConfig
import com.nexus.ai.data.security.ApiKeyStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NexusApiFactory {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun createChatApi(config: ProviderConfig, keyStore: ApiKeyStore, debug: Boolean): NexusChatApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(keyStore))
            .connectTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.timeoutSeconds, TimeUnit.SECONDS)
            .apply {
                if (debug) {
                    // Jangan pernah log header Authorization; hanya body/metadata.
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NexusChatApi::class.java)
    }
}
