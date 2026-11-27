package com.nexus.ai.data.repository

import com.nexus.ai.data.remote.NexusChatApi
import com.nexus.ai.data.remote.dto.ChatCompletionRequest
import com.nexus.ai.data.remote.dto.ChatMessageDto
import com.nexus.ai.data.settings.AppSettings
import retrofit2.HttpException
import java.io.IOException

/** Hasil pemanggilan AI. Error dikembalikan sebagai pesan yang bisa ditampilkan ke pengguna. */
sealed interface ChatResult {
    data class Success(val text: String, val totalTokens: Int?) : ChatResult
    data class Failure(val message: String) : ChatResult
}

class ChatRepository(
    private val api: NexusChatApi,
    private val readSettings: suspend () -> AppSettings,
) {

    suspend fun complete(messages: List<ChatMessageDto>): ChatResult {
        return try {
            val settings = readSettings()
            val response = api.chatCompletions(
                ChatCompletionRequest(
                    model = settings.model,
                    messages = messages,
                    maxTokens = settings.maxOutputTokens,
                )
            )
            val text = response.choices.firstOrNull()?.message?.content
            if (text.isNullOrBlank()) {
                ChatResult.Failure("Respons kosong dari server.")
            } else {
                ChatResult.Success(text, response.usage?.totalTokens)
            }
        } catch (e: HttpException) {
            ChatResult.Failure(httpMessage(e.code()))
        } catch (e: IOException) {
            ChatResult.Failure(e.message ?: "Gagal terhubung ke server.")
        } catch (e: Exception) {
            ChatResult.Failure("Terjadi kesalahan tidak terduga.")
        }
    }

    private fun httpMessage(code: Int): String = when (code) {
        400 -> "Permintaan tidak valid (400). Periksa nama model."
        401, 403 -> "Key ditolak server (${code}). Periksa API key di Settings."
        404 -> "Endpoint tidak ditemukan (404). Periksa konfigurasi endpoint."
        429 -> "Batas permintaan tercapai (429). Coba lagi nanti."
        in 500..599 -> "Server sedang bermasalah ($code). Coba lagi nanti."
        else -> "Permintaan gagal dengan kode $code."
    }
}
