package com.nexus.ai.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Penyimpanan API key terenkripsi di perangkat.
 *
 * Key dimasukkan pengguna lewat layar Settings. Tidak ada key di source code, BuildConfig, atau APK.
 * Nilai dienkripsi dengan AES-256 GCM; kunci enkripsi disimpan di Android Keystore.
 *
 * Catatan: ini menyimpang dari aturan "backend proxy menyimpan secret" dan hanya
 * dipakai atas persetujuan eksplisit pemilik untuk pemakaian pribadi.
 */
class ApiKeyStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun hasKey(): Boolean = !prefs.getString(KEY_API, null).isNullOrBlank()

    /** Mengembalikan key, atau null jika belum diatur. Jangan pernah ditampilkan atau di-log utuh. */
    fun getKey(): String? = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() }

    fun saveKey(rawKey: String) {
        val trimmed = rawKey.trim()
        require(trimmed.isNotEmpty()) { "API key kosong" }
        prefs.edit().putString(KEY_API, trimmed).apply()
    }

    fun clearKey() {
        prefs.edit().remove(KEY_API).apply()
    }

    /** Tampilan aman untuk UI, misalnya "bwr_live_****abcd". */
    fun maskedKey(): String? {
        val key = getKey() ?: return null
        if (key.length <= 8) return "****"
        return key.take(8) + "****" + key.takeLast(4)
    }

    private companion object {
        const val FILE_NAME = "nexus_secure_prefs"
        const val KEY_API = "api_key"
    }
}
