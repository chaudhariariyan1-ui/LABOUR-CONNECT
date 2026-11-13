package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = PureWhite,
    primaryContainer = SkyBlueSoft,
    onPrimaryContainer = DeepNavyDark,
    secondary = RoyalBlue,
    onSecondary = PureWhite,
    secondaryContainer = IceBlueContainer,
    onSecondaryContainer = NavyPrimary,
    tertiary = VerifiedGreen,
    onTertiary = PureWhite,
    tertiaryContainer = VerifiedGreenBg,
    onTertiaryContainer = VerifiedGreen,
    background = AlabasterBg,
    onBackground = InkDark,
    surface = PureWhite,
    onSurface = InkDark,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = SlateBorder,
    outlineVariant = ColorTokens.OutlineLight,
    error = DangerRed,
    onError = PureWhite,
    errorContainer = DangerRedBg,
    onErrorContainer = DangerRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = SkyBlueSoft,
    secondary = CobaltAccent,
    onSecondary = PureWhite,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = SkyBlueSoft,
    tertiary = VerifiedGreenBg,
    onTertiary = VerifiedGreen,
    background = DarkBackground,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = SlateBorder,
    outline = SlateSubtle,
    error = DangerRedBg,
    onError = DangerRed
)

private object ColorTokens {
    val OutlineLight = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
}

val ShramSetuShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ShramSetuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ShramSetuTypography,
        shapes = ShramSetuShapes,
        content = content
    )
}
