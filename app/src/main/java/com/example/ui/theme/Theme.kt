package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MonsterSirenMonochromeColorScheme = darkColorScheme(
    primary = MsrPureWhite,
    onPrimary = MsrBackgroundDeep,
    primaryContainer = MsrSurfaceElevated,
    onPrimaryContainer = MsrWhite,
    secondary = MsrGrayLight,
    onSecondary = MsrBackgroundDeep,
    secondaryContainer = MsrSurface,
    onSecondaryContainer = MsrWhite,
    tertiary = MsrWhite,
    onTertiary = MsrBackgroundDeep,
    background = MsrBackground,
    onBackground = MsrWhite,
    surface = MsrSurface,
    onSurface = MsrWhite,
    surfaceVariant = MsrSurfaceElevated,
    onSurfaceVariant = MsrGrayMuted,
    surfaceContainerLowest = MsrBackgroundDeep,
    surfaceContainerLow = MsrBackground,
    surfaceContainer = MsrSurface,
    surfaceContainerHigh = MsrSurfaceElevated,
    surfaceContainerHighest = MsrMiniPlayerBg,
    outline = MsrHairline,
    outlineVariant = MsrHairlineDim,
    error = MsrAccentRed,
    onError = MsrPureWhite
)

private val MonsterSirenShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun MonsterSirenTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MonsterSirenMonochromeColorScheme,
        typography = AppTypography,
        shapes = MonsterSirenShapes,
        content = content
    )
}
