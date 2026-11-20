package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SongWithAlbum
import com.example.data.repository.LrcParser
import com.example.player.PlaybackUiState
import com.example.player.RepeatMode
import com.example.ui.components.AudioWaveIndicator
import com.example.ui.components.DownloadActionButton
import com.example.ui.components.MsrHalftoneBackground
import com.example.ui.components.MsrHookArrowButton
import com.example.ui.components.MsrRepeatModeGlyph
import com.example.ui.components.MsrVinylTurntableDisc
import com.example.ui.components.MsrWaveNodeDivider
import com.example.ui.theme.MsrDrawerBg
import com.example.ui.theme.MsrGrayDark
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrGrayMuted
import com.example.ui.theme.MsrHairline
import com.example.ui.theme.MsrMonoMicro
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrWhite
import com.example.ui.util.Formatters

/**
 * Exact pixel match to Screenshot 5 (Full Vinyl Player) + Screenshot 3 (Right-Side Slide-Over Playlist Drawer).
 */
@Composable
fun FullPlayerOverlay(
    playbackState: PlaybackUiState,
    allSongsFallback: List<SongWithAlbum>,
    showPlaylistDrawer: Boolean,
    onShowPlaylistDrawerChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit,
    onPlayFallbackSongs: (List<SongWithAlbum>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadSong: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (showPlaylistDrawer) {
            onShowPlaylistDrawerChange(false)
        } else {
            onDismiss()
        }
    }

    val song = playbackState.currentSong ?: return
    var showLyrics by rememberSaveable { mutableStateOf(false) }

    var isSeeking by remember { mutableStateOf(false) }
    var sliderDragPos by remember { mutableFloatStateOf(0f) }

    val duration = playbackState.durationMs.coerceAtLeast(1L)
    val displayedPosition = if (isSeeking) {
        (sliderDragPos * duration).toLong()
    } else {
        playbackState.positionMs.coerceAtMost(duration)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("full_player_overlay")
    ) {
        MsrHalftoneBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Bar (Screenshot 5): Left downward chevron `⌄` | Right `PLAYLIST ⊙`
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_collapse_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_collapse_player),
                            tint = MsrWhite,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clickable { onShowPlaylistDrawerChange(true) }
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .testTag("player_queue_button")
                    ) {
                        Text(
                            text = "PLAYLIST",
                            style = MaterialTheme.typography.labelLarge,
                            color = MsrWhite
                        )
                        // Small concentric vinyl circle icon `⊙` (exact Screenshot 5 top-right)
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawCircle(
                                color = MsrGrayLight,
                                radius = size.minDimension / 2f - 1.dp.toPx(),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            drawCircle(
                                color = MsrWhite,
                                radius = 2.dp.toPx()
                            )
                        }
                    }
                }

                // Track Title & Artist (Top-Left Aligned — exact Screenshot 5 & Screenshot 3 left side)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 12.dp, top = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.song.name.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MsrWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (playbackState.isPlayingOfflineFile) {
                                "${song.song.artistsJoined}  ·  OFFLINE"
                            } else {
                                song.song.artistsJoined
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MsrGrayMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(song.song.cid) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (song.song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (song.song.isFavorite) MsrPureWhite else MsrGrayMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DownloadActionButton(
                        downloadStatus = song.song.downloadStatus,
                        downloadProgress = song.song.downloadProgress,
                        onDownload = { onDownloadSong(song.song.cid) },
                        onCancel = { onCancelDownload(song.song.cid) },
                        onDelete = { onDeleteDownload(song.song.cid) },
                        modifier = Modifier.testTag("player_download_button")
                    )
                }

                // Center Stage: Rotating Vinyl Turntable (Screenshot 5) OR Synced Lyrics with Wave Divider (Screenshot 3 left)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showLyrics = !showLyrics },
                    contentAlignment = Alignment.Center
                ) {
                    if (!showLyrics) {
                        MsrVinylTurntableDisc(
                            localCoverPath = song.albumLocalCoverPath,
                            remoteCoverUrl = song.albumCoverUrl,
                            isPlaying = playbackState.isPlaying
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 22.dp, vertical = 14.dp)
                        ) {
                            MsrWaveNodeDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            MsrSyncedLyricsList(
                                lyrics = playbackState.lyrics,
                                positionMs = displayedPosition,
                                onSeekToLine = onSeekTo
                            )
                        }
                    }
                }

                // Bottom Timeline & Timestamp (exact Screenshot 5 right-aligned `--:-- / --:--` above thin line + white dot)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val posStr = if (playbackState.durationMs > 0L) Formatters.formatDuration(displayedPosition) else "--:--"
                        val durStr = if (playbackState.durationMs > 0L) Formatters.formatDuration(playbackState.durationMs) else "--:--"
                        Text(
                            text = "$posStr  /  $durStr",
                            style = MsrMonoMicro,
                            color = MsrGrayMuted
                        )
                    }

                    val sliderValue = if (isSeeking) {
                        sliderDragPos
                    } else if (playbackState.durationMs > 0L) {
                        (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    Slider(
                        value = sliderValue,
                        onValueChange = { value ->
                            isSeeking = true
                            sliderDragPos = value
                        },
                        onValueChangeFinished = {
                            val target = (sliderDragPos * duration).toLong()
                            isSeeking = false
                            onSeekTo(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MsrPureWhite,
                            activeTrackColor = MsrWhite,
                            inactiveTrackColor = MsrHairline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_slider")
                    )
                }

                // Exact 5-Icon Monochrome Bottom Transport Bar (Screenshot 5)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCyclePlaybackMode,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_repeat_button")
                    ) {
                        MsrRepeatModeGlyph(
                            repeatMode = playbackState.repeatMode,
                            shuffleEnabled = playbackState.shuffleEnabled
                        )
                    }

                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_previous_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastRewind,
                            contentDescription = stringResource(R.string.cd_previous),
                            tint = MsrWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("player_play_pause_button")
                    ) {
                        if (playbackState.isBuffering) {
                            CircularProgressIndicator(
                                color = MsrPureWhite,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(
                                    if (playbackState.isPlaying) R.string.cd_pause else R.string.cd_play
                                ),
                                tint = MsrPureWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FastForward,
                            contentDescription = stringResource(R.string.cd_next),
                            tint = MsrWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { showLyrics = !showLyrics },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_tab_lyrics")
                    ) {
                        AudioWaveIndicator(
                            isPlaying = playbackState.isPlaying,
                            color = if (showLyrics) MsrPureWhite else MsrGrayLight
                        )
                    }
                }
            }
        }

        // Exact Screenshot 3: Right-Side Slide-Over Dark Playlist Drawer
        MsrSlideOverPlaylistDrawer(
            visible = showPlaylistDrawer,
            playbackState = playbackState,
            allSongsFallback = allSongsFallback,
            onDismiss = { onShowPlaylistDrawerChange(false) },
            onSelectQueueIndex = { idx ->
                onPlayQueueIndex(idx)
                onShowPlaylistDrawerChange(false)
            },
            onSelectFallbackSong = { songs, idx ->
                onPlayFallbackSongs(songs, idx)
                onShowPlaylistDrawerChange(false)
            }
        )
    }
}

