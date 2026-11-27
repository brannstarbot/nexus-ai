package com.nexus.ai.data.config

import kotlinx.serialization.Serializable

/**
 * Konfigurasi provider AI. Aplikasi TIDAK memegang API key.
 * Semua request dikirim ke backend proxy NEXUS, yang menyimpan secret dan
 * meneruskan ke AI Router BangunWeb.
 *
 * Path & format request sengaja dapat diubah di Settings, karena dokumentasi
 * resmi provider harus diverifikasi sebelum dipasang sebagai default.
 */
@Serializable
data class ProviderConfig(
    /** Base URL backend proxy, mis. https://api.anda.com/ (dari BuildConfig secara default). */
    val baseUrl: String,
    /** Path endpoint chat, relatif terhadap baseUrl. Diisi dari dokumentasi provider. */
    val chatPath: String = "",
    /** Path endpoint untuk tes koneksi/health check. */
    val healthPath: String = "",
    /** Nama model yang dipilih pengguna. */
    val model: String = "",
    /** Batas token output yang diminta. */
    val maxOutputTokens: Int = 1024,
    val timeoutSeconds: Long = 60,
)
