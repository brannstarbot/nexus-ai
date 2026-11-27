package com.nexus.ai.data.remote

import com.nexus.ai.data.security.ApiKeyStore
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Menambahkan header Authorization dari key yang tersimpan di perangkat.
 * Dibaca setiap request, sehingga perubahan key di Settings langsung berlaku.
 */
class AuthInterceptor(private val keyStore: ApiKeyStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = keyStore.getKey()
            ?: throw IOException("API key belum diatur. Masukkan key di Settings.")
        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $key")
            .build()
        return chain.proceed(request)
    }
}
