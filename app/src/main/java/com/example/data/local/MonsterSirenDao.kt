package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonsterSirenDao {

    @Query(
        """
        SELECT a.*,
            (SELECT COUNT(*) FROM songs s WHERE s.albumCid = a.cid) AS totalSongs,
            (SELECT COUNT(*) FROM songs s WHERE s.albumCid = a.cid AND s.downloadStatus = ${DownloadStatus.DOWNLOADED}) AS downloadedSongs
        FROM albums a
        ORDER BY a.displayOrder ASC
        """
    )
    fun observeAllAlbumsWithStats(): Flow<List<AlbumWithStats>>

    @Query(
        """
        SELECT a.*,
            (SELECT COUNT(*) FROM songs s WHERE s.albumCid = a.cid) AS totalSongs,
            (SELECT COUNT(*) FROM songs s WHERE s.albumCid = a.cid AND s.downloadStatus = ${DownloadStatus.DOWNLOADED}) AS downloadedSongs
        FROM albums a
        WHERE a.cid = :albumCid
        LIMIT 1
        """
    )
    fun observeAlbumWithStats(albumCid: String): Flow<AlbumWithStats?>

    @Query("SELECT * FROM albums WHERE cid = :albumCid LIMIT 1")
    suspend fun getAlbumById(albumCid: String): AlbumEntity?

    @Query("SELECT * FROM albums")
    suspend fun getAllAlbumsSnapshot(): List<AlbumEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbums(albums: List<AlbumEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: AlbumEntity)

    @Query("UPDATE albums SET localCoverPath = :localCoverPath WHERE cid = :albumCid")
    suspend fun updateAlbumLocalCover(albumCid: String, localCoverPath: String?)

    @Query(
        """
        SELECT s.*, a.name AS albumName, a.coverUrl AS albumCoverUrl, a.localCoverPath AS albumLocalCoverPath
        FROM songs s
        LEFT JOIN albums a ON s.albumCid = a.cid
        ORDER BY a.displayOrder ASC, s.trackIndex ASC, s.name ASC
        """
    )
    fun observeAllSongsWithAlbum(): Flow<List<SongWithAlbum>>

    @Query(
        """
        SELECT s.*, a.name AS albumName, a.coverUrl AS albumCoverUrl, a.localCoverPath AS albumLocalCoverPath
        FROM songs s
        LEFT JOIN albums a ON s.albumCid = a.cid
        WHERE s.albumCid = :albumCid
        ORDER BY s.trackIndex ASC, s.name ASC
        """
    )
    fun observeSongsByAlbum(albumCid: String): Flow<List<SongWithAlbum>>

    @Query(
        """
        SELECT s.*, a.name AS albumName, a.coverUrl AS albumCoverUrl, a.localCoverPath AS albumLocalCoverPath
        FROM songs s
        LEFT JOIN albums a ON s.albumCid = a.cid
        WHERE s.downloadStatus = ${DownloadStatus.DOWNLOADED}
        ORDER BY s.downloadedAt DESC, s.name ASC
        """
    )
    fun observeDownloadedSongs(): Flow<List<SongWithAlbum>>

    @Query(
        """
        SELECT s.*, a.name AS albumName, a.coverUrl AS albumCoverUrl, a.localCoverPath AS albumLocalCoverPath
        FROM songs s
        LEFT JOIN albums a ON s.albumCid = a.cid
        WHERE s.downloadStatus = ${DownloadStatus.DOWNLOADING}
        ORDER BY s.name ASC
        """
    )
    fun observeActiveDownloads(): Flow<List<SongWithAlbum>>

    @Query(
        """
        SELECT s.*, a.name AS albumName, a.coverUrl AS albumCoverUrl, a.localCoverPath AS albumLocalCoverPath
        FROM songs s
        LEFT JOIN albums a ON s.albumCid = a.cid
        WHERE s.isFavorite = 1
        ORDER BY a.displayOrder ASC, s.trackIndex ASC
        """
    )
    fun observeFavoriteSongs(): Flow<List<SongWithAlbum>>

    @Query("SELECT COALESCE(SUM(fileSizeBytes), 0) FROM songs WHERE downloadStatus = ${DownloadStatus.DOWNLOADED}")
    fun observeTotalOfflineBytes(): Flow<Long>

    @Query("SELECT COUNT(*) FROM songs WHERE downloadStatus = ${DownloadStatus.DOWNLOADED}")
    fun observeDownloadedSongCount(): Flow<Int>

    @Query(
        """
        SELECT s.*, a.name AS albumName, a.coverUrl AS albumCoverUrl, a.localCoverPath AS albumLocalCoverPath
        FROM songs s
        LEFT JOIN albums a ON s.albumCid = a.cid
        WHERE s.cid = :songCid
        LIMIT 1
        """
    )
    suspend fun getSongWithAlbum(songCid: String): SongWithAlbum?

    @Query("SELECT * FROM songs WHERE cid = :songCid LIMIT 1")
    suspend fun getSongById(songCid: String): SongEntity?

    @Query("SELECT * FROM songs WHERE albumCid = :albumCid ORDER BY trackIndex ASC")
    suspend fun getSongsByAlbumSnapshot(albumCid: String): List<SongEntity>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsSnapshot(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE downloadStatus = ${DownloadStatus.DOWNLOADED}")
    suspend fun getDownloadedSongsSnapshot(): List<SongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSong(song: SongEntity)

    @Query("UPDATE songs SET downloadStatus = :status, downloadProgress = :progress WHERE cid = :songCid")
    suspend fun updateDownloadProgress(songCid: String, status: Int, progress: Int)

    @Query(
        """
        UPDATE songs SET
            sourceUrl = COALESCE(:sourceUrl, sourceUrl),
            lyricUrl = COALESCE(:lyricUrl, lyricUrl),
            lyricsContent = COALESCE(:lyricsContent, lyricsContent),
            localAudioPath = :localAudioPath,
            downloadStatus = :status,
            downloadProgress = :progress,
            fileSizeBytes = :fileSizeBytes,
            downloadedAt = :downloadedAt
        WHERE cid = :songCid
        """
    )
    suspend fun updateSongDownloadResult(
        songCid: String,
        sourceUrl: String?,
        lyricUrl: String?,
        lyricsContent: String?,
        localAudioPath: String?,
        status: Int,
        progress: Int,
        fileSizeBytes: Long,
        downloadedAt: Long
    )

    @Query(
        """
        UPDATE songs SET
            sourceUrl = COALESCE(:sourceUrl, sourceUrl),
            lyricUrl = COALESCE(:lyricUrl, lyricUrl),
            lyricsContent = COALESCE(:lyricsContent, lyricsContent)
        WHERE cid = :songCid
        """
    )
    suspend fun updateSongMediaMetadata(
        songCid: String,
        sourceUrl: String?,
        lyricUrl: String?,
        lyricsContent: String?
    )

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE cid = :songCid")
    suspend fun setFavorite(songCid: String, isFavorite: Boolean)

    @Query(
        """
        UPDATE songs SET
            localAudioPath = NULL,
            downloadStatus = ${DownloadStatus.NOT_DOWNLOADED},
            downloadProgress = 0,
            fileSizeBytes = 0,
            downloadedAt = 0
        WHERE cid = :songCid
        """
    )
    suspend fun clearSongDownload(songCid: String)

    @Query(
        """
        UPDATE songs SET
            localAudioPath = NULL,
            downloadStatus = ${DownloadStatus.NOT_DOWNLOADED},
            downloadProgress = 0,
            fileSizeBytes = 0,
            downloadedAt = 0
        WHERE downloadStatus = ${DownloadStatus.DOWNLOADING}
        """
    )
    suspend fun resetStuckDownloads()
}
