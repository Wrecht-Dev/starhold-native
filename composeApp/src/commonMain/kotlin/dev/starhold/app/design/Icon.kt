package dev.starhold.app.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Monochrome 24×24 icon set. Paths are drawn with the same visual language as
 * the web client's inline SVGs (1.7 px round strokes, filled glyphs for media).
 */
enum class AppIcon(val path: String, val filled: Boolean = false) {
    Home("M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z"),
    Film("M3 4h18v16H3zM3 9h18M3 15h18M8 4v16M16 4v16"),
    Tv("M3 7h18v13H3zM8 2l4 4 4-4"),
    Mic("M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3zM5 11a7 7 0 0 0 14 0M12 18v3M8 21h8"),
    Music("M9 18V5l10-2v13M9 18a3 3 0 1 1-6 0 3 3 0 1 1 6 0M19 16a3 3 0 1 1-6 0 3 3 0 1 1 6 0"),
    Book("M4 4h6a4 4 0 0 1 4 4v13a3 3 0 0 0-3-3H4zM20 4h-6a4 4 0 0 0-4 4v13a3 3 0 0 1 3-3h7z"),
    Comic("M4 4h16v12H9l-5 5zM8 8h8M8 12h5"),
    Game("M6 9h12a4 4 0 0 1 4 4v1a3 3 0 0 1-5 2l-2-2H9l-2 2a3 3 0 0 1-5-2v-1a4 4 0 0 1 4-4zM8 11v3M6.5 12.5h3"),
    Grid("M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z"),
    Search("M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14zM16 16l4.5 4.5"),
    User("M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM4 21a8 8 0 0 1 16 0"),
    Star("M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14l-5-4.87 6.91-1.01z"),
    StarFilled("M12 2l3.09 6.26L22 9.27l-5 4.87L18.18 22 12 18.56 5.82 22 7 14.14l-5-4.87 6.91-1.01z", filled = true),
    Check("M4 12l5 5L20 6"),
    CheckCircle("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20zM8 12.5l2.5 2.5 5-5"),
    Close("M6 6l12 12M18 6L6 18"),
    Play("M8 5v14l11-7z", filled = true),
    Pause("M6 5h4v14H6zM14 5h4v14h-4z", filled = true),
    SkipBack("M6 6h2v12H6zM20 6l-9 6 9 6z", filled = true),
    SkipForward("M18 6h-2v12h2zM4 6l9 6-9 6z", filled = true),
    Volume("M4 9h4l5-4v14l-5-4H4zM17 8a6 6 0 0 1 0 8M19.5 5.5a9 9 0 0 1 0 13"),
    Mute("M4 9h4l5-4v14l-5-4H4zM17 9l4 6M21 9l-4 6"),
    Subtitles("M3 5h18v14H3zM6 15h5M13 15h5M6 11h3M11 11h7"),
    Settings("M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6zM12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M19.1 4.9L17 7M7 17l-2.1 2.1"),
    Pip("M3 5h18v14H3zM12 12h7v5h-7z"),
    Fullscreen("M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"),
    Download("M12 3v12M7 10l5 5 5-5M4 21h16"),
    External("M14 3h7v7M21 3l-9 9M10 5H5v14h14v-5"),
    ChevronLeft("M15 5l-7 7 7 7"),
    ChevronRight("M9 5l7 7-7 7"),
    ArrowLeft("M20 12H4M10 6l-6 6 6 6"),
    Clock("M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 7v5l3 2"),
    Plus("M12 5v14M5 12h14"),
    Trash("M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 11v6M14 11v6"),
    Refresh("M20 12a8 8 0 1 1-2.3-5.7M20 4v5h-5"),
    Info("M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 11v6M12 7h.01"),
    Warning("M12 3l10 18H2zM12 9v5M12 17h.01"),
    Lock("M6 11h12v10H6zM9 11V8a3 3 0 0 1 6 0v3"),
    Devices("M3 6h13v10H3zM19 10h2v9h-6v-2"),
    Palette("M12 3a9 9 0 0 0 0 18c1.7 0 2-1 2-2 0-2 1.5-2.5 3-2.5a4 4 0 0 0 4-4C21 6.5 17 3 12 3zM7.5 9h.01M11 6.5h.01M15.5 8h.01"),
    Cast("M3 5h18v14h-7M3 11a7 7 0 0 1 7 7M3 15a3 3 0 0 1 3 3M3 19h.01"),
    List("M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"),
    Filter("M3 5h18M6 12h12M10 19h4"),
    Queue("M4 6h12M4 12h12M4 18h8M18 12v9M18 12l4 2v5"),
    UserCircle("M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18zM12 8a3 3 0 1 0 0 6 3 3 0 0 0 0-6zM6 19a6 6 0 0 1 12 0"),
}

