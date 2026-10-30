package dev.starhold.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType

@Composable
fun Spinner(size: Dp = 20.dp, color: Color = starholdColors.accent) {
    CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp,
    )
}

@Composable
fun Wordmark(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        AppIcon(AppIcon.StarFilled, size = size, tint = starholdColors.accent)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "STARHOLD",
            style = starholdType.h3,
            color = starholdColors.text,
            letterSpacing = 2.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: AppIcon? = null,
    large: Boolean = false,
) {
    val colors = starholdColors
    Surface(
        modifier = modifier
            .heightIn(min = if (large) Tokens.Size.btnLarge else 40.dp)
            .clip(RoundedCornerShape(Tokens.Radius.sm))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) colors.accent else colors.surface2,
        contentColor = if (enabled) Color.White else colors.faint,
        shape = RoundedCornerShape(Tokens.Radius.sm),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                AppIcon(icon, size = 16.dp, tint = if (enabled) Color.White else colors.faint)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = starholdType.small, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: AppIcon? = null,
) {
    val colors = starholdColors
    Surface(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(Tokens.Radius.sm))
            .clickable(enabled = enabled, onClick = onClick),
        color = Color.White.copy(alpha = 0.06f),
        contentColor = colors.text,
        shape = RoundedCornerShape(Tokens.Radius.sm),
        border = BorderStroke(1.dp, colors.lineStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                AppIcon(icon, size = 16.dp, tint = colors.text)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = starholdType.small, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun IconButton(
    icon: AppIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    size: Dp = Tokens.Size.btnSmall,
    iconSize: Dp = 20.dp,
    background: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(Tokens.Radius.sm))
            .background(background)
            .clickable(onClick = onClick)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon, size = iconSize, tint = tint)
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = starholdType.h3, color = starholdColors.text)
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun PageTitle(text: String, subtitle: String? = null) {
    Column(Modifier.padding(bottom = 20.dp)) {
        Text(text, style = starholdType.display, color = starholdColors.text)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = starholdType.small, color = starholdColors.muted)
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = starholdType.h2, color = starholdColors.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = starholdType.small,
            color = starholdColors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(420.dp),
        )
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

@Composable
fun ErrorNotice(message: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(AppIcon.Warning, size = 18.dp, tint = starholdColors.danger)
            Spacer(Modifier.width(8.dp))
            Text(message, style = starholdType.small, color = starholdColors.danger)
        }
        if (onRetry != null) {
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Try again", onRetry, icon = AppIcon.Refresh)
        }
    }
}

@Composable
fun StarholdTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardType?.let { KeyboardOptions(keyboardType = it) } ?: KeyboardOptions.Default,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.Radius.sm),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = starholdColors.accent,
            unfocusedBorderColor = starholdColors.lineStrong,
            focusedLabelColor = starholdColors.accent,
            unfocusedLabelColor = starholdColors.muted,
            cursorColor = starholdColors.accent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
    )
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, count: Int? = null) {
    val colors = starholdColors
    Surface(
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick),
        color = if (selected) colors.accent.copy(alpha = 0.18f) else Color.Transparent,
        contentColor = if (selected) colors.accent else colors.muted,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (selected) colors.accent.copy(alpha = 0.6f) else colors.lineStrong),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = starholdType.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (count != null) {
                Spacer(Modifier.width(6.dp))
                Text(count.toString(), style = starholdType.caption, color = colors.faint)
            }
        }
    }
}

@Composable
fun LoadingBlock(label: String = "Loading") {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spinner(size = 26.dp)
            Spacer(Modifier.height(12.dp))
            Text(label, style = starholdType.small, color = starholdColors.muted)
        }
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(starholdColors.line))
}

@Composable
fun Badge(text: String, color: Color = starholdColors.surface2, contentColor: Color = starholdColors.muted) {
    Surface(shape = RoundedCornerShape(4.dp), color = color, contentColor = contentColor) {
        Text(
            text,
            style = starholdType.caption,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun MetaLine(parts: List<String?>, modifier: Modifier = Modifier, color: Color = starholdColors.muted) {
    val text = parts.filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
    if (text.isNotBlank()) {
        Text(text, style = starholdType.small, color = color, modifier = modifier)
    }
}

@Composable
fun HSpacer(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun VSpacer(height: Dp) = Spacer(Modifier.height(height))
