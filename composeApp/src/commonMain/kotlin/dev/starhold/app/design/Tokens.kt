package dev.starhold.app.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Canonical values mirrored from `starhold-server/design/tokens.json`
 * (see docs/01-UI-SPEC.md). Do not hard-code colours in screens.
 */
object Tokens {
    val Bg = Color(0xFF101010)
    val BgDeep = Color(0xFF080808)
    val Surface = Color(0xFF1A1A1A)
    val Surface2 = Color(0xFF252525)
    val Card = Color(0xFF202020)
    val Line = Color(0x1AFFFFFF)
    val LineStrong = Color(0x2EFFFFFF)
    val Text = Color(0xFFF5F5F5)
    val Muted = Color(0xFFB3B3B3)
    val Faint = Color(0xFF999999)
    val Accent = Color(0xFF6A6CF0)
    val Danger = Color(0xFFCF6679)
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFC107)
    val Scrim = Color(0xCC000000)
    val Glass = Color(0xD11A1A1A)
    val GlassBorder = Color(0x1FFFFFFF)

    object Radius {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 28.dp
        val pill = 999.dp
    }

    object Space {
        val xxs = 2.dp
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
        val xxxl = 48.dp
    }

    object Size {
        val btnSmall = 36.dp
        val btnLarge = 44.dp
        val nav = 64.dp
        val pageMax = 1600.dp
        val card = 150.dp
        val heroMin = 440.dp
        val detailPoster = 156.dp
    }

    const val MOTION_FAST_MS = 140
    const val MOTION_BASE_MS = 180
    const val HERO_ROTATE_MS = 8_000L

    /** Parses `#rrggbb` (the only accent format the server accepts). */
    fun parseHex(value: String?): Color? {
        val v = value?.trim() ?: return null
        if (!Regex("^#[0-9a-fA-F]{6}$").matches(v)) return null
        val rgb = v.substring(1).toLong(16)
        return Color(0xFF000000 or rgb)
    }

    fun toHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return "#" + listOf(r, g, b).joinToString("") { it.toString(16).padStart(2, '0') }
    }
}
