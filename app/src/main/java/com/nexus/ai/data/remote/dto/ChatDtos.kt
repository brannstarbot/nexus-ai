package com.nexus.ai.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Format chat completions (kompatibel dengan endpoint /v1/chat/completions AI Router BangunWeb). */

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
) {
    @Serializable
    data class Choice(
        val message: ChatMessageDto? = null,
        @SerialName("finish_reason") val finishReason: String? = null,
    )

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens") val promptTokens: Int? = null,
        @SerialName("completion_tokens") val completionTokens: Int? = null,
        @SerialName("total_tokens") val totalTokens: Int? = null,
    )
}
