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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SongWithAlbum
import com.example.player.PlaybackUiState
import com.example.ui.components.AudioWaveIndicator
import com.example.ui.components.CoverArtImage
import com.example.ui.components.DownloadActionButton
import com.example.ui.components.MsrHalftoneBackground
import com.example.ui.components.MsrHamburgerButton
import com.example.ui.components.MsrOutlinedPlayButton
import com.example.ui.components.MsrSearchPillBar
import com.example.ui.components.MsrSectionDividerWithHatches
import com.example.ui.theme.MsrAccentRed
import com.example.ui.theme.MsrBackgroundDeep
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrGrayMuted
import com.example.ui.theme.MsrHairline
import com.example.ui.theme.MsrMonoMicro
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrSectionHeading
import com.example.ui.theme.MsrSurfaceElevated
import com.example.ui.theme.MsrWhite
import com.example.ui.util.Formatters
import com.example.ui.viewmodel.CatalogUiState
import com.example.ui.viewmodel.OfflineVaultUiState

private const val PLAYLIST_TAB_OFFLINE = 0
private const val PLAYLIST_TAB_FAVORITES = 1
private const val PLAYLIST_TAB_QUEUE = 2
private const val PLAYLIST_TAB_ALL = 3

/**
 * Unified PLAYLIST screen opened from the slide-over navigation menu ("PLAYLIST / Плейлист").
 * Combines Offline Vault, Favorites, Current Playback Queue, and All Tracks in Monster Siren's monochrome aesthetic.
 */