/**
 * Exact 70%-width right-hand dark playlist slide-over drawer matching Screenshot 3.
 * Used both from the Full Player ("PLAYLIST ⊙") AND from the Persistent Mini-Player list icon!
 */
@Composable
fun MsrSlideOverPlaylistDrawer(
    visible: Boolean,
    playbackState: PlaybackUiState,
    allSongsFallback: List<SongWithAlbum>,
    onDismiss: () -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onSelectFallbackSong: (List<SongWithAlbum>, Int) -> Unit
) {
    if (visible) {
        BackHandler(onBack = onDismiss)
    }

    // Subtle dark scrim on left 30% so underlying screen peeks through cleanly (exact Screenshot 3)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val songsToDisplay = playbackState.queue.ifEmpty { allSongsFallback }
            val usingQueue = playbackState.queue.isNotEmpty()

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.72f)
                    .fillMaxHeight()
                    .background(MsrDrawerBg)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .testTag("playlist_slide_drawer")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top-Right custom angled-hook close arrow (`→` exact Screenshot 3)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MsrHookArrowButton(
                            pointRight = true,
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_playlist_drawer_button")
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 6.dp,
                                bottom = 48.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = songsToDisplay,
                                key = { idx, item -> "drawer_${idx}_${item.song.cid}" }
                            ) { index, item ->
                                val isCurrent = if (usingQueue) {
                                    index == playbackState.currentIndex
                                } else {
                                    playbackState.currentSong?.song?.cid == item.song.cid
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (usingQueue) {
                                                onSelectQueueIndex(index)
                                            } else {
                                                onSelectFallbackSong(songsToDisplay, index)
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrent) MsrPureWhite else MsrWhite,
                                        modifier = Modifier.width(34.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.song.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isCurrent) MsrPureWhite else MsrWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = if (item.isDownloaded) {
                                                "${item.song.artistsJoined}  ·  OFFLINE"
                                            } else {
                                                item.song.artistsJoined
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MsrGrayMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Soft bottom dark fade matching Screenshot 3
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, MsrDrawerBg)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MsrSyncedLyricsList(
    lyrics: List<com.example.data.repository.LyricLine>,
    positionMs: Long,
    onSeekToLine: (Long) -> Unit
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.player_no_lyrics),
                style = MaterialTheme.typography.bodyLarge,
                color = MsrGrayDark
            )
        }
        return
    }

    val activeIndex = LrcParser.findActiveLineIndex(lyrics, positionMs)
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            Text(
                text = line.text,
                style = if (isActive) {
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (isActive) MsrPureWhite else MsrGrayMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeekToLine(line.timestampMs) }
                    .padding(vertical = 2.dp)
            )
        }
    }
}
