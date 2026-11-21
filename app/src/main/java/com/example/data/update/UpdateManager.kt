package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GithubReleaseDto(
    @Json(name = "tag_name") val tagName: String,
    val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    val assets: List<GithubAssetDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GithubAssetDto(
    val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0L
)

data class AppUpdate(
    val tag: String,
    val versionName: String,
    val notes: String?,
    val releaseUrl: String?,
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long
)

/**
 * Checks the latest GitHub release of this fan app and installs the APK asset.
 * Silent installation is not possible on a normal Android device, so the flow is:
 * check -> download -> system install confirmation (one tap).
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val REPOSITORY = "neponum/monster-siren-fan-app"
        private const val RELEASES_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"
        private const val USER_AGENT = "MonsterSirenFanApp/${BuildConfig.VERSION_NAME}"

        fun canInstall(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

        fun unknownSourcesIntent(context: Context): Intent =
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun check(): Result<AppUpdate?> = withContext(Dispatchers.IO) {
        runCatching { fetchLatest() }
    }

    private fun fetchLatest(): AppUpdate? {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val json = response.body?.string().orEmpty()
            val release = moshi.adapter(GithubReleaseDto::class.java)
                .fromJson(json)
                ?.takeIf { it.tagName.isNotBlank() }
                ?: return null

            if (!isNewer(release.tagName)) return null

            val asset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return null

            return AppUpdate(
                tag = release.tagName,
                versionName = release.tagName.removePrefix("v").trim(),
                notes = release.body?.takeIf { it.isNotBlank() },
                releaseUrl = release.htmlUrl,
                downloadUrl = asset.browserDownloadUrl,
                fileName = asset.name,
                sizeBytes = asset.size
            )
        }
    }

    private fun isNewer(tag: String): Boolean {
        val latest = tag.removePrefix("v").trim()
        val current = BuildConfig.VERSION_NAME
        if (latest.equals(current, ignoreCase = true)) return false

        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        if (latestParts.isEmpty() || currentParts.isEmpty()) return true

        for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
            val a = latestParts.getOrElse(index) { 0 }
            val b = currentParts.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun download(update: AppUpdate, onProgress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            val target = File(context.cacheDir, "msr-update.apk")
            if (target.exists()) target.delete()

            val request = Request.Builder()
                .url(update.downloadUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code} при загрузке обновления")
                val body = response.body ?: error("Пустой ответ при загрузке обновления")
                val total = body.contentLength()
                body.byteStream().use { input ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
            }
            target
        }

    fun install(apk: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
