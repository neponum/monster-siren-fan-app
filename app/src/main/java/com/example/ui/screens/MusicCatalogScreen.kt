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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AlbumWithStats
import com.example.data.local.SongWithAlbum
import com.example.player.PlaybackUiState
import com.example.ui.components.CoverArtImage
import com.example.ui.components.DownloadActionButton
import com.example.ui.components.MsrAlbumSleeveIcon
import com.example.ui.components.MsrHalftoneBackground
import com.example.ui.components.MsrHamburgerButton
import com.example.ui.components.MsrScrollFooterMarker
import com.example.ui.components.MsrSearchPillBar
import com.example.ui.components.MsrSectionDividerWithHatches
import com.example.ui.components.MsrVerticalHatchScrollbar
import com.example.ui.theme.MsrBackgroundDeep
import com.example.ui.theme.MsrGrayDark
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrGrayMuted
import com.example.ui.theme.MsrHairline
import com.example.ui.theme.MsrMonoMicro
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrSectionHeading
import com.example.ui.theme.MsrWhite
import com.example.ui.viewmodel.CatalogUiState

@Composable
fun MusicCatalogScreen(
    state: CatalogUiState,
    playbackState: PlaybackUiState,
    isMenuOpen: Boolean = false,
    onSearchQueryChange: (String) -> Unit,
    onToggleOfflineOnly: () -> Unit,
    onSyncCatalog: () -> Unit,
    onOpenMenu: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlaySongs: (List<SongWithAlbum>, Int) -> Unit,
    onDownloadAlbum: (String) -> Unit,
    onDownloadSong: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteSongDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    MsrHalftoneBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Exact Top Row: Pill Search Bar ("A WORLD FAMILIARLY UNKNOWN") + 3-line Hamburger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MsrSearchPillBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                MsrHamburgerButton(
                    onClick = onOpenMenu,
                    hidden = isMenuOpen,
                    modifier = Modifier.testTag("open_msr_menu_button")
                )
            }

            // Exact Section Header: Album Sleeve Icon + "ALBUMS" + Offline filter link + Hatched Divider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MsrAlbumSleeveIcon()
                        Text(
                            text = "ALBUMS",
                            style = MsrSectionHeading,
                            color = MsrWhite
                        )
                    }

                    // Minimal monochrome offline filter toggle integrated cleanly into header
                    Text(
                        text = if (state.offlineOnlyMode) {
                            "OFFLINE (${state.downloadedCount})"
                        } else if (state.downloadedCount > 0) {
                            "ALL · ${state.downloadedCount} OFF"
                        } else {
                            "ALL"
                        },
                        style = MsrMonoMicro,
                        color = if (state.offlineOnlyMode) MsrPureWhite else MsrGrayMuted,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (state.offlineOnlyMode) MsrWhite else MsrHairline.copy(alpha = 0.5f)
                            )
                            .clickable(onClick = onToggleOfflineOnly)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("offline_only_chip")
                    )
                }

                MsrSectionDividerWithHatches()
            }

            // Main Single-Column Album List + Right-Edge Vertical Hatch Scrollbar
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.albums.isEmpty() && state.songs.isEmpty()) {
                    MsrEmptyState(
                        isSyncing = state.isSyncing,
                        offlineOnlyMode = state.offlineOnlyMode,
                        errorMessage = state.syncError,
                        onRetry = onSyncCatalog,
                        onDisableOfflineOnly = onToggleOfflineOnly
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 22.dp,
                            top = 14.dp,
                            bottom = 36.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("albums_list")
                    ) {
                        items(
                            items = state.albums,
                            key = { it.album.cid }
                        ) { albumWithStats ->
                            MsrAlbumListRow(
                                item = albumWithStats,
                                onClick = { onAlbumClick(albumWithStats.album.cid) },
                                onDownloadAlbum = { onDownloadAlbum(albumWithStats.album.cid) }
                            )
                        }

                        // When actively searching, also display matching individual tracks right below albums
                        if (state.searchQuery.isNotBlank() && state.songs.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Text(
                                        text = "SONGS (${state.songs.size})",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MsrWhite
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    MsrSectionDividerWithHatches()
                                }
                            }

                            itemsIndexed(
                                items = state.songs.take(40),
                                key = { _, s -> "search_song_${s.song.cid}" }
                            ) { idx, songItem ->
                                val isCurrent = playbackState.currentSong?.song?.cid == songItem.song.cid
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPlaySongs(state.songs, idx) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CoverArtImage(
                                        localPath = songItem.albumLocalCoverPath,
                                        remoteUrl = songItem.albumCoverUrl,
                                        contentDescription = songItem.song.name,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = songItem.song.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isCurrent) MsrPureWhite else MsrWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = songItem.song.artistsJoined,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MsrGrayMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    DownloadActionButton(
                                        downloadStatus = songItem.song.downloadStatus,
                                        downloadProgress = songItem.song.downloadProgress,
                                        onDownload = { onDownloadSong(songItem.song.cid) },
                                        onCancel = { onCancelDownload(songItem.song.cid) },
                                        onDelete = { onDeleteSongDownload(songItem.song.cid) }
                                    )
                                }
                            }
                        }
                    }

                    // Exact right-edge vertical diagonal hatch bar (Screenshot 1)
                    MsrVerticalHatchScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                            .fillMaxHeight(0.45f)
                    )

                    // Exact bottom-right "SCROLL / <<< " marker hovering right above mini-player
                    MsrScrollFooterMarker(
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
        }
    }
}

