package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DownloadStatus
import com.example.data.local.MonsterSirenDao
import com.example.data.remote.MonsterSirenApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class OfflineDownloadManager(
    private val context: Context,
    private val api: MonsterSirenApi,
    private val okHttpClient: OkHttpClient,
    private val dao: MonsterSirenDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadSemaphore = Semaphore(permits = 2)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val audioDir: File by lazy {
        File(context.filesDir, "msr_audio").apply { mkdirs() }
    }

    private val coverDir: File by lazy {
        File(context.filesDir, "msr_covers").apply { mkdirs() }
    }

    init {
        scope.launch {
            dao.resetStuckDownloads()
        }
    }

    fun downloadSong(songCid: String) {
        if (activeJobs[songCid]?.isActive == true) return

        val job = scope.launch {
            try {
                dao.updateDownloadProgress(songCid, DownloadStatus.DOWNLOADING, 0)
                downloadSemaphore.withPermit {
                    performSongDownload(songCid)
                }
            } catch (ce: CancellationException) {
                dao.updateDownloadProgress(songCid, DownloadStatus.NOT_DOWNLOADED, 0)
                throw ce
            } catch (e: Exception) {
                Log.e(TAG, "Failed downloading song $songCid", e)
                dao.updateDownloadProgress(songCid, DownloadStatus.FAILED, 0)
            } finally {
                activeJobs.remove(songCid)
            }
        }
        activeJobs[songCid] = job
    }

    fun downloadAlbum(albumCid: String) {
        scope.launch {
            // Ensure album details/tracks are loaded if possible
            try {
                val detailResp = api.getAlbumDetail(albumCid)
                val detail = detailResp.data
                if (detail != null) {
                    val existingAlbum = dao.getAlbumById(albumCid)
                    if (existingAlbum != null) {
                        dao.upsertAlbum(
                            existingAlbum.copy(
                                intro = detail.intro.orEmpty(),
                                belong = detail.belong.orEmpty(),
                                coverUrl = detail.coverUrl ?: existingAlbum.coverUrl,
                                coverDeUrl = detail.coverDeUrl ?: existingAlbum.coverDeUrl,
                                detailsLoaded = true
                            )
                        )
                    }
                    // Cache album cover locally for offline browsing
                    ensureAlbumCoverCached(albumCid, detail.coverUrl ?: existingAlbum?.coverUrl)

                    val songRefs = detail.songs.orEmpty()
                    if (songRefs.isNotEmpty()) {
                        val existingSongs = dao.getSongsByAlbumSnapshot(albumCid).associateBy { it.cid }
                        val merged = songRefs.mapIndexed { idx, ref ->
                            val existing = existingSongs[ref.cid]
                            if (existing != null) {
                                existing.copy(
                                    name = ref.name,
                                    artistsJoined = ref.artistes?.takeIf { it.isNotEmpty() }?.joinToString(" / ")
                                        ?: existing.artistsJoined,
                                    trackIndex = idx
                                )
                            } else {
                                com.example.data.local.SongEntity(
                                    cid = ref.cid,
                                    albumCid = albumCid,
                                    name = ref.name,
                                    artistsJoined = ref.artistes?.takeIf { it.isNotEmpty() }?.joinToString(" / ")
                                        ?: "塞壬唱片-MSR",
                                    trackIndex = idx
                                )
                            }
                        }
                        dao.upsertSongs(merged)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Album detail prefetch skipped for $albumCid: ${e.message}")
            }

            val songs = dao.getSongsByAlbumSnapshot(albumCid)
            songs.forEach { song ->
                val alreadyExists = song.downloadStatus == DownloadStatus.DOWNLOADED &&
                    !song.localAudioPath.isNullOrBlank() &&
                    File(song.localAudioPath).exists()
                if (!alreadyExists) {
                    downloadSong(song.cid)
                }
            }
        }
    }

    fun cancelDownload(songCid: String) {
        activeJobs.remove(songCid)?.cancel()
        scope.launch {
            dao.updateDownloadProgress(songCid, DownloadStatus.NOT_DOWNLOADED, 0)
            cleanupTempFiles(songCid)
        }
    }

    suspend fun deleteSongDownload(songCid: String) = withContext(Dispatchers.IO) {
        activeJobs.remove(songCid)?.cancel()
        val song = dao.getSongById(songCid)
        song?.localAudioPath?.let { path ->
            runCatching { File(path).delete() }
        }
        cleanupTempFiles(songCid)
        dao.clearSongDownload(songCid)
    }

    suspend fun deleteAlbumDownloads(albumCid: String) = withContext(Dispatchers.IO) {
        val songs = dao.getSongsByAlbumSnapshot(albumCid)
        songs.forEach { song ->
            activeJobs.remove(song.cid)?.cancel()
            song.localAudioPath?.let { path -> runCatching { File(path).delete() } }
            cleanupTempFiles(song.cid)
            dao.clearSongDownload(song.cid)
        }
    }

    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        val downloaded = dao.getDownloadedSongsSnapshot()
        downloaded.forEach { song ->
            song.localAudioPath?.let { path -> runCatching { File(path).delete() } }
            dao.clearSongDownload(song.cid)
        }
        audioDir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    suspend fun ensureAlbumCoverCached(albumCid: String, coverUrl: String?): String? =
        withContext(Dispatchers.IO) {
            if (coverUrl.isNullOrBlank()) return@withContext null
            val existingAlbum = dao.getAlbumById(albumCid)
            val existingPath = existingAlbum?.localCoverPath
            if (!existingPath.isNullOrBlank() && File(existingPath).exists()) {
                return@withContext existingPath
            }

            val ext = guessImageExtension(coverUrl)
            val destFile = File(coverDir, "${albumCid}.$ext")
            val tmpFile = File(coverDir, "${albumCid}.$ext.tmp")
            return@withContext try {
                val request = Request.Builder().url(coverUrl).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body ?: return@withContext null
                    FileOutputStream(tmpFile).use { output ->
                        body.byteStream().copyTo(output)
                    }
                }
                if (tmpFile.exists() && tmpFile.length() > 0) {
                    if (destFile.exists()) destFile.delete()
                    tmpFile.renameTo(destFile)
                    dao.updateAlbumLocalCover(albumCid, destFile.absolutePath)
                    destFile.absolutePath
                } else {
                    tmpFile.delete()
                    null
                }
            } catch (e: Exception) {
                tmpFile.delete()
                Log.w(TAG, "Cover cache failed for $albumCid: ${e.message}")
                null
            }
        }

    private suspend fun performSongDownload(songCid: String) {
        val existingSong = dao.getSongById(songCid)
            ?: throw IllegalStateException("Song $songCid not found in catalog")

        // 1. Fetch fresh song detail for direct audio & lyrics URLs
        var sourceUrl = existingSong.sourceUrl
        var lyricUrl = existingSong.lyricUrl
        var lyricsText = existingSong.lyricsContent

        if (sourceUrl.isNullOrBlank() || (lyricUrl == null && lyricsText == null)) {
            val detailResp = api.getSongDetail(songCid)
            val detail = detailResp.data
            if (detail != null) {
                sourceUrl = detail.sourceUrl ?: sourceUrl
                lyricUrl = detail.lyricUrl ?: lyricUrl
            }
        }

        if (sourceUrl.isNullOrBlank()) {
            throw IllegalStateException("Missing audio sourceUrl for $songCid")
        }

        // 2. Download LRC lyrics if available
        if (!lyricUrl.isNullOrBlank() && lyricsText.isNullOrBlank()) {
            lyricsText = fetchTextUrl(lyricUrl)
        }

        // 3. Cache album cover for full offline experience
        val album = dao.getAlbumById(existingSong.albumCid)
        if (album != null) {
            ensureAlbumCoverCached(album.cid, album.coverUrl)
        }

        // 4. Stream audio file to internal storage with progress reporting
        val ext = guessAudioExtension(sourceUrl)
        val finalFile = File(audioDir, "${songCid}.$ext")
        val tempFile = File(audioDir, "${songCid}.$ext.tmp")

        val request = Request.Builder().url(sourceUrl).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} downloading audio")
            }
            val body = response.body ?: throw IllegalStateException("Empty audio response body")
            val contentLength = body.contentLength()
            var bytesCopied = 0L
            var lastProgress = -1

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (contentLength > 0) {
                            val pct = ((bytesCopied * 100L) / contentLength).toInt().coerceIn(0, 99)
                            if (pct != lastProgress && (pct - lastProgress >= 3 || pct == 99)) {
                                lastProgress = pct
                                dao.updateDownloadProgress(songCid, DownloadStatus.DOWNLOADING, pct)
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            dao.updateSongDownloadResult(
                songCid = songCid,
                sourceUrl = sourceUrl,
                lyricUrl = lyricUrl,
                lyricsContent = lyricsText,
                localAudioPath = finalFile.absolutePath,
                status = DownloadStatus.DOWNLOADED,
                progress = 100,
                fileSizeBytes = finalFile.length(),
                downloadedAt = System.currentTimeMillis()
            )
        }
    }

    suspend fun fetchTextUrl(url: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val req = Request.Builder().url(url).build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed fetching LRC from $url: ${e.message}")
            null
        }
    }

    private fun cleanupTempFiles(songCid: String) {
        audioDir.listFiles { file -> file.name.startsWith("$songCid.") && file.name.endsWith(".tmp") }
            ?.forEach { runCatching { it.delete() } }
    }

    private fun guessAudioExtension(url: String): String {
        val clean = url.substringBefore('?').lowercase()
        return when {
            clean.endsWith(".wav") -> "wav"
            clean.endsWith(".mp3") -> "mp3"
            clean.endsWith(".flac") -> "flac"
            clean.endsWith(".m4a") -> "m4a"
            clean.endsWith(".ogg") -> "ogg"
            else -> "mp3"
        }
    }

    private fun guessImageExtension(url: String): String {
        val clean = url.substringBefore('?').lowercase()
        return when {
            clean.endsWith(".png") -> "png"
            clean.endsWith(".webp") -> "webp"
            else -> "jpg"
        }
    }

    companion object {
        private const val TAG = "OfflineDownloadManager"
    }
}
