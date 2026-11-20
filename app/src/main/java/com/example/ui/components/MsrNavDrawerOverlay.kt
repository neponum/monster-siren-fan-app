package com.example.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.MontserratFontFamily
import com.example.ui.theme.MsrGrayLight
import com.example.ui.theme.MsrGrayMuted
import com.example.ui.theme.MsrHairline
import com.example.ui.theme.MsrWhite

data class MsrNavMenuItem(
    val route: String,
    val primaryLabel: String,
    val secondaryLabel: String,
    val badgeText: String? = null
)

/**
 * Exact Monster Siren left-side frosted-glass navigation drawer overlay:
 * - Left ~70% frosted blur panel over the underlying page (blurred ONLY behind the left panel)
 * - Right ~30% sharp dimmed strip revealing the current page with top-right crisp `X` close button
 * - Sits directly above the persistent #1D1D1D mini-player bar
 * - Bold uppercase English tab on left + localized label on right, separated by hatched hairlines
 * - Footer tagline "A WORLD FAMILIARLY UNKNOWN" anchored at the bottom of the left panel
 */
@Composable
fun MsrSlideOverNavMenu(
    visible: Boolean,
    activeRoute: String,
    items: List<MsrNavMenuItem>,
    backdropLayer: GraphicsLayer?,
    onSelectRoute: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "msr_nav_drawer_progress"
    )

    if (visible) {
        BackHandler(onBack = onDismiss)
    }

    if (progress <= 0.001f) return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("msr_nav_drawer_overlay")
    ) {
        val totalWidth = maxWidth
        val panelFraction = 0.70f
        val panelWidth = totalWidth * panelFraction
        val revealedWidth = panelWidth * progress

        // Right-side scrim covering the full screen (tapping right 30% closes the menu cleanly)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.24f * progress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Left 70% Frosted-Glass Curtain (clipped strictly to left panel so right 30% stays 100% sharp!)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(revealedWidth)
                .fillMaxHeight()
                .clipToBounds()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // absorb clicks inside panel
                )
        ) {
            // Real-time GPU blurred backdrop of the underlying screen (API 31+)
            if (backdropLayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Canvas(
                    modifier = Modifier
                        .width(totalWidth)
                        .fillMaxHeight()
                        .blur(radius = 22.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                ) {
                    drawLayer(backdropLayer)
                }
            }

            // Frosted dark glass tint overlay (matches screenshot's deep smoky blur translucency)
            val tintAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.72f else 0.94f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0A0A0A).copy(alpha = (tintAlpha + 0.06f).coerceAtMost(0.96f)),
                                Color(0xFF0D0D0D).copy(alpha = tintAlpha),
                                Color(0xFF0B0B0B).copy(alpha = (tintAlpha + 0.04f).coerceAtMost(0.96f))
                            )
                        )
                    )
            )

            // Subtle vertical hairline on the right edge of the frosted panel
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(0.8.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.07f * progress))
            )

            // Menu content sliding smoothly into place inside the fixed-width 70% column
            Column(
                modifier = Modifier
                    .width(panelWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = -(1f - progress) * size.width * 0.35f
                        alpha = progress
                    }
                    .statusBarsPadding()
            ) {
                // Generous top breathing room matching the screenshot's vertical composition
                Spacer(modifier = Modifier.weight(0.95f))

                // Stacked navigation items separated ONLY between items by hatched dividers
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items.forEachIndexed { index, item ->
                        val isActive = activeRoute == item.route ||
                            (item.route == "music" && activeRoute.startsWith("album/"))

                        MsrNavDrawerRow(
                            item = item,
                            isActive = isActive,
                            onClick = { onSelectRoute(item.route) }
                        )

                        if (index < items.lastIndex) {
                            MsrDrawerHatchedDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1.25f))

                // Bottom footer tagline right above the mini-player (exact screenshot placement)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A WORLD FAMILIARLY UNKNOWN",
                        fontFamily = MontserratFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = 2.4.sp,
                        color = MsrGrayMuted.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        // Top-right crisp `X` close button in the exact spot of the hamburger icon
        val closeCd = stringResource(R.string.cd_close_menu)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 8.dp, top = 10.dp)
                .minimumInteractiveComponentSize()
                .graphicsLayer {
                    alpha = progress
                    rotationZ = (1f - progress) * -45f
                }
                .semantics {
                    contentDescription = closeCd
                }
                .clickable(
                    role = Role.Button,
                    onClick = onDismiss
                )
                .testTag("msr_nav_drawer_close_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val stroke = 1.6.dp.toPx()
                val inset = 1.dp.toPx()
                drawLine(
                    color = MsrWhite,
                    start = Offset(inset, inset),
                    end = Offset(size.width - inset, size.height - inset),
                    strokeWidth = stroke,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = MsrWhite,
                    start = Offset(size.width - inset, inset),
                    end = Offset(inset, size.height - inset),
                    strokeWidth = stroke,
                    cap = StrokeCap.Square
                )
            }
        }
    }
}

@Composable
private fun MsrNavDrawerRow(
    item: MsrNavMenuItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Matches screenshot: currently active route is muted/dimmed gray while navigable items are bright white
    val primaryColor = if (isActive) MsrGrayMuted else MsrWhite
    val secondaryColor = if (isActive) MsrGrayMuted.copy(alpha = 0.8f) else MsrWhite.copy(alpha = 0.92f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 22.dp)
            .testTag("nav_drawer_item_${item.route}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.primaryLabel,
            fontFamily = MontserratFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            letterSpacing = 1.2.sp,
            color = primaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.15f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(0.85f)
        ) {
            Text(
                text = item.secondaryLabel,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 0.4.sp,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.badgeText.isNullOrBlank()) {
                Text(
                    text = item.badgeText,
                    fontFamily = MontserratFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = MsrGrayLight.copy(alpha = if (isActive) 0.55f else 0.78f),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Exact horizontal hairline running flush from the left edge and ending with 7 diagonal `\\\\\\\` hatches
 * near the right edge of the left frosted panel (matching the uploaded menu screenshot).
 */
@Composable
private fun MsrDrawerHatchedDivider(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val midY = size.height / 2f
        val rightMargin = 10.dp.toPx()
        val hatchAreaWidth = 42.dp.toPx()
        val lineEndX = (size.width - rightMargin - hatchAreaWidth - 6.dp.toPx()).coerceAtLeast(0f)

        drawLine(
            color = MsrHairline.copy(alpha = 0.55f),
            start = Offset(0f, midY),
            end = Offset(lineEndX, midY),
            strokeWidth = 0.9.dp.toPx()
        )

        val hatchCount = 7
        val spacing = hatchAreaWidth / hatchCount
        for (i in 0 until hatchCount) {
            val x = lineEndX + 6.dp.toPx() + i * spacing
            drawLine(
                color = MsrGrayMuted.copy(alpha = 0.75f),
                start = Offset(x, midY - 3.dp.toPx()),
                end = Offset(x + 3.5.dp.toPx(), midY + 3.dp.toPx()),
                strokeWidth = 1.1.dp.toPx()
            )
        }
    }
}
