package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.DownloadStatus
import com.example.player.PlaybackUiState
import com.example.player.RepeatMode as PlayerRepeatMode
import com.example.ui.theme.MsrAccentRed
import com.example.ui.theme.MsrBackground
import com.example.ui.theme.MsrBackgroundDeep
import com.example.ui.theme.MsrGrayDark
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrGrayMuted
import com.example.ui.theme.MsrHairline
import com.example.ui.theme.MsrHairlineDim
import com.example.ui.theme.MsrHalftoneDot
import com.example.ui.theme.MsrMiniPlayerBg
import com.example.ui.theme.MsrMonoMicro
import com.example.ui.theme.MsrPureWhite
import com.example.ui.theme.MsrSearchPillBg
import com.example.ui.theme.MsrSurfaceElevated
import com.example.ui.theme.MsrWhite
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Exact Monster Siren charcoal-black background with organic halftone dot-matrix waves
 * visible across all official mobile screens.
 */
@Composable
fun MsrHalftoneBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MsrBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 11.dp.toPx()
            val cols = (size.width / step).toInt() + 1
            val rows = (size.height / step).toInt() + 1

            for (r in 0..rows) {
                val y = r * step
                val ny = y / size.height.coerceAtLeast(1f)
                for (c in 0..cols) {
                    val x = c * step
                    val nx = x / size.width.coerceAtLeast(1f)

                    // Diagonal flowing halftone clouds matching MSR web texture
                    val wave1 = sin((nx * 3.8f + ny * 2.4f) * PI.toFloat())
                    val wave2 = cos((nx * 2.1f - ny * 4.5f) * PI.toFloat())
                    val wave3 = sin((ny * 6.0f + nx * 1.2f) * PI.toFloat())
                    val density = (wave1 * 0.45f + wave2 * 0.35f + wave3 * 0.20f)

                    if (density > 0.16f) {
                        val intensity = ((density - 0.16f) / 0.84f).coerceIn(0f, 1f)
                        val radius = (0.7f + intensity * 1.45f).dp.toPx()
                        drawCircle(
                            color = MsrHalftoneDot.copy(alpha = 0.25f + intensity * 0.55f),
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        content()
    }
}

/**
 * Offline-first raw square cover image with zero rounded corners.
 */
@Composable
fun CoverArtImage(
    localPath: String?,
    remoteUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val modelData = remember(localPath, remoteUrl) {
        val localFile = localPath?.takeIf { it.isNotBlank() }?.let { File(it) }
        when {
            localFile != null && localFile.exists() -> localFile
            !remoteUrl.isNullOrBlank() -> remoteUrl
            else -> R.drawable.ic_msr_cover_placeholder
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(modelData)
            .crossfade(true)
            .build(),
        placeholder = painterResource(R.drawable.ic_msr_cover_placeholder),
        error = painterResource(R.drawable.ic_msr_cover_placeholder),
        fallback = painterResource(R.drawable.ic_msr_cover_placeholder),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

/**
 * Exact pill-shaped dark search bar ("A WORLD FAMILIARLY UNKNOWN" + magnifying glass)
 * from top of Screenshots 1 & 4.
 */
@Composable
fun MsrSearchPillBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(MsrSearchPillBg)
            .border(1.dp, MsrHairlineDim, RoundedCornerShape(50))
            .padding(start = 18.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "A WORLD FAMILIARLY UNKNOWN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MsrGrayMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MsrWhite),
                cursorBrush = SolidColor(MsrPureWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input")
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_clear_search),
                tint = MsrGrayLight,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onQueryChange("") }
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_search),
                tint = MsrGrayLight,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Exact 3-thin-line hamburger menu icon (top-right of Screenshots 1, 2, 4).
 */
@Composable
fun MsrHamburgerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hidden: Boolean = false
) {
    val buttonAlpha by animateFloatAsState(
        targetValue = if (hidden) 0f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "hamburger_alpha"
    )
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer { alpha = buttonAlpha }
            .clickable(enabled = !hidden, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 24.dp, height = 18.dp)) {
            val stroke = 1.6.dp.toPx()
            val y1 = stroke
            val y2 = size.height / 2f
            val y3 = size.height - stroke
            drawLine(MsrWhite, Offset(0f, y1), Offset(size.width, y1), stroke)
            drawLine(MsrWhite, Offset(0f, y2), Offset(size.width, y2), stroke)
            drawLine(MsrWhite, Offset(0f, y3), Offset(size.width, y3), stroke)
        }
    }
}

/**
 * Exact Monster Siren custom angled-hook horizontal arrow (left-pointing in Screenshots 2 & 4,
 * right-pointing in Screenshot 3 playlist drawer).
 */
@Composable
fun MsrHookArrowButton(
    pointRight: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 34.dp, height = 16.dp)) {
            val stroke = 1.8.dp.toPx()
            val baseY = size.height * 0.72f
            val hookTopY = size.height * 0.15f
            if (!pointRight) {
                // Horizontal line + upward-right angled hook on the left tip (←)
                drawLine(MsrWhite, Offset(0f, baseY), Offset(size.width, baseY), stroke, StrokeCap.Square)
                drawLine(MsrWhite, Offset(0f, baseY), Offset(size.width * 0.28f, hookTopY), stroke, StrokeCap.Square)
            } else {
                // Horizontal line + upward-left angled hook on the right tip (→)
                drawLine(MsrWhite, Offset(0f, baseY), Offset(size.width, baseY), stroke, StrokeCap.Square)
                drawLine(MsrWhite, Offset(size.width, baseY), Offset(size.width * 0.72f, hookTopY), stroke, StrokeCap.Square)
            }
        }
    }
}