private fun rememberPath(data: String): Path {
    return SvgPathParser(data).parse()
}

@Composable
fun AppIcon(
    icon: AppIcon,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
) {
    val path = remember(icon) { rememberPath(icon.path) }
    Canvas(modifier.size(size)) {
        val strokePx = if (icon.filled) 0f else 1.7.dp.toPx()
        val pad = if (icon.filled) 0f else strokePx / 2f + 0.4.dp.toPx()
        val extent = this.size.minDimension - pad * 2f
        val scale = extent / 24f
        if (scale <= 0f) return@Canvas
        withTransform({
            translate(pad, pad)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            if (icon.filled) {
                drawPath(path, tint, style = Fill)
            } else {
                drawPath(
                    path,
                    tint,
                    style = Stroke(
                        width = strokePx / scale,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

/**
 * Minimal SVG path parser for the icon subset: M L H V C S Q T A Z, absolute
 * and relative. Elliptical arcs are converted to centre parameterisation; icons
 * never use a rotated arc.
 */
internal class SvgPathParser(private val data: String) {
    private var index = 0
    private var currentX = 0f
    private var currentY = 0f
    private var startX = 0f
    private var startY = 0f
    private var lastCubicX = 0f
    private var lastCubicY = 0f
    private var lastQuadX = 0f
    private var lastQuadY = 0f
    private var previousCommand = ' '

    fun parse(): Path {
        val path = Path()
        var command = ' '
        while (true) {
            skipSeparators()
            if (index >= data.length) break
            val ch = data[index]
            val isLetter = ch.isLetter()
            if (isLetter) {
                command = ch
                index++
            } else if (command == ' ' || command == 'Z' || command == 'z') {
                break
            } else {
                // Implicit repetition: after M/m the extra coordinate pairs
                // continue as L/l, as SVG specifies.
                if (command == 'M') command = 'L'
                if (command == 'm') command = 'l'
            }
            when (command) {
                'M' -> moveTo(path, readX(), readY(), relative = false)
                'm' -> moveTo(path, readX(), readY(), relative = true)
                'L' -> lineTo(path, readX(), readY(), relative = false)
                'l' -> lineTo(path, readX(), readY(), relative = true)
                'H' -> horizontalTo(path, readX(), relative = false)
                'h' -> horizontalTo(path, readX(), relative = true)
                'V' -> verticalTo(path, readY(), relative = false)
                'v' -> verticalTo(path, readY(), relative = true)
                'C' -> {
                    val x1 = readX(); val y1 = readY(); val x2 = readX(); val y2 = readY()
                    val x = readX(); val y = readY()
                    cubicTo(path, x1, y1, x2, y2, x, y, relative = false)
                }
                'c' -> {
                    val x1 = readX(); val y1 = readY(); val x2 = readX(); val y2 = readY()
                    val x = readX(); val y = readY()
                    cubicTo(path, x1, y1, x2, y2, x, y, relative = true)
                }
                'S' -> {
                    val x2 = readX(); val y2 = readY(); val x = readX(); val y = readY()
                    smoothCubicTo(path, x2, y2, x, y, relative = false)
                }
                's' -> {
                    val x2 = readX(); val y2 = readY(); val x = readX(); val y = readY()
                    smoothCubicTo(path, x2, y2, x, y, relative = true)
                }
                'Q' -> {
                    val x1 = readX(); val y1 = readY(); val x = readX(); val y = readY()
                    quadTo(path, x1, y1, x, y, relative = false)
                }
                'q' -> {
                    val x1 = readX(); val y1 = readY(); val x = readX(); val y = readY()
                    quadTo(path, x1, y1, x, y, relative = true)
                }
                'T' -> {
                    val x = readX(); val y = readY()
                    smoothQuadTo(path, x, y, relative = false)
                }
                't' -> {
                    val x = readX(); val y = readY()
                    smoothQuadTo(path, x, y, relative = true)
                }
                'A' -> {
                    val rx = readNumber(); val ry = readNumber(); val rotation = readNumber()
                    val large = readNumber() != 0f
                    val sweep = readNumber() != 0f
                    val x = readX(); val y = readY()
                    arcTo(path, rx, ry, rotation, large, sweep, x, y, relative = false)
                }
                'a' -> {
                    val rx = readNumber(); val ry = readNumber(); val rotation = readNumber()
                    val large = readNumber() != 0f
                    val sweep = readNumber() != 0f
                    val x = readX(); val y = readY()
                    arcTo(path, rx, ry, rotation, large, sweep, x, y, relative = true)
                }
                'Z', 'z' -> {
                    path.close()
                    currentX = startX
                    currentY = startY
                }
                else -> return path
            }
            previousCommand = command
        }
        return path
    }

    private fun skipSeparators() {
        while (index < data.length && (data[index] == ' ' || data[index] == ',' || data[index] == '\n' || data[index] == '\r' || data[index] == '\t')) {
            index++
        }
    }

    private fun readNumber(): Float {
        skipSeparators()
        val start = index
        if (index < data.length && (data[index] == '-' || data[index] == '+')) index++
        var seenDot = false
        while (index < data.length) {
            val c = data[index]
            if (c.isDigit()) { index++; continue }
            if (c == '.' && !seenDot) { seenDot = true; index++; continue }
            if ((c == 'e' || c == 'E') && index + 1 < data.length &&
                (data[index + 1].isDigit() || data[index + 1] == '-' || data[index + 1] == '+')
            ) {
                index += 2
                while (index < data.length && data[index].isDigit()) index++
                break
            }
            break
        }
        if (start == index) return 0f
        return data.substring(start, index).toFloatOrNull() ?: 0f
    }

    private fun readX(): Float = readNumber()
    private fun readY(): Float = readNumber()

    private fun moveTo(path: Path, x: Float, y: Float, relative: Boolean) {
        val nx = if (relative) currentX + x else x
        val ny = if (relative) currentY + y else y
        path.moveTo(nx, ny)
        currentX = nx; currentY = ny; startX = nx; startY = ny
    }

    private fun lineTo(path: Path, x: Float, y: Float, relative: Boolean) {
        val nx = if (relative) currentX + x else x
        val ny = if (relative) currentY + y else y
        path.lineTo(nx, ny)
        currentX = nx; currentY = ny
    }

    private fun horizontalTo(path: Path, x: Float, relative: Boolean) {
        val nx = if (relative) currentX + x else x
        path.lineTo(nx, currentY)
        currentX = nx
    }

    private fun verticalTo(path: Path, y: Float, relative: Boolean) {
        val ny = if (relative) currentY + y else y
        path.lineTo(currentX, ny)
        currentY = ny
    }

    private fun cubicTo(
        path: Path,
        x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float,
        relative: Boolean,
    ) {
        val ox = if (relative) currentX else 0f
        val oy = if (relative) currentY else 0f
        path.cubicTo(ox + x1, oy + y1, ox + x2, oy + y2, ox + x, oy + y)
        lastCubicX = ox + x2; lastCubicY = oy + y2
        currentX = ox + x; currentY = oy + y
    }

    private fun smoothCubicTo(path: Path, x2: Float, y2: Float, x: Float, y: Float, relative: Boolean) {
        val ox = if (relative) currentX else 0f
        val oy = if (relative) currentY else 0f
        val reflect = previousCommand in listOf('C', 'c', 'S', 's')
        val x1 = if (reflect) currentX * 2f - lastCubicX else currentX
        val y1 = if (reflect) currentY * 2f - lastCubicY else currentY
        path.cubicTo(x1, y1, ox + x2, oy + y2, ox + x, oy + y)
        lastCubicX = ox + x2; lastCubicY = oy + y2
        currentX = ox + x; currentY = oy + y
    }

    private fun quadTo(path: Path, x1: Float, y1: Float, x: Float, y: Float, relative: Boolean) {
        val ox = if (relative) currentX else 0f
        val oy = if (relative) currentY else 0f
        path.quadraticTo(ox + x1, oy + y1, ox + x, oy + y)
        lastQuadX = ox + x1; lastQuadY = oy + y1
        currentX = ox + x; currentY = oy + y
    }

    private fun smoothQuadTo(path: Path, x: Float, y: Float, relative: Boolean) {
        val ox = if (relative) currentX else 0f
        val oy = if (relative) currentY else 0f
        val reflect = previousCommand in listOf('Q', 'q', 'T', 't')
        val x1 = if (reflect) currentX * 2f - lastQuadX else currentX
        val y1 = if (reflect) currentY * 2f - lastQuadY else currentY
        path.quadraticTo(x1, y1, ox + x, oy + y)
        lastQuadX = x1; lastQuadY = y1
        currentX = ox + x; currentY = oy + y
    }

    private fun arcTo(
        path: Path,
        rxIn: Float, ryIn: Float, rotationDeg: Float,
        largeArc: Boolean, sweep: Boolean,
        x: Float, y: Float, relative: Boolean,
    ) {
        val x2 = if (relative) currentX + x else x
        val y2 = if (relative) currentY + y else y
        var rx = abs(rxIn)
        var ry = abs(ryIn)
        if (rx == 0f || ry == 0f) {
            path.lineTo(x2, y2)
            currentX = x2; currentY = y2
            return
        }
        val phi = rotationDeg * (Math.PI / 180.0)
        val cosPhi = cos(phi).toFloat()
        val sinPhi = sin(phi).toFloat()
        val dx = (currentX - x2) / 2f
        val dy = (currentY - y2) / 2f
        val x1p = cosPhi * dx + sinPhi * dy
        val y1p = -sinPhi * dx + cosPhi * dy
        val lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        if (lambda > 1f) {
            val s = sqrt(lambda)
            rx *= s; ry *= s
        }
        val numerator = max(0f, rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p)
        val denominator = rx * rx * y1p * y1p + ry * ry * x1p * x1p
        val coefficient = if (denominator == 0f) 0f else sqrt(numerator / denominator)
        val sign = if (largeArc != sweep) 1f else -1f
        val cxp = sign * coefficient * rx * y1p / ry
        val cyp = -sign * coefficient * ry * x1p / rx
        val cx = cosPhi * cxp - sinPhi * cyp + (currentX + x2) / 2f
        val cy = sinPhi * cxp + cosPhi * cyp + (currentY + y2) / 2f
        val startVectorX = (x1p - cxp) / rx
        val startVectorY = (y1p - cyp) / ry
        val endVectorX = (-x1p - cxp) / rx
        val endVectorY = (-y1p - cyp) / ry
        var startAngle = atan2(startVectorY, startVectorX)
        var extent = atan2(endVectorX * startVectorY - endVectorY * startVectorX, endVectorX * startVectorX + endVectorY * startVectorY)
        if (!sweep && extent > 0) extent -= (2 * Math.PI).toFloat()
        if (sweep && extent < 0) extent += (2 * Math.PI).toFloat()
        val rect = Rect(cx - rx, cy - ry, cx + rx, cy + ry)
        path.arcTo(
            rect,
            startAngle * (180f / Math.PI).toFloat(),
            extent * (180f / Math.PI).toFloat(),
            forceMoveTo = false,
        )
        currentX = x2; currentY = y2
        lastCubicX = x2; lastCubicY = y2
    }
}
