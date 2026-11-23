package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object DownloadStatus {
    const val NOT_DOWNLOADED = 0
    const val DOWNLOADING = 1
    const val DOWNLOADED = 2
    const val FAILED = 3
}

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val cid: String,
    val name: String,
    val intro: String = "",
    val belong: String = "",
    val coverUrl: String = "",
    val coverDeUrl: String = "",
    val localCoverPath: String? = null,
    val artistsJoined: String = "塞壬唱片-MSR",
    val displayOrder: Int = 0,
    val detailsLoaded: Boolean = false
)

@Entity(
    tableName = "songs",
    indices = [Index(value = ["albumCid"]), Index(value = ["downloadStatus"])]
)
data class SongEntity(
    @PrimaryKey val cid: String,
    val albumCid: String,
    val name: String,
    val artistsJoined: String = "塞壬唱片-MSR",
    val sourceUrl: String? = null,
    val lyricUrl: String? = null,
    val lyricsContent: String? = null,
    val localAudioPath: String? = null,
    val downloadStatus: Int = DownloadStatus.NOT_DOWNLOADED,
    val downloadProgress: Int = 0,
    val fileSizeBytes: Long = 0L,
    val trackIndex: Int = 0,
    val isFavorite: Boolean = false,
    val downloadedAt: Long = 0L
)

data class AlbumWithStats(
    @Embedded val album: AlbumEntity,
    val totalSongs: Int,
    val downloadedSongs: Int
) {
    val isFullyDownloaded: Boolean
        get() = totalSongs > 0 && downloadedSongs >= totalSongs

    val hasAnyDownloaded: Boolean
        get() = downloadedSongs > 0
}

data class SongWithAlbum(
    @Embedded val song: SongEntity,
    val albumName: String?,
    val albumCoverUrl: String?,
    val albumLocalCoverPath: String?
) {
    val isDownloaded: Boolean
        get() = song.downloadStatus == DownloadStatus.DOWNLOADED && !song.localAudioPath.isNullOrBlank()
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songCid"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("songCid")]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songCid: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val songCount: Int
)
