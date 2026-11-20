package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DownloadStatus
import com.example.data.local.SongWithAlbum
import com.example.player.PlaybackUiState
import com.example.ui.components.AudioWaveIndicator
import com.example.ui.components.CoverArtImage
import com.example.ui.components.DownloadActionButton
import com.example.ui.components.MsrHalftoneBackground
import com.example.ui.components.MsrHamburgerButton
import com.example.ui.components.MsrHookArrowButton
import com.example.ui.components.MsrOutlinedPlayButton
import com.example.ui.components.MsrWaveNodeDivider
import com.example.ui.theme.MsrAccentRed
import com.example.ui.theme.MsrBackgroundDeep
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrGrayMuted
import com.example.ui.theme.MsrHairline
import com.example.ui.theme.MsrMonoMicro
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrWhite
import com.example.ui.viewmodel.AlbumDetailUiState

@Composable
fun AlbumDetailScreen(
    albumCid: String,
    state: AlbumDetailUiState,
    playbackState: PlaybackUiState,
    isMenuOpen: Boolean = false,
    onLoadAlbumDetail: (String) -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
    onPlaySongs: (List<SongWithAlbum>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadAlbum: (String) -> Unit,
    onDeleteAlbumDownloads: (String) -> Unit,
    onDownloadSong: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteSongDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(albumCid) {
        onLoadAlbumDetail(albumCid)
    }

    val albumStats = state.album
    val songs = state.songs
    val isDownloadingAny = songs.any { it.song.downloadStatus == DownloadStatus.DOWNLOADING }
    val isAlbumPlaying = playbackState.isPlaying &&
        playbackState.currentSong?.song?.albumCid == albumCid

    MsrHalftoneBackground(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 28.dp),
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("album_detail_list")
        ) {
            // Exact Top Bar from Screenshot 2: Custom Left Hook Arrow + Right 3-line Hamburger
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MsrHookArrowButton(
                        pointRight = false,
                        onClick = onBack,
                        modifier = Modifier.testTag("album_back_button")
                    )
                    MsrHamburgerButton(
                        onClick = onOpenMenu,
                        hidden = isMenuOpen,
                        modifier = Modifier.testTag("album_menu_button")
                    )
                }
            }

            // Large Centered Album Artwork Card with thin bottom crimson edge (exact Screenshot 2)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    CoverArtImage(
                        localPath = albumStats?.album?.localCoverPath,
                        remoteUrl = albumStats?.album?.coverDeUrl?.takeIf { it.isNotBlank() }
                            ?: albumStats?.album?.coverUrl,
                        contentDescription = albumStats?.album?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.16f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(MsrAccentRed.copy(alpha = 0.85f))
                    )
                }
            }

            // Album Title + Artist on left + Exact Outlined `[ ||| PLAY ]` button on right (Screenshot 2)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = albumStats?.album?.name ?: "Monster Siren",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MsrWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = albumStats?.album?.artistsJoined ?: "塞壬唱片-MSR",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MsrGrayLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        MsrOutlinedPlayButton(
                            label = "PLAY",
                            isPlaying = isAlbumPlaying,
                            onClick = { if (songs.isNotEmpty()) onPlaySongs(songs, 0) },
                            modifier = Modifier.testTag("album_play_all_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Exact MSR Wave Node Decorative Divider (`◎~~~~~~~~`)
                    MsrWaveNodeDivider()

                    // Clean monochrome Offline Download control row integrated seamlessly
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val fullyDownloaded = albumStats?.isFullyDownloaded == true
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .border(1.dp, if (fullyDownloaded) MsrPureWhite else MsrHairline)
                                .background(MsrBackgroundDeep.copy(alpha = 0.5f))
                                .clickable(enabled = songs.isNotEmpty() && !fullyDownloaded) {
                                    onDownloadAlbum(albumCid)
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("album_download_all_button")
                        ) {
                            Icon(
                                imageVector = if (fullyDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                contentDescription = null,
                                tint = if (fullyDownloaded) MsrPureWhite else MsrGrayLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    fullyDownloaded -> "OFFLINE READY (${songs.size}/${songs.size})"
                                    isDownloadingAny -> "DOWNLOADING (${albumStats?.downloadedSongs ?: 0}/${songs.size})…"
                                    else -> "DOWNLOAD ALBUM OFFLINE"
                                },
                                style = MsrMonoMicro,
                                color = if (fullyDownloaded) MsrPureWhite else MsrGrayLight
                            )
                        }

                        if (albumStats?.hasAnyDownloaded == true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onDeleteAlbumDownloads(albumCid) }
                                    .padding(horizontal = 8.dp, vertical = 7.dp)
                                    .testTag("album_delete_cache_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = stringResource(R.string.album_delete_downloads),
                                    tint = MsrGrayMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    if (albumStats != null && albumStats.totalSongs > 0 && (isDownloadingAny || (albumStats.hasAnyDownloaded && !albumStats.isFullyDownloaded))) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { albumStats.downloadedSongs.toFloat() / albumStats.totalSongs.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp),
                            color = MsrPureWhite,
                            trackColor = MsrHairline
                        )
                    }

                    // Official Album Intro paragraph (exact Screenshot 2 spacing)
                    val introText = albumStats?.album?.intro?.trim().orEmpty()
                    if (introText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = introText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MsrGrayLight
                        )
                    }
                }
            }

            // Exact Clean Spaced Numbered Tracklist (`1`, `2`, `3` — Screenshot 2)
            itemsIndexed(
                items = songs,
                key = { _, item -> item.song.cid }
            ) { index, songItem ->
                val isCurrent = playbackState.currentSong?.song?.cid == songItem.song.cid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaySongs(songs, index) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("song_row_${songItem.song.cid}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.width(30.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (isCurrent) {
                                AudioWaveIndicator(isPlaying = playbackState.isPlaying, color = MsrPureWhite)
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MsrWhite
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = songItem.song.name.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isCurrent) MsrPureWhite else MsrWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = songItem.song.artistsJoined,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MsrGrayMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onToggleFavorite(songItem.song.cid) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("favorite_song_${songItem.song.cid}")
                        ) {
                            Icon(
                                imageVector = if (songItem.song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.cd_favorite),
                                tint = if (songItem.song.isFavorite) MsrPureWhite else MsrGrayMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DownloadActionButton(
                            downloadStatus = songItem.song.downloadStatus,
                            downloadProgress = songItem.song.downloadProgress,
                            onDownload = { onDownloadSong(songItem.song.cid) },
                            onCancel = { onCancelDownload(songItem.song.cid) },
                            onDelete = { onDeleteSongDownload(songItem.song.cid) },
                            modifier = Modifier.testTag("download_song_${songItem.song.cid}")
                        )
                    }

                    if (songItem.song.downloadStatus == DownloadStatus.DOWNLOADING) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { songItem.song.downloadProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.5.dp),
                            color = MsrPureWhite,
                            trackColor = MsrHairline
                        )
                    }
                }
            }
        }
    }
}
