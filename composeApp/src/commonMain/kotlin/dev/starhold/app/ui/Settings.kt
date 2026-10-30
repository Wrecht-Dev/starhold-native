package dev.starhold.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.starhold.app.AppState
import dev.starhold.core.CORE_VERSION
import dev.starhold.app.Navigator
import dev.starhold.app.Screen
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.errorText
import dev.starhold.app.player.playerEngineName
import dev.starhold.core.model.AppPassword
import dev.starhold.core.model.CompanionDevice
import dev.starhold.core.model.MeUser
import dev.starhold.core.model.MfaBegin
import dev.starhold.core.model.UiPreferences
import dev.starhold.core.network.StarholdApi
import kotlinx.coroutines.launch

private val ACCENTS = listOf(
    "#6A6CF0" to "Indigo",
    "#E5484D" to "Red",
    "#30A46C" to "Green",
    "#F5A623" to "Amber",
    "#8E4EC6" to "Violet",
    "#00A8A8" to "Teal",
)

@Composable
fun SettingsScreen(app: AppState, nav: Navigator) {
    val api = app.api ?: return
    val colors = starholdColors
    val scope = rememberCoroutineScope()

    var me by remember { mutableStateOf<MeUser?>(null) }
    var health by remember { mutableStateOf<StarholdApi.ServerHealth?>(app.serverHealth) }
    var mfaEnabled by remember { mutableStateOf(false) }
    var passwords by remember { mutableStateOf<List<AppPassword>>(emptyList()) }
    var devices by remember { mutableStateOf<List<CompanionDevice>>(emptyList()) }
    var errorReporting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    var mfaBegin by remember { mutableStateOf<MfaBegin?>(null) }
    var mfaMode by remember { mutableStateOf("idle") }
    var mfaCode by remember { mutableStateOf("") }
    var mfaPassword by remember { mutableStateOf("") }
    var recoveryCodes by remember { mutableStateOf<List<String>>(emptyList()) }

    var newPasswordName by remember { mutableStateOf("") }
    var createdPassword by remember { mutableStateOf<String?>(null) }

    var accentInput by remember(app.preferences.accentColor) {
        mutableStateOf(app.preferences.accentColor.ifBlank { "#6A6CF0" })
    }

    fun setPrefs(transform: (UiPreferences) -> UiPreferences) {
        scope.launch { app.updatePreferences(transform) }
    }

    suspend fun refresh() {
        runCatching { api.me() }.onSuccess { me = it }
        health = runCatching { api.health() }.getOrNull() ?: health
        mfaEnabled = runCatching { api.mfaStatus().enabled }.getOrDefault(false)
        passwords = runCatching { api.appPasswords() }.getOrDefault(emptyList())
        devices = runCatching { api.companionDevices() }.getOrDefault(emptyList())
        errorReporting = runCatching { api.errorReporting() }.getOrDefault(false)
    }

    LaunchedEffect(api) { refresh() }

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (nav.canGoBack) {
                        IconButton(AppIcon.ArrowLeft, onClick = { nav.pop() }, contentDescription = "Back", tint = colors.muted)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("Settings", style = starholdType.h1, color = colors.text)
                }
                Spacer(Modifier.height(4.dp))
                Text("Everything here is stored on this device and your server.", style = starholdType.small, color = colors.muted)
                Spacer(Modifier.height(20.dp))

                Column(Modifier.widthIn(max = 860.dp)) {
                    if (message != null) {
                        Text(message!!, style = starholdType.small, color = colors.success)
                        Spacer(Modifier.height(10.dp))
                    }
                    if (error != null) {
                        Text(error!!, style = starholdType.small, color = colors.danger)
                        Spacer(Modifier.height(10.dp))
                    }

                    Section("Account") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(46.dp).clip(CircleShape).background(colors.surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    (me?.username ?: app.session?.username ?: "?").take(1).uppercase(),
                                    style = starholdType.h3,
                                    color = colors.text,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    me?.username ?: app.session?.username ?: "",
                                    style = starholdType.body,
                                    color = colors.text,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    listOfNotNull(
                                        me?.role ?: app.session?.role,
                                        app.serverUrl,
                                    ).joinToString(" · "),
                                    style = starholdType.caption,
                                    color = colors.muted,
                                )
                            }
                            SecondaryButton("Sign out", onClick = { confirmSignOut = true }, icon = AppIcon.External)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SecondaryButton("Revoke other sessions", onClick = {
                                scope.launch {
                                    busy = true
                                    runCatching { api.revokeOtherSessions() }
                                        .onSuccess { message = "Signed out $it other session(s)." }
                                        .onFailure { error = errorText(it) }
                                    busy = false
                                }
                            }, icon = AppIcon.Devices)
                            SecondaryButton("Refresh", onClick = {
                                scope.launch {
                                    busy = true
                                    refresh()
                                    busy = false
                                }
                            }, icon = AppIcon.Refresh)
                        }
                    }

                    Section("Appearance") {
                        Text("Navigation", style = starholdType.label, color = colors.muted)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UiPreferences.NAV_POSITIONS.forEach { position ->
                                Chip(
                                    text = position.replaceFirstChar { it.uppercase() },
                                    selected = app.preferences.navPosition == position,
                                    onClick = { setPrefs { it.copy(navPosition = position) } },
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Density", style = starholdType.label, color = colors.muted)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UiPreferences.DENSITIES.forEach { density ->
                                Chip(
                                    text = density.replaceFirstChar { it.uppercase() },
                                    selected = app.preferences.density == density,
                                    onClick = { setPrefs { it.copy(density = density) } },
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Accent colour", style = starholdType.label, color = colors.muted)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            ACCENTS.forEach { (hex, name) ->
                                val selected = app.preferences.accentColor.equals(hex, ignoreCase = true)
                                Box(
                                    Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Tokens.parseHex(hex) ?: colors.accent)
                                        .border(
                                            width = if (selected) 2.dp else 1.dp,
                                            color = if (selected) colors.text else colors.lineStrong,
                                            shape = CircleShape,
                                        )
                                        .clickable {
                                            accentInput = hex
                                            setPrefs { it.copy(accentColor = hex) }
                                        },
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarholdTextField(
                                value = accentInput,
                                onValueChange = { accentInput = it },
                                label = "Custom hex",
                                placeholder = "#RRGGBB",
                                modifier = Modifier.width(220.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            SecondaryButton("Apply", onClick = {
                                val normalised = accentInput.trim().let { if (it.startsWith("#")) it else "#$it" }
                                if (Tokens.parseHex(normalised) != null) {
                                    setPrefs { it.copy(accentColor = normalised.lowercase()) }
                                    message = "Accent updated."
                                } else {
                                    error = "Use a colour like #6A6CF0."
                                }
                            })
                        }
                        Spacer(Modifier.height(18.dp))
                        SettingRow("Show backdrops", "Hero art on Home and detail pages") {
                            Switch(
                                checked = app.preferences.showBackdrops,
                                onCheckedChange = { value -> setPrefs { it.copy(showBackdrops = value) } },
                                colors = switchColors(),
                            )
                        }
                        SettingRow("Show watched badges", "A tick on finished titles") {
                            Switch(
                                checked = app.preferences.showWatchedBadges,
                                onCheckedChange = { value -> setPrefs { it.copy(showWatchedBadges = value) } },
                                colors = switchColors(),
                            )
                        }
                    }

                    Section("Playback") {
                        SettingRow(
                            "Player engine",
                            "How video is decoded on this device",
                        ) { Text(playerEngineName(), style = starholdType.caption, color = colors.muted) }
                        SettingRow("Default volume", "Remembered on this device") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .width(180.dp)
                                        .clickable { app.updateVolume(if (app.volume > 0.5f) 0.2f else 1f) },
                                ) {
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = { app.volume },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = colors.accent,
                                        trackColor = colors.surface2,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("${(app.volume * 100).toInt()}%", style = starholdType.caption, color = colors.muted)
                            }
                        }
                        Text(
                            "Desktop video opens in mpv; Android uses Media3 and iOS uses AVPlayer. " +
                                "Subtitle styling and progress sync behave the same everywhere.",
                            style = starholdType.caption,
                            color = colors.faint,
                        )
                    }

                    Section("Security") {
                        SettingRow(
                            "Two-factor authentication",
                            if (mfaEnabled) "A code is required at sign-in" else "Add an authenticator app",
                        ) {
                            Switch(
                                checked = mfaEnabled,
                                onCheckedChange = { wanted ->
                                    error = null
                                    if (wanted) {
                                        scope.launch {
                                            runCatching { api.mfaBegin() }
                                                .onSuccess { mfaBegin = it; mfaMode = "setup"; mfaCode = "" }
                                                .onFailure { error = errorText(it) }
                                        }
                                    } else {
                                        mfaMode = "disable"
                                        mfaCode = ""
                                        mfaPassword = ""
                                    }
                                },
                                colors = switchColors(),
                            )
                        }
                        if (mfaMode == "setup" && mfaBegin != null) {
                            Spacer(Modifier.height(12.dp))
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Tokens.Radius.sm))
                                    .background(colors.surface2)
                                    .padding(14.dp),
                            ) {
                                Text("Add this key to your authenticator", style = starholdType.label, color = colors.text)
                                Spacer(Modifier.height(8.dp))
                                SelectionContainer {
                                    Text(mfaBegin!!.secret, style = starholdType.mono, color = colors.accent)
                                }
                                Spacer(Modifier.height(6.dp))
                                SelectionContainer {
                                    Text(mfaBegin!!.uri, style = starholdType.caption, color = colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.height(10.dp))
                                StarholdTextField(mfaCode, { mfaCode = it }, "6-digit code", modifier = Modifier.width(200.dp))
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PrimaryButton("Enable", onClick = {
                                        scope.launch {
                                            runCatching { api.mfaConfirm(mfaCode.trim()) }
                                                .onSuccess { result ->
                                                    recoveryCodes = result.recoveryCodes
                                                    mfaEnabled = result.enabled
                                                    mfaMode = if (result.recoveryCodes.isEmpty()) "idle" else "recovery"
                                                    mfaBegin = null
                                                }
                                                .onFailure { error = errorText(it) }
                                        }
                                    })
                                    SecondaryButton("Cancel", onClick = { mfaMode = "idle"; mfaBegin = null })
                                }
                            }
                        }
                        if (mfaMode == "recovery") {
                            Spacer(Modifier.height(12.dp))
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Tokens.Radius.sm))
                                    .background(colors.surface2)
                                    .padding(14.dp),
                            ) {
                                Text("Save these recovery codes", style = starholdType.label, color = colors.text)
                                Spacer(Modifier.height(6.dp))
                                Text("Each code works once if you lose your authenticator.", style = starholdType.caption, color = colors.muted)
                                Spacer(Modifier.height(8.dp))
                                SelectionContainer {
                                    Text(recoveryCodes.joinToString("   "), style = starholdType.mono, color = colors.text)
                                }
                                Spacer(Modifier.height(10.dp))
                                PrimaryButton("Done", onClick = { mfaMode = "idle" })
                            }
                        }
                        if (mfaMode == "disable") {
                            Spacer(Modifier.height(12.dp))
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Tokens.Radius.sm))
                                    .background(colors.surface2)
                                    .padding(14.dp),
                            ) {
                                Text("Confirm your password and current code", style = starholdType.label, color = colors.text)
                                Spacer(Modifier.height(8.dp))
                                StarholdTextField(mfaPassword, { mfaPassword = it }, "Password", isPassword = true, modifier = Modifier.width(260.dp))
                                Spacer(Modifier.height(8.dp))
                                StarholdTextField(mfaCode, { mfaCode = it }, "6-digit code", modifier = Modifier.width(200.dp))
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PrimaryButton("Disable", onClick = {
                                        scope.launch {
                                            runCatching { api.mfaDisable(mfaPassword, mfaCode.trim()) }
                                                .onSuccess { mfaEnabled = it.enabled; mfaMode = "idle"; message = "Two-factor authentication disabled." }
                                                .onFailure { error = errorText(it) }
                                        }
                                    })
                                    SecondaryButton("Cancel", onClick = { mfaMode = "idle" })
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("App passwords", style = starholdType.label, color = colors.muted)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "For third-party players that cannot use two-factor sign-in.",
                            style = starholdType.caption,
                            color = colors.faint,
                        )
                        Spacer(Modifier.height(8.dp))
                        passwords.forEach { password ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(password.name, style = starholdType.small, color = colors.text)
                                    Text(
                                        "Created ${password.createdAt}" + (password.lastUsedAt?.let { " · used $it" } ?: ""),
                                        style = starholdType.caption,
                                        color = colors.faint,
                                    )
                                }
                                IconButton(AppIcon.Trash, onClick = {
                                    scope.launch {
                                        runCatching { api.revokeAppPassword(password.id) }
                                            .onSuccess { passwords = passwords.filter { it.id != password.id } }
                                            .onFailure { error = errorText(it) }
                                    }
                                }, contentDescription = "Revoke ${password.name}", tint = colors.danger, size = 32.dp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarholdTextField(newPasswordName, { newPasswordName = it }, "New app password name", modifier = Modifier.width(260.dp))
                            Spacer(Modifier.width(8.dp))
                            SecondaryButton("Create", onClick = {
                                if (newPasswordName.isBlank()) {
                                    error = "Give the password a name."
                                } else {
                                    scope.launch {
                                        error = null
                                        runCatching { api.createAppPassword(newPasswordName.trim()) }
                                            .onSuccess { created ->
                                                createdPassword = created.password
                                                passwords = passwords + created.appPassword
                                                newPasswordName = ""
                                            }
                                            .onFailure { error = errorText(it) }
                                    }
                                }
                            }, icon = AppIcon.Plus)
                        }
                        if (createdPassword != null) {
                            Spacer(Modifier.height(10.dp))
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Tokens.Radius.sm))
                                    .background(colors.surface2)
                                    .padding(12.dp),
                            ) {
                                Text("Copy this password now — it is shown once.", style = starholdType.caption, color = colors.accent)
                                Spacer(Modifier.height(6.dp))
                                SelectionContainer {
                                    Text(createdPassword!!, style = starholdType.mono, color = colors.text)
                                }
                                Spacer(Modifier.height(8.dp))
                                SecondaryButton("Done", onClick = { createdPassword = null })
                            }
                        }
                    }

                    Section("Devices") {
                        if (devices.isEmpty()) {
                            Text("No companion devices have been paired.", style = starholdType.small, color = colors.muted)
                        }
                        devices.forEach { device ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        device.name.ifBlank { "Unnamed device" },
                                        style = starholdType.small,
                                        color = colors.text,
                                    )
                                    Text(
                                        listOfNotNull(device.kind, device.lastSeen?.let { "last seen $it" }).joinToString(" · "),
                                        style = starholdType.caption,
                                        color = colors.faint,
                                    )
                                }
                                SecondaryButton("Revoke", onClick = {
                                    scope.launch {
                                        runCatching { api.revokeDevice(device.id) }
                                            .onSuccess { devices = devices.filter { it.id != device.id } }
                                            .onFailure { error = errorText(it) }
                                    }
                                })
                            }
                        }
                    }

                    Section("Privacy") {
                        SettingRow(
                            "Server error reporting",
                            "Send app errors to this server's log",
                        ) {
                            Switch(
                                checked = errorReporting,
                                onCheckedChange = { value ->
                                    scope.launch {
                                        runCatching { api.setErrorReporting(value) }
                                            .onSuccess { errorReporting = value }
                                            .onFailure { error = errorText(it) }
                                    }
                                },
                                colors = switchColors(),
                            )
                        }
                    }

                    Section("About") {
                        SettingRow("Native client", "Version $CORE_VERSION") {
                            Text("Compose Multiplatform", style = starholdType.caption, color = colors.faint)
                        }
                        SettingRow("Server", app.serverUrl ?: "") {
                            Text(
                                health?.let { "${it.name.ifBlank { "Starhold" }} · ${it.version}" } ?: "Unknown",
                                style = starholdType.caption,
                                color = colors.muted,
                            )
                        }
                        Text(
                            "Starhold is self-hosted software. Your library, account and playback history stay on your server.",
                            style = starholdType.caption,
                            color = colors.faint,
                        )
                    }
                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You will need your password (and two-factor code, if enabled) to sign in again.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    app.signOut()
                    nav.reset(Screen.Home)
                }) { Text("Sign out", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            },
            containerColor = colors.surface,
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = starholdColors
    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        SectionTitle(title)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Tokens.Radius.md))
                .background(colors.surface)
                .border(1.dp, colors.line, RoundedCornerShape(Tokens.Radius.md))
                .padding(18.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    val colors = starholdColors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = starholdType.small, color = colors.text, fontWeight = FontWeight.Medium)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = starholdType.caption, color = colors.faint)
            }
        }
        Spacer(Modifier.width(16.dp))
        control()
    }
}

@Composable
private fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = starholdColors.accent,
    uncheckedThumbColor = starholdColors.muted,
    uncheckedTrackColor = starholdColors.surface2,
)
