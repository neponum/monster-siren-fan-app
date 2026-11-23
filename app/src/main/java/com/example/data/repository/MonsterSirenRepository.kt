package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.AlbumEntity
import com.example.data.local.AlbumWithStats
import com.example.data.local.DownloadStatus
import com.example.data.local.MonsterSirenDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.PlaylistWithCount
import com.example.data.local.SongEntity
import com.example.data.local.SongWithAlbum
import com.example.data.local.UserPreferences
import com.example.data.remote.MonsterSirenApi
import com.example.data.remote.NewsDetailDto
import com.example.data.remote.NewsSummaryDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class MonsterSirenRepository(
    private val context: Context,
    private val api: MonsterSirenApi,
    private val dao: MonsterSirenDao,
    val downloadManager: OfflineDownloadManager,
    val preferences: UserPreferences
) {

    val allAlbums: Flow<List<AlbumWithStats>> = dao.observeAllAlbumsWithStats()
    val allSongs: Flow<List<SongWithAlbum>> = dao.observeAllSongsWithAlbum()
    val downloadedSongs: Flow<List<SongWithAlbum>> = dao.observeDownloadedSongs()
    val activeDownloads: Flow<List<SongWithAlbum>> = dao.observeActiveDownloads()
    val favoriteSongs: Flow<List<SongWithAlbum>> = dao.observeFavoriteSongs()
    val totalOfflineBytes: Flow<Long> = dao.observeTotalOfflineBytes()
    val downloadedSongCount: Flow<Int> = dao.observeDownloadedSongCount()

    val playlists: Flow<List<PlaylistWithCount>> = dao.observePlaylistsWithCount()

    fun observePlaylistSongs(playlistId: Long): Flow<List<SongWithAlbum>> =
        dao.observePlaylistSongs(playlistId)

    fun observeAlbum(albumCid: String): Flow<AlbumWithStats?> = dao.observeAlbumWithStats(albumCid)

    fun observeSongsByAlbum(albumCid: String): Flow<List<SongWithAlbum>> =
        dao.observeSongsByAlbum(albumCid)

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun syncCatalog(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("Нет подключения к интернету"))
        }

        try {
            val (albumsResp, songsResp) = coroutineScope {
                val albumsDeferred = async { api.getAlbums() }
                val songsDeferred = async { api.getSongs() }
                albumsDeferred.await() to songsDeferred.await()
            }

            val remoteAlbums = albumsResp.data.orEmpty()
            val remoteSongs = songsResp.data?.list.orEmpty()

            if (remoteAlbums.isEmpty() && remoteSongs.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Пустой ответ от сервера Monster Siren"))
            }

            val existingAlbums = dao.getAllAlbumsSnapshot().associateBy { it.cid }
            val mergedAlbums = remoteAlbums.mapIndexed { index, dto ->
                val existing = existingAlbums[dto.cid]
                AlbumEntity(
                    cid = dto.cid,
                    name = dto.name,
                    intro = existing?.intro.orEmpty(),
                    belong = existing?.belong.orEmpty(),
                    coverUrl = dto.coverUrl ?: existing?.coverUrl.orEmpty(),
                    coverDeUrl = existing?.coverDeUrl.orEmpty(),
                    localCoverPath = existing?.localCoverPath,
                    artistsJoined = dto.artistes?.takeIf { it.isNotEmpty() }?.joinToString(" / ")
                        ?: existing?.artistsJoined ?: "塞壬唱片-MSR",
                    displayOrder = index,
                    detailsLoaded = existing?.detailsLoaded ?: false
                )
            }
            if (mergedAlbums.isNotEmpty()) {
                dao.upsertAlbums(mergedAlbums)
            }

            val existingSongs = dao.getAllSongsSnapshot().associateBy { it.cid }
            val perAlbumCounter = mutableMapOf<String, Int>()
            val mergedSongs = remoteSongs.map { dto ->
                val existing = existingSongs[dto.cid]
                val orderInAlbum = perAlbumCounter.getOrDefault(dto.albumCid, 0)
                perAlbumCounter[dto.albumCid] = orderInAlbum + 1

                // Verify local file still exists if marked downloaded
                val validLocalFile = existing?.localAudioPath?.takeIf { File(it).exists() }
                val cleanStatus = if (existing?.downloadStatus == DownloadStatus.DOWNLOADED && validLocalFile == null) {
                    DownloadStatus.NOT_DOWNLOADED
                } else {
                    existing?.downloadStatus ?: DownloadStatus.NOT_DOWNLOADED
                }

                SongEntity(
                    cid = dto.cid,
                    albumCid = dto.albumCid,
                    name = dto.name,
                    artistsJoined = dto.artists?.takeIf { it.isNotEmpty() }?.joinToString(" / ")
                        ?: existing?.artistsJoined ?: "塞壬唱片-MSR",
                    sourceUrl = existing?.sourceUrl,
                    lyricUrl = existing?.lyricUrl,
                    lyricsContent = existing?.lyricsContent,
                    localAudioPath = validLocalFile,
                    downloadStatus = cleanStatus,
                    downloadProgress = if (cleanStatus == DownloadStatus.DOWNLOADED) 100 else existing?.downloadProgress ?: 0,
                    fileSizeBytes = if (cleanStatus == DownloadStatus.DOWNLOADED) existing?.fileSizeBytes ?: 0L else 0L,
                    trackIndex = existing?.takeIf { it.trackIndex > 0 }?.trackIndex ?: orderInAlbum,
                    isFavorite = existing?.isFavorite ?: false,
                    downloadedAt = existing?.downloadedAt ?: 0L
                )
            }
            if (mergedSongs.isNotEmpty()) {
                dao.upsertSongs(mergedSongs)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Catalog sync failed", e)
            Result.failure(e)
        }
    }

    suspend fun loadAlbumDetail(albumCid: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            // Offline: silently rely on cached Room data
            return@withContext Result.success(Unit)
        }

        try {
            val response = api.getAlbumDetail(albumCid)
            val detail = response.data ?: return@withContext Result.success(Unit)
            val existingAlbum = dao.getAlbumById(albumCid)

            val updatedAlbum = AlbumEntity(
                cid = detail.cid,
                name = detail.name,
                intro = detail.intro.orEmpty(),
                belong = detail.belong.orEmpty(),
                coverUrl = detail.coverUrl ?: existingAlbum?.coverUrl.orEmpty(),
                coverDeUrl = detail.coverDeUrl ?: existingAlbum?.coverDeUrl.orEmpty(),
                localCoverPath = existingAlbum?.localCoverPath,
                artistsJoined = existingAlbum?.artistsJoined ?: "塞壬唱片-MSR",
                displayOrder = existingAlbum?.displayOrder ?: 9999,
                detailsLoaded = true
            )
            dao.upsertAlbum(updatedAlbum)

            val songRefs = detail.songs.orEmpty()
            if (songRefs.isNotEmpty()) {
                val existingSongs = dao.getSongsByAlbumSnapshot(albumCid).associateBy { it.cid }
                val mergedSongs = songRefs.mapIndexed { index, ref ->
                    val existing = existingSongs[ref.cid]
                    val validLocalFile = existing?.localAudioPath?.takeIf { File(it).exists() }
                    SongEntity(
                        cid = ref.cid,
                        albumCid = albumCid,
                        name = ref.name,
                        artistsJoined = ref.artistes?.takeIf { it.isNotEmpty() }?.joinToString(" / ")
                            ?: existing?.artistsJoined ?: "塞壬唱片-MSR",
                        sourceUrl = existing?.sourceUrl,
                        lyricUrl = existing?.lyricUrl,
                        lyricsContent = existing?.lyricsContent,
                        localAudioPath = validLocalFile,
                        downloadStatus = if (existing?.downloadStatus == DownloadStatus.DOWNLOADED && validLocalFile == null) {
                            DownloadStatus.NOT_DOWNLOADED
                        } else {
                            existing?.downloadStatus ?: DownloadStatus.NOT_DOWNLOADED
                        },
                        downloadProgress = existing?.downloadProgress ?: 0,
                        fileSizeBytes = existing?.fileSizeBytes ?: 0L,
                        trackIndex = index,
                        isFavorite = existing?.isFavorite ?: false,
                        downloadedAt = existing?.downloadedAt ?: 0L
                    )
                }
                dao.upsertSongs(mergedSongs)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "loadAlbumDetail failed for $albumCid: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Resolves a song for playback: prefers local offline file with ZERO network calls.
     * If not downloaded locally, fetches fresh sourceUrl + LRC lyrics from Monster Siren API.
     */
    suspend fun resolveSongForPlayback(songCid: String): ResolvedPlaybackTrack? =
        withContext(Dispatchers.IO) {
            val current = dao.getSongWithAlbum(songCid) ?: return@withContext null
            val localPath = current.song.localAudioPath
            if (!localPath.isNullOrBlank() && File(localPath).exists()) {
                return@withContext ResolvedPlaybackTrack(
                    songWithAlbum = current,
                    mediaUri = File(localPath).toURI().toString(),
                    isOfflineFile = true,
                    lyrics = LrcParser.parse(current.song.lyricsContent)
                )
            }

            // Not downloaded locally -> requires network
            if (!isNetworkAvailable()) {
                return@withContext null
            }

            return@withContext try {
                val detailResp = api.getSongDetail(songCid)
                val detail = detailResp.data
                val sourceUrl = detail?.sourceUrl ?: current.song.sourceUrl
                val lyricUrl = detail?.lyricUrl ?: current.song.lyricUrl

                if (sourceUrl.isNullOrBlank()) return@withContext null

                var lyricsText = current.song.lyricsContent
                if (lyricsText.isNullOrBlank() && !lyricUrl.isNullOrBlank()) {
                    lyricsText = downloadManager.fetchTextUrl(lyricUrl)
                }

                dao.updateSongMediaMetadata(
                    songCid = songCid,
                    sourceUrl = sourceUrl,
                    lyricUrl = lyricUrl,
                    lyricsContent = lyricsText
                )

                val refreshed = dao.getSongWithAlbum(songCid) ?: current
                ResolvedPlaybackTrack(
                    songWithAlbum = refreshed,
                    mediaUri = sourceUrl,
                    isOfflineFile = false,
                    lyrics = LrcParser.parse(lyricsText)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed resolving remote track $songCid", e)
                null
            }
        }

    suspend fun toggleFavorite(songCid: String) = withContext(Dispatchers.IO) {
        val current = dao.getSongById(songCid) ?: return@withContext
        dao.setFavorite(songCid, !current.isFavorite)
    }

    suspend fun getSongWithAlbum(songCid: String): SongWithAlbum? = withContext(Dispatchers.IO) {
        dao.getSongWithAlbum(songCid)
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Введите название плейлиста")
        }
        val now = System.currentTimeMillis()
        dao.insertPlaylist(
            PlaylistEntity(
                name = trimmed,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            dao.renamePlaylist(playlistId, trimmed, System.currentTimeMillis())
        }
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songCid: String): Boolean =
        withContext(Dispatchers.IO) {
            if (dao.getPlaylistById(playlistId) == null) return@withContext false
            if (dao.isSongInPlaylist(playlistId, songCid)) return@withContext false
            val position = dao.nextPlaylistPosition(playlistId)
            val inserted = dao.insertPlaylistSong(
                PlaylistSongEntity(
                    playlistId = playlistId,
                    songCid = songCid,
                    position = position,
                    addedAt = System.currentTimeMillis()
                )
            )
            if (inserted != -1L) {
                dao.touchPlaylist(playlistId, System.currentTimeMillis())
                true
            } else {
                false
            }
        }

    suspend fun removeSongFromPlaylist(playlistId: Long, songCid: String) =
        withContext(Dispatchers.IO) {
            dao.removePlaylistSong(playlistId, songCid)
            dao.touchPlaylist(playlistId, System.currentTimeMillis())
        }

    suspend fun fetchNews(): Result<List<NewsSummaryDto>> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("Нет подключения к интернету"))
        }
        try {
            val resp = api.getNews()
            Result.success(resp.data?.list.orEmpty())
        } catch (e: Exception) {
            Log.w(TAG, "fetchNews failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchNewsDetail(cid: String): Result<NewsDetailDto?> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("Нет подключения к интернету"))
        }
        try {
            val resp = api.getNewsDetail(cid)
            Result.success(resp.data)
        } catch (e: Exception) {
            Log.w(TAG, "fetchNewsDetail failed: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "MonsterSirenRepo"
    }
}

data class ResolvedPlaybackTrack(
    val songWithAlbum: SongWithAlbum,
    val mediaUri: String,
    val isOfflineFile: Boolean,
    val lyrics: List<LyricLine>
)
