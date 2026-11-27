package com.nexus.ai.data.remote

import com.nexus.ai.data.remote.dto.ChatCompletionRequest
import com.nexus.ai.data.remote.dto.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.POST

/** Base URL: https://router.bangunweb.com/v1/ (lihat ProviderConfig). Auth ditambahkan oleh AuthInterceptor. */
interface NexusChatApi {
    @POST("chat/completions")
    suspend fun chatCompletions(@Body body: ChatCompletionRequest): ChatCompletionResponse
}