/**
 * Exact stacked Album Sleeve + Vinyl Hole icon (beside ALBUMS & MUSIC in Screenshots 1 & 4).
 */
@Composable
fun MsrAlbumSleeveIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        // Top thin sleeve edge lines
        drawLine(MsrGrayLight, Offset(w * 0.18f, h * 0.10f), Offset(w * 0.88f, h * 0.10f), 1.3.dp.toPx())
        drawLine(MsrGrayLight, Offset(w * 0.12f, h * 0.22f), Offset(w * 0.92f, h * 0.22f), 1.3.dp.toPx())
        // Main white square sleeve
        val top = h * 0.30f
        val rectW = w * 0.78f
        val rectH = h * 0.66f
        val left = (w - rectW) / 2f
        drawRect(
            color = MsrWhite,
            topLeft = Offset(left, top),
            size = Size(rectW, rectH)
        )
        // Center black vinyl hole
        drawCircle(
            color = MsrBackground,
            radius = rectH * 0.25f,
            center = Offset(left + rectW / 2f, top + rectH / 2f)
        )
    }
}

/**
 * Exact 4-node network icon (beside INFO in Screenshot 4).
 */
@Composable
fun MsrInfoNodesIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.36f
        val stroke = 1.4.dp.toPx()

        val corners = listOf(
            Offset(cx - r, cy - r),
            Offset(cx + r, cy - r),
            Offset(cx - r, cy + r),
            Offset(cx + r, cy + r)
        )
        corners.forEach { pt ->
            drawLine(MsrGrayLight, Offset(cx, cy), pt, stroke)
            drawRect(
                color = MsrWhite,
                topLeft = Offset(pt.x - 2.dp.toPx(), pt.y - 2.dp.toPx()),
                size = Size(4.dp.toPx(), 4.dp.toPx())
            )
        }
        drawRect(
            color = MsrBackground,
            topLeft = Offset(cx - 4.5.dp.toPx(), cy - 4.5.dp.toPx()),
            size = Size(9.dp.toPx(), 9.dp.toPx())
        )
        drawRect(
            color = MsrWhite,
            topLeft = Offset(cx - 4.5.dp.toPx(), cy - 4.5.dp.toPx()),
            size = Size(9.dp.toPx(), 9.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * Exact horizontal hairline ending on the right with diagonal slash hatches `\\\\\\\`
 * (Screenshots 1 & 4).
 */
@Composable
fun MsrSectionDividerWithHatches(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val midY = size.height / 2f
        val hatchAreaWidth = 46.dp.toPx()
        val lineEndX = (size.width - hatchAreaWidth - 6.dp.toPx()).coerceAtLeast(0f)

        drawLine(
            color = MsrHairline,
            start = Offset(0f, midY),
            end = Offset(lineEndX, midY),
            strokeWidth = 1.dp.toPx()
        )

        val hatchCount = 7
        val spacing = hatchAreaWidth / hatchCount
        for (i in 0 until hatchCount) {
            val x = lineEndX + 6.dp.toPx() + i * spacing
            drawLine(
                color = MsrGrayMuted,
                start = Offset(x, midY - 3.5.dp.toPx()),
                end = Offset(x + 4.dp.toPx(), midY + 3.5.dp.toPx()),
                strokeWidth = 1.2.dp.toPx()
            )
        }
    }
}

/**
 * Exact MSR decorative wave line with hollow node `◎` on the left emitting intertwined sine curves
 * (Album Detail Screenshot 2 & Player Lyrics Screenshot 3).
 */
@Composable
fun MsrWaveNodeDivider(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(22.dp)
    ) {
        val cy = size.height / 2f
        val nodeR = 4.5.dp.toPx()
        val startX = nodeR + 1.dp.toPx()

        // Outer hollow circle + inner dot
        drawCircle(
            color = MsrGrayMuted,
            radius = nodeR,
            center = Offset(startX, cy),
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = MsrGrayLight,
            radius = 2.dp.toPx(),
            center = Offset(startX, cy)
        )

        val waveStart = startX + nodeR
        val waveLen = (size.width - waveStart).coerceAtLeast(1f)

        // 3 graceful intertwined sine curves fading to right
        val amplitudes = listOf(5.dp.toPx(), 3.2.dp.toPx(), 1.8.dp.toPx())
        val phases = listOf(0f, 0.7f, 1.5f)
        val alphas = listOf(0.55f, 0.35f, 0.22f)

        amplitudes.forEachIndexed { idx, amp ->
            val path = Path().apply {
                moveTo(waveStart, cy)
                val steps = 64
                for (i in 1..steps) {
                    val t = i.toFloat() / steps
                    val x = waveStart + t * waveLen
                    val fade = (1f - t).coerceIn(0f, 1f)
                    val y = cy + sin(t * 2.2f * PI.toFloat() + phases[idx]) * amp * fade
                    lineTo(x, y)
                }
            }
            drawPath(
                path = path,
                color = MsrGrayLight.copy(alpha = alphas[idx]),
                style = Stroke(width = 0.9.dp.toPx())
            )
        }
    }
}

/**
 * Exact right-edge vertical diagonal-tick scrollbar decoration from Screenshot 1.
 */
@Composable
fun MsrVerticalHatchScrollbar(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.width(8.dp)) {
        val step = 5.5.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = MsrGrayDark.copy(alpha = 0.75f),
                start = Offset(0f, y),
                end = Offset(size.width, y + 3.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
            y += step
        }
    }
}

/**
 * Exact bottom-right "SCROLL / <<< " editorial marker from Screenshot 1.
 */
@Composable
fun MsrScrollFooterMarker(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "SCROLL",
            style = MaterialTheme.typography.labelSmall,
            color = MsrGrayLight,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(0.55f)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MsrHairline)
            )
            Text(
                text = " <<< ",
                style = MsrMonoMicro,
                color = MsrGrayLight
            )
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(1.dp)
                    .background(MsrHairline)
            )
        }
    }
}

/**
 * Exact `[ |||  PLAY ]` sharp outlined rectangular button from Screenshot 2.
 */
@Composable
fun MsrOutlinedPlayButton(
    label: String = "PLAY",
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .background(MsrBackgroundDeep.copy(alpha = 0.65f))
            .border(1.dp, MsrGrayLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        AudioWaveIndicator(isPlaying = isPlaying, color = MsrWhite)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MsrWhite
        )
    }
}

/**
 * Exact persistent bottom mini-player bar from Screenshots 1, 2, 4:
 * #1D1D1D fill, flush 54dp cover on left, bold white track title, white Play triangle + Playlist list icon.
 */
@Composable
fun MsrPersistentMiniPlayer(
    playbackState: PlaybackUiState,
    onOpenFullPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenPlaylistDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong
    val titleText = song?.song?.name ?: "MONSTER SIREN"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MsrMiniPlayerBg)
            .navigationBarsPadding()
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clickable(enabled = song != null, onClick = onOpenFullPlayer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flush square album thumbnail touching left edge
            CoverArtImage(
                localPath = song?.albumLocalCoverPath,
                remoteUrl = song?.albumCoverUrl,
                contentDescription = stringResource(R.string.cd_album_cover),
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = titleText.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MsrWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (playbackState.isBuffering) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MsrWhite,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                IconButton(
                    onClick = onTogglePlayPause,
                    enabled = song != null,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(
                            if (playbackState.isPlaying) R.string.cd_pause else R.string.cd_play
                        ),
                        tint = if (song != null) MsrPureWhite else MsrGrayDark,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            IconButton(
                onClick = onOpenPlaylistDrawer,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mini_player_playlist_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = stringResource(R.string.cd_queue),
                    tint = MsrWhite,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

/**
 * Exact dark matte rotating Vinyl Disc with circular album artwork & animated left tonearm
 * matching Screenshot 5: when playing, the tonearm cartridge swings smoothly onto the vinyl grooves;
 * when paused/stopped, it retracts cleanly off the vinyl to the left edge.
 */
@Composable
fun MsrVinylTurntableDisc(
    localCoverPath: String?,
    remoteCoverUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Preserve exact vinyl angle when paused instead of snapping back to 0°
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                val start = rotation.value % 360f
                rotation.snapTo(start)
                rotation.animateTo(
                    targetValue = start + 360f,
                    animationSpec = tween(durationMillis = 18_000, easing = LinearEasing)
                )
            }
        }
    }

    // Smoothly swing tonearm cartridge onto the vinyl when playing, and pull away when stopped
    val tonearmSweepDegrees by animateFloatAsState(
        targetValue = if (isPlaying) 27f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "tonearm_sweep"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.02f),
        contentAlignment = Alignment.Center
    ) {
        // Matte charcoal rotating vinyl platter
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(1f)
                .graphicsLayer {
                    rotationZ = rotation.value
                }
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFF141414),
                            Color(0xFF232323),
                            Color(0xFF111111),
                            Color(0xFF262626),
                            Color(0xFF141414)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Subtle vinyl specular grooves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxR = size.minDimension / 2f
                listOf(0.95f, 0.89f, 0.83f, 0.77f, 0.71f, 0.65f).forEach { factor ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.045f),
                        radius = maxR * factor,
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }
            }

            // Centered circular album cover artwork
            CoverArtImage(
                localPath = localCoverPath,
                remoteUrl = remoteCoverUrl,
                contentDescription = stringResource(R.string.cd_album_cover),
                modifier = Modifier
                    .fillMaxSize(0.56f)
                    .clip(CircleShape)
            )
        }

        // Animated tonearm & cartridge ("стручок") drawn ON TOP of the vinyl platter
        Canvas(modifier = Modifier.matchParentSize()) {
            val pivot = Offset(x = -size.width * 0.04f, y = size.height * 0.60f)
            val armEnd = Offset(x = size.width * 0.085f, y = size.height * 0.43f)
            val headW = 16.dp.toPx()
            val headH = 32.dp.toPx()

            // Base angle positions the cartridge off the vinyl on the left (matching paused Screenshot 5);
            // +27° clockwise smoothly places the cartridge squarely on top of the spinning vinyl grooves.
            rotate(degrees = tonearmSweepDegrees, pivot = pivot) {
                // Soft drop shadow beneath arm & cartridge when over vinyl
                if (tonearmSweepDegrees > 1f) {
                    val shadowOffset = Offset(3.dp.toPx(), 5.dp.toPx())
                    drawLine(
                        color = Color.Black.copy(alpha = 0.45f),
                        start = pivot + shadowOffset,
                        end = armEnd + shadowOffset,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Slim metallic tonearm rod
                drawLine(
                    color = Color(0xFF343434),
                    start = pivot,
                    end = armEnd,
                    strokeWidth = 3.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF525252),
                    start = pivot,
                    end = armEnd,
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Cartridge head angled along the arm tip
                rotate(degrees = 36f, pivot = armEnd) {
                    val headTopLeft = Offset(
                        x = armEnd.x - headW / 2f,
                        y = armEnd.y - headH * 0.82f
                    )
                    // Drop shadow for cartridge head
                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.45f),
                        topLeft = headTopLeft + Offset(2.dp.toPx(), 3.dp.toPx()),
                        size = Size(headW, headH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    // Main dark anodized cartridge body
                    drawRoundRect(
                        color = Color(0xFF2B2B2B),
                        topLeft = headTopLeft,
                        size = Size(headW, headH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    // Subtle metallic rim highlight
                    drawRoundRect(
                        color = Color(0xFF4A4A4A),
                        topLeft = headTopLeft,
                        size = Size(headW, headH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    // Signature 3 parallel dark grip/vent grooves on cartridge head (Screenshot 5)
                    val grooveInset = 3.5.dp.toPx()
                    listOf(0.28f, 0.45f, 0.62f).forEach { frac ->
                        val gy = headTopLeft.y + headH * frac
                        drawLine(
                            color = Color(0xFF171717),
                            start = Offset(headTopLeft.x + grooveInset, gy),
                            end = Offset(headTopLeft.x + headW - grooveInset, gy),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exact loop-with-A / Repeat icon from Screenshots 3 & 5 bottom-left.
 */
@Composable
fun MsrRepeatModeGlyph(
    repeatMode: PlayerRepeatMode,
    shuffleEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 1.6.dp.toPx()
            val color = if (repeatMode != PlayerRepeatMode.OFF || shuffleEnabled) MsrPureWhite else MsrGrayMuted
            val w = size.width
            val h = size.height
            // Top loop arc/line with right arrowhead
            val topY = h * 0.28f
            val botY = h * 0.72f
            drawLine(color, Offset(w * 0.22f, topY), Offset(w * 0.78f, topY), stroke)
            drawLine(color, Offset(w * 0.66f, topY - 4.dp.toPx()), Offset(w * 0.78f, topY), stroke)
            drawLine(color, Offset(w * 0.66f, topY + 4.dp.toPx()), Offset(w * 0.78f, topY), stroke)
            drawLine(color, Offset(w * 0.16f, topY + 4.dp.toPx()), Offset(w * 0.16f, botY - 4.dp.toPx()), stroke)

            // Bottom loop line with left arrowhead
            drawLine(color, Offset(w * 0.22f, botY), Offset(w * 0.78f, botY), stroke)
            drawLine(color, Offset(w * 0.34f, botY - 4.dp.toPx()), Offset(w * 0.22f, botY), stroke)
            drawLine(color, Offset(w * 0.34f, botY + 4.dp.toPx()), Offset(w * 0.22f, botY), stroke)
            drawLine(color, Offset(w * 0.84f, topY + 4.dp.toPx()), Offset(w * 0.84f, botY - 4.dp.toPx()), stroke)
        }

        val modeChar = when {
            shuffleEnabled -> "S"
            repeatMode == PlayerRepeatMode.ONE -> "1"
            else -> "A"
        }
        Text(
            text = modeChar,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (repeatMode != PlayerRepeatMode.OFF || shuffleEnabled) MsrPureWhite else MsrGrayMuted
        )
    }
}

/**
 * Subtle monochrome offline download button blending cleanly into Monster Siren's grayscale palette.
 */
@Composable
fun DownloadActionButton(
    downloadStatus: Int,
    downloadProgress: Int,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (downloadStatus) {
        DownloadStatus.DOWNLOADED -> {
            IconButton(
                onClick = onDelete,
                modifier = modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.DownloadDone,
                    contentDescription = stringResource(R.string.cd_delete_download),
                    tint = MsrPureWhite,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        DownloadStatus.DOWNLOADING -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onCancel)
                    .padding(4.dp)
            ) {
                if (downloadProgress > 0) {
                    CircularProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.size(22.dp),
                        color = MsrPureWhite,
                        trackColor = MsrHairlineDim,
                        strokeWidth = 2.dp
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MsrPureWhite,
                        trackColor = MsrHairlineDim,
                        strokeWidth = 2.dp
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = MsrWhite,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
        DownloadStatus.FAILED -> {
            IconButton(
                onClick = onDownload,
                modifier = modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = stringResource(R.string.retry_button),
                    tint = MsrAccentRed,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        else -> {
            IconButton(
                onClick = onDownload,
                modifier = modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.cd_download),
                    tint = MsrGrayDark,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Soundwave bars icon (used inside `[ ||| PLAY ]` button and bottom-right of Full Player).
 */
@Composable
fun AudioWaveIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MsrWhite
) {
    val transition = rememberInfiniteTransition(label = "msr_eq")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse), label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 1.0f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse), label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.45f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(380, easing = LinearEasing), RepeatMode.Reverse), label = "b3"
    )

    Canvas(modifier = modifier.size(width = 16.dp, height = 16.dp)) {
        val heights = if (isPlaying) listOf(bar1, bar2, bar3) else listOf(0.45f, 1.0f, 0.55f)
        val strokeW = 3.dp.toPx()
        val gap = (size.width - strokeW * 3) / 2f
        heights.forEachIndexed { idx, h ->
            val x = strokeW / 2f + idx * (strokeW + gap)
            val barH = size.height * h
            val top = (size.height - barH) / 2f
            drawLine(
                color = color,
                start = Offset(x, top),
                end = Offset(x, top + barH),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}
