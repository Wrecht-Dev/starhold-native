package dev.starhold.app.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.starhold.app.resources.Res
import dev.starhold.app.resources.manrope_400
import dev.starhold.app.resources.manrope_600
import dev.starhold.app.resources.manrope_700
import dev.starhold.app.resources.manrope_800
import dev.starhold.core.model.UiPreferences
import org.jetbrains.compose.resources.Font

@Immutable
data class StarholdColors(
    val bg: Color = Tokens.Bg,
    val bgDeep: Color = Tokens.BgDeep,
    val surface: Color = Tokens.Surface,
    val surface2: Color = Tokens.Surface2,
    val card: Color = Tokens.Card,
    val line: Color = Tokens.Line,
    val lineStrong: Color = Tokens.LineStrong,
    val text: Color = Tokens.Text,
    val muted: Color = Tokens.Muted,
    val faint: Color = Tokens.Faint,
    val accent: Color = Tokens.Accent,
    val danger: Color = Tokens.Danger,
    val success: Color = Tokens.Success,
    val warning: Color = Tokens.Warning,
    val scrim: Color = Tokens.Scrim,
    val glass: Color = Tokens.Glass,
    val glassBorder: Color = Tokens.GlassBorder,
)

@Immutable
data class StarholdType(
    val display: TextStyle,
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val body: TextStyle,
    val small: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle,
    val hero: TextStyle,
)

val LocalStarholdColors = staticCompositionLocalOf { StarholdColors() }
val LocalStarholdType = staticCompositionLocalOf { defaultType(FontFamily.SansSerif) }
val LocalStarholdSpacing = staticCompositionLocalOf { 1f }
val LocalIsTelevision = staticCompositionLocalOf { false }

/** Scales a dimension by the account's density preference. */
@Composable
fun space(value: androidx.compose.ui.unit.Dp): androidx.compose.ui.unit.Dp =
    value * LocalStarholdSpacing.current

private fun defaultType(family: FontFamily): StarholdType = StarholdType(
    display = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    h1 = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.3).sp),
    h2 = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.2).sp),
    h3 = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.1).sp),
    body = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    small = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    label = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp, lineHeight = 17.sp),
    caption = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp),
    mono = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
    hero = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, lineHeight = 55.sp, letterSpacing = (-2).sp),
)

@Composable
fun StarholdTheme(
    preferences: UiPreferences = UiPreferences(),
    content: @Composable () -> Unit,
) {
    val accent = Tokens.parseHex(preferences.accentColor) ?: Tokens.Accent
    val colors = remember(accent) { StarholdColors(accent = accent) }

    val manrope = FontFamily(
        Font(Res.font.manrope_400, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.manrope_600, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.manrope_700, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.manrope_800, FontWeight.ExtraBold, FontStyle.Normal),
    )
    val type = remember(manrope) { defaultType(manrope) }
    val scale = if (preferences.density == "compact") 0.86f else 1f

    val materialType = remember(type) {
        Typography(
            displayLarge = type.hero,
            headlineLarge = type.display,
            headlineMedium = type.h2,
            titleLarge = type.h3,
            titleMedium = type.body,
            bodyLarge = type.body,
            bodyMedium = type.small,
            bodySmall = type.caption,
            labelLarge = type.label,
        )
    }

    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        background = colors.bg,
        onBackground = colors.text,
        surface = colors.surface,
        onSurface = colors.text,
        surfaceVariant = colors.surface2,
        onSurfaceVariant = colors.muted,
        outline = colors.lineStrong,
        error = colors.danger,
    )

    CompositionLocalProvider(
        LocalStarholdColors provides colors,
        LocalStarholdType provides type,
        LocalStarholdSpacing provides scale,
    ) {
        MaterialTheme(colorScheme = scheme, typography = materialType, content = content)
    }
}

/** Convenience accessors used throughout the UI. */
val starholdColors: StarholdColors
    @Composable get() = LocalStarholdColors.current

val starholdType: StarholdType
    @Composable get() = LocalStarholdType.current

@Composable
fun isDarkTheme(): Boolean = isSystemInDarkTheme()

val navHeight = 64.dp