@Composable
fun MsrPlaylistScreen(
    catalogState: CatalogUiState,
    offlineState: OfflineVaultUiState,
    playbackState: PlaybackUiState,
    isMenuOpen: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onOpenMenu: () -> Unit,
    onPlaySongs: (List<SongWithAlbum>, Int) -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onToggleOfflineOnly: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadSong: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteSongDownload: (String) -> Unit,
    onClearAllDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(PLAYLIST_TAB_OFFLINE) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MsrSurfaceElevated,
            title = {
                Text(
                    text = stringResource(R.string.confirm_clear_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MsrWhite
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.confirm_clear_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MsrGrayLight
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAllDownloads()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MsrAccentRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(
                        text = stringResource(R.string.action_cancel).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MsrGrayLight
                    )
                }
            }
        )
    }

    MsrHalftoneBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar: Search pill when browsing ALL tracks, or brand logo + hamburger
            if (selectedTab == PLAYLIST_TAB_ALL) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MsrSearchPillBar(
                        query = catalogState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MsrHamburgerButton(
                        onClick = onOpenMenu,
                        hidden = isMenuOpen,
                        modifier = Modifier.testTag("playlist_open_menu_button")
                    )
                }
            } else {
                MsrSimpleSectionTopBar(
                    isMenuOpen = isMenuOpen,
                    onOpenMenu = onOpenMenu,
                    tag = "playlist_open_menu_button"
                )
            }

            // Section Header: PLAYLIST + storage summary + Hatched Divider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MsrWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "PLAYLIST",
                            style = MsrSectionHeading,
                            color = MsrWhite
                        )
                    }
                    Text(
                        text = Formatters.formatFileSize(offlineState.totalOfflineBytes),
                        style = MsrMonoMicro,
                        color = MsrGrayLight
                    )
                }
                MsrSectionDividerWithHatches()

                Spacer(modifier = Modifier.height(12.dp))

                // Sharp monochrome segmented sub-tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        PLAYLIST_TAB_OFFLINE to "OFFLINE (${offlineState.downloadedSongs.size})",
                        PLAYLIST_TAB_FAVORITES to "FAV (${offlineState.favoriteSongs.size})",
                        PLAYLIST_TAB_QUEUE to "QUEUE",
                        PLAYLIST_TAB_ALL to "ALL"
                    )
                    tabs.forEach { (tabId, label) ->
                        val active = selectedTab == tabId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .border(1.dp, if (active) MsrPureWhite else MsrHairline.copy(alpha = 0.55f))
                                .background(
                                    if (active) MsrPureWhite.copy(alpha = 0.12f)
                                    else MsrBackgroundDeep.copy(alpha = 0.55f)
                                )
                                .clickable { selectedTab = tabId }
                                .testTag("playlist_subtab_$tabId"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MsrMonoMicro,
                                color = if (active) MsrPureWhite else MsrGrayMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("playlist_content_list")
            ) {
                when (selectedTab) {
                    PLAYLIST_TAB_OFFLINE -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MsrOutlinedPlayButton(
                                        label = "PLAY OFFLINE (${offlineState.downloadedSongs.size})",
                                        isPlaying = playbackState.isPlaying && playbackState.isPlayingOfflineFile,
                                        onClick = {
                                            if (offlineState.downloadedSongs.isNotEmpty()) {
                                                onPlaySongs(offlineState.downloadedSongs, 0)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("offline_play_all_button")
                                    )

                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .border(1.dp, if (catalogState.offlineOnlyMode) MsrPureWhite else MsrHairline)
                                            .background(MsrBackgroundDeep.copy(alpha = 0.65f))
                                            .clickable(onClick = onToggleOfflineOnly)
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (catalogState.offlineOnlyMode) "ONLY OFF: ON" else "ONLY OFF",
                                            style = MsrMonoMicro,
                                            color = if (catalogState.offlineOnlyMode) MsrPureWhite else MsrGrayMuted
                                        )
                                    }

                                    if (offlineState.downloadedSongs.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .border(1.dp, MsrHairline)
                                                .clickable { showClearDialog = true }
                                                .testTag("offline_clear_all_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteSweep,
                                                contentDescription = stringResource(R.string.offline_clear_all),
                                                tint = MsrGrayLight,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (offlineState.activeDownloads.isNotEmpty()) {
                                    offlineState.activeDownloads.forEach { active ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = active.song.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MsrWhite,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    text = "${active.song.downloadProgress}%",
                                                    style = MsrMonoMicro,
                                                    color = MsrGrayLight
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { active.song.downloadProgress / 100f },
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

                        if (offlineState.downloadedSongs.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.offline_empty_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MsrGrayMuted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = offlineState.downloadedSongs,
                                key = { _, s -> "vault_${s.song.cid}" }
                            ) { idx, songItem ->
                                MsrSongListRow(
                                    songItem = songItem,
                                    indexLabel = "${idx + 1}",
                                    isCurrent = playbackState.currentSong?.song?.cid == songItem.song.cid,
                                    isPlaying = playbackState.isPlaying,
                                    subtitleExtra = Formatters.formatFileSize(songItem.song.fileSizeBytes),
                                    onClick = { onPlaySongs(offlineState.downloadedSongs, idx) },
                                    onToggleFavorite = { onToggleFavorite(songItem.song.cid) },
                                    onDownload = { onDownloadSong(songItem.song.cid) },
                                    onCancel = { onCancelDownload(songItem.song.cid) },
                                    onDelete = { onDeleteSongDownload(songItem.song.cid) }
                                )
                            }
                        }
                    }

                    PLAYLIST_TAB_FAVORITES -> {
                        if (offlineState.favoriteSongs.isNotEmpty()) {
                            item {
                                MsrOutlinedPlayButton(
                                    label = "PLAY FAVORITES (${offlineState.favoriteSongs.size})",
                                    isPlaying = playbackState.isPlaying,
                                    onClick = { onPlaySongs(offlineState.favoriteSongs, 0) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("favorites_play_all_button")
                                )
                            }

                            itemsIndexed(
                                items = offlineState.favoriteSongs,
                                key = { _, s -> "fav_${s.song.cid}" }
                            ) { idx, songItem ->
                                MsrSongListRow(
                                    songItem = songItem,
                                    indexLabel = "${idx + 1}",
                                    isCurrent = playbackState.currentSong?.song?.cid == songItem.song.cid,
                                    isPlaying = playbackState.isPlaying,
                                    subtitleExtra = if (songItem.isDownloaded) "OFFLINE" else null,
                                    onClick = { onPlaySongs(offlineState.favoriteSongs, idx) },
                                    onToggleFavorite = { onToggleFavorite(songItem.song.cid) },
                                    onDownload = { onDownloadSong(songItem.song.cid) },
                                    onCancel = { onCancelDownload(songItem.song.cid) },
                                    onDelete = { onDeleteSongDownload(songItem.song.cid) }
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = stringResource(R.string.favorites_empty_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MsrGrayMuted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    PLAYLIST_TAB_QUEUE -> {
                        val queueItems = playbackState.queue
                        if (queueItems.isEmpty()) {
                            item {
                                Text(
                                    text = "Очередь воспроизведения пуста. Выберите любой альбом или трек, чтобы начать прослушивание.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MsrGrayMuted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = queueItems,
                                key = { idx, s -> "queue_${idx}_${s.song.cid}" }
                            ) { idx, songItem ->
                                MsrSongListRow(
                                    songItem = songItem,
                                    indexLabel = "${idx + 1}",
                                    isCurrent = idx == playbackState.currentIndex,
                                    isPlaying = playbackState.isPlaying,
                                    subtitleExtra = if (songItem.isDownloaded) "OFFLINE" else null,
                                    onClick = { onPlayQueueIndex(idx) },
                                    onToggleFavorite = { onToggleFavorite(songItem.song.cid) },
                                    onDownload = { onDownloadSong(songItem.song.cid) },
                                    onCancel = { onCancelDownload(songItem.song.cid) },
                                    onDelete = { onDeleteSongDownload(songItem.song.cid) }
                                )
                            }
                        }
                    }

                    else -> {
                        if (catalogState.songs.isEmpty()) {
                            item {
                                Text(
                                    text = if (catalogState.offlineOnlyMode) {
                                        stringResource(R.string.offline_empty_hint)
                                    } else {
                                        stringResource(R.string.empty_catalog_hint)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MsrGrayMuted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = catalogState.songs,
                                key = { _, s -> "all_track_${s.song.cid}" }
                            ) { idx, songItem ->
                                MsrSongListRow(
                                    songItem = songItem,
                                    indexLabel = null,
                                    isCurrent = playbackState.currentSong?.song?.cid == songItem.song.cid,
                                    isPlaying = playbackState.isPlaying,
                                    subtitleExtra = if (songItem.isDownloaded) "OFFLINE" else null,
                                    onClick = { onPlaySongs(catalogState.songs, idx) },
                                    onToggleFavorite = { onToggleFavorite(songItem.song.cid) },
                                    onDownload = { onDownloadSong(songItem.song.cid) },
                                    onCancel = { onCancelDownload(songItem.song.cid) },
                                    onDelete = { onDeleteSongDownload(songItem.song.cid) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MsrSimpleSectionTopBar(
    isMenuOpen: Boolean,
    onOpenMenu: () -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_msr_logo_mark),
                contentDescription = "Monster Siren",
                tint = MsrWhite,
                modifier = Modifier.size(width = 34.dp, height = 17.dp)
            )
            Text(
                text = "MONSTER SIREN",
                style = MaterialTheme.typography.labelLarge,
                color = MsrGrayMuted
            )
        }
        MsrHamburgerButton(
            onClick = onOpenMenu,
            hidden = isMenuOpen,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
internal fun MsrSongListRow(
    songItem: SongWithAlbum,
    indexLabel: String?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    subtitleExtra: String?,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (indexLabel != null) {
            Box(
                modifier = Modifier.width(26.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isCurrent) {
                    AudioWaveIndicator(isPlaying = isPlaying, color = MsrPureWhite)
                } else {
                    Text(
                        text = indexLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MsrGrayMuted
                    )
                }
            }
        }

        CoverArtImage(
            localPath = songItem.albumLocalCoverPath,
            remoteUrl = songItem.albumCoverUrl,
            contentDescription = songItem.song.name,
            modifier = Modifier.size(46.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = songItem.song.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MsrPureWhite else MsrWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = if (!subtitleExtra.isNullOrBlank()) {
                "${songItem.song.artistsJoined} · $subtitleExtra"
            } else {
                songItem.song.artistsJoined
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MsrGrayMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.minimumInteractiveComponentSize()
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
            onDownload = onDownload,
            onCancel = onCancel,
            onDelete = onDelete
        )
    }
}