/**
 * Exact horizontal single-column Album Row matching Screenshot 1:
 * 92dp square cover on left, generous gap, crisp bold title + muted artist.
 */
@Composable
private fun MsrAlbumListRow(
    item: AlbumWithStats,
    onClick: () -> Unit,
    onDownloadAlbum: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("album_card_${item.album.cid}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(90.dp)) {
            CoverArtImage(
                localPath = item.album.localCoverPath,
                remoteUrl = item.album.coverUrl,
                contentDescription = item.album.name,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle white bottom progress hairline if saved offline
            if (item.hasAnyDownloaded && item.totalSongs > 0) {
                val frac = (item.downloadedSongs.toFloat() / item.totalSongs.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(MsrBackgroundDeep.copy(alpha = 0.7f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .height(2.dp)
                            .background(MsrPureWhite)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.album.name,
                style = MaterialTheme.typography.titleLarge,
                color = MsrWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (item.hasAnyDownloaded) {
                    "${item.album.artistsJoined}  ·  ${item.downloadedSongs}/${item.totalSongs}"
                } else {
                    item.album.artistsJoined
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MsrGrayMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDownloadAlbum,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("quick_download_album_${item.album.cid}")
        ) {
            Icon(
                imageVector = if (item.isFullyDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                contentDescription = stringResource(R.string.cd_download),
                tint = if (item.isFullyDownloaded) MsrPureWhite else MsrGrayDark,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun MsrEmptyState(
    isSyncing: Boolean,
    offlineOnlyMode: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDisableOfflineOnly: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isSyncing) {
                CircularProgressIndicator(color = MsrWhite, strokeWidth = 2.dp)
                Text(
                    text = "LOADING ALBUMS…",
                    style = MaterialTheme.typography.labelLarge,
                    color = MsrWhite
                )
            } else {
                Text(
                    text = if (offlineOnlyMode) "NO OFFLINE ALBUMS" else "CATALOG UNAVAILABLE",
                    style = MaterialTheme.typography.titleLarge,
                    color = MsrWhite
                )
                Text(
                    text = errorMessage ?: if (offlineOnlyMode) {
                        stringResource(R.string.offline_empty_hint)
                    } else {
                        stringResource(R.string.empty_catalog_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MsrGrayMuted,
                    textAlign = TextAlign.Center
                )
                Box(
                    modifier = Modifier
                        .border(1.dp, MsrGrayLight)
                        .clickable { if (offlineOnlyMode) onDisableOfflineOnly() else onRetry() }
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                        .testTag("empty_catalog_action_button")
                ) {
                    Text(
                        text = if (offlineOnlyMode) "SHOW ALL" else "RETRY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MsrWhite
                    )
                }
            }
        }
    }
}
