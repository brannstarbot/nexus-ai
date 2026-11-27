# NEXUS AI

Aplikasi Android native (Kotlin + Jetpack Compose) untuk chat AI dengan fitur tambahan game catur.

> **Status: belum diverifikasi.** Sudah ada:
> - **Settings:** input API key (tersimpan terenkripsi di perangkat), model, batas token, tema, dan tes API.
> - **Chat:** bubble pesan, input, indikator mengetik, penanganan error, salin pesan, dan chat baru.
>   Riwayat masih di memori; belum disimpan.
>
> Belum ada: streaming, render Markdown dan syntax highlighting, riwayat tersimpan (Room), layar
> History, dan Chess. Kode belum dikompilasi dan belum diuji ke endpoint BangunWeb.

## Requirements

- Android Studio Ladybug (2024.2) atau lebih baru
- JDK 17
- Android SDK 35
- minSdk 26

## Build

1. Buka folder `NexusAI/` di Android Studio.
2. Salin `local.properties.example` menjadi `local.properties` lalu isi `sdk.dir` dan `nexus.backend.url`.
3. Jalankan **Sync Project with Gradle Files**.
4. Jalankan dari terminal:

```bash
./gradlew :app:assembleDebug
```

## Konfigurasi API

Aturan dasar: key **tidak pernah** ditaruh di source code, `local.properties`, `BuildConfig`, atau APK.

**Penyimpangan untuk pemakaian pribadi:** atas permintaan pemilik, key dapat dimasukkan lewat
Settings dan disimpan terenkripsi di perangkat (`ApiKeyStore`, EncryptedSharedPreferences dengan
kunci di Android Keystore). Ini tidak memakai backend proxy. Risikonya: siapa pun yang bisa
membuka perangkat yang sudah di-root atau mengambil key lewat jaringan dapat membaca key.
Jika aplikasi ini dibagikan ke orang lain, kembali ke backend proxy.

- `nexus.backend.url` di `local.properties` adalah base URL backend proxy. Nilai ini masuk ke
  `BuildConfig.BACKEND_BASE_URL`.
- `ProviderConfig` (`app/src/main/java/com/nexus/ai/data/config/ProviderConfig.kt`) menyimpan
  path endpoint, model, dan batas token. Path dan format request **belum dipasang** karena
  dokumentasi resmi AI Router BangunWeb belum diverifikasi. Isi `chatPath` dan `healthPath`
  setelah dokumentasi tersedia.

### Menghubungkan backend proxy

Backend proxy bertugas:
1. Menerima request dari aplikasi.
2. Menambahkan API key dari environment server, bukan dari aplikasi.
3. Meneruskan ke endpoint AI Router BangunWeb sesuai dokumentasi provider.
4. Mengembalikan respons (atau stream) ke aplikasi.

API key tidak boleh ditaruh di `local.properties`, `build.gradle`, atau source code.

## Struktur

```
app/src/main/java/com/nexus/ai/
├── MainActivity.kt
├── NexusApplication.kt
├── di/AppContainer.kt            # dependency container manual
├── data/config/ProviderConfig.kt  # konfigurasi provider (tanpa secret)
├── navigation/                    # NexusApp, NexusDestination (4 tab)
└── ui/theme/                      # Color, Type, Shape, Theme (dark-first, aksen crimson)
```

Rencana lanjutan: `data/local` (Room), `data/remote` (Retrofit/Ktor + streaming),
`domain/chess` (aturan catur), `feature/chat`, `feature/chess`, `feature/history`,
`feature/settings`.
