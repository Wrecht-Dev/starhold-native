package dev.starhold.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.starhold.app.AppState
import dev.starhold.app.Screen
import dev.starhold.app.design.AppIcon
import dev.starhold.app.design.Tokens
import dev.starhold.app.design.starholdColors
import dev.starhold.app.design.starholdType
import dev.starhold.app.errorText
import dev.starhold.core.network.ApiException
import kotlinx.coroutines.launch

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(starholdColors.bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Wordmark(size = 30.dp)
            Spacer(Modifier.height(20.dp))
            Spinner()
            Spacer(Modifier.height(10.dp))
            Text("Waking Starhold", style = starholdType.caption, color = starholdColors.muted)
        }
    }
}

@Composable
fun ServerSetupScreen(app: AppState) {
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (connecting) return
        error = null
        connecting = true
        scope.launch {
            app.connect(address).fold(
                onSuccess = { connecting = false },
                onFailure = { error = errorText(it); connecting = false },
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Tokens.Bg, Tokens.BgDeep),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Wordmark(size = 34.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                "Connect to your server",
                style = starholdType.h2,
                color = starholdColors.text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Enter the address of your Starhold server. It stays on this device.",
                style = starholdType.small,
                color = starholdColors.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))
            StarholdTextField(
                value = address,
                onValueChange = { address = it; error = null },
                label = "Server address",
                placeholder = "https://media.example.com",
                keyboardType = KeyboardType.Uri,
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, style = starholdType.small, color = starholdColors.danger)
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = if (connecting) "Connecting…" else "Connect",
                onClick = ::submit,
                enabled = !connecting && address.isNotBlank(),
                icon = if (connecting) null else AppIcon.External,
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Self-hosted. No account leaves your network.",
                style = starholdType.caption,
                color = starholdColors.faint,
            )
        }
    }
}

@Composable
fun AuthFlow(app: AppState, onSignedIn: () -> Unit) {
    var screen by remember { mutableStateOf<Screen>(Screen.Landing) }
    when (val current = screen) {
        Screen.Login -> LoginScreen(
            app = app,
            onBack = { screen = Screen.Landing },
            onSignedIn = onSignedIn,
        )
        is Screen.Signup -> SignupScreen(
            app = app,
            token = current.token,
            code = current.code,
            onBack = { screen = Screen.Login },
            onSignedIn = onSignedIn,
        )
        else -> LandingScreen(
            app = app,
            onSignIn = { screen = Screen.Login },
            onSignUp = { screen = Screen.Signup() },
        )
    }
}

@Composable
private fun LandingScreen(app: AppState, onSignIn: () -> Unit, onSignUp: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(starholdColors.accent.copy(alpha = 0.22f), Tokens.Bg, Tokens.BgDeep),
                    radius = 1200f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Wordmark(size = 40.dp)
            Spacer(Modifier.height(26.dp))
            Text(
                "Everything you watch,\nliving at home.",
                style = starholdType.display,
                color = starholdColors.text,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp(),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Movies, TV, music, books and Live TV from your own server — on every screen.",
                style = starholdType.body,
                color = starholdColors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 560.dp),
            )
            Spacer(Modifier.height(30.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("Sign in", onSignIn, large = true, icon = AppIcon.UserCircle)
                SecondaryButton("Create account", onSignUp, icon = AppIcon.Plus)
            }
            Spacer(Modifier.height(40.dp))
            Text(
                app.serverUrl ?: "",
                style = starholdType.caption,
                color = starholdColors.faint,
            )
        }
    }
}

@Composable
private fun LoginScreen(app: AppState, onBack: () -> Unit, onSignedIn: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mfa by remember { mutableStateOf("") }
    var needsMfa by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy) return
        error = null
        busy = true
        scope.launch {
            app.login(username.trim(), password, mfa.ifBlank { null }).fold(
                onSuccess = { busy = false; onSignedIn() },
                onFailure = { e ->
                    busy = false
                    if (e is ApiException && e.code == "mfa_required") {
                        needsMfa = true
                        error = "Enter the code from your authenticator."
                    } else if (e is ApiException && e.code == "mfa_invalid") {
                        needsMfa = true
                        error = "That code was not accepted."
                    } else {
                        error = errorText(e)
                    }
                },
            )
        }
    }

    AuthCard(
        title = "Welcome back",
        subtitle = app.serverUrl ?: "",
        onBack = onBack,
    ) {
        StarholdTextField(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            keyboardType = KeyboardType.Text,
        )
        Spacer(Modifier.height(12.dp))
        StarholdTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true,
            keyboardType = KeyboardType.Password,
        )
        if (needsMfa) {
            Spacer(Modifier.height(12.dp))
            StarholdTextField(
                value = mfa,
                onValueChange = { mfa = it },
                label = "Two-factor code",
                placeholder = "6-digit code",
                keyboardType = KeyboardType.Number,
            )
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error!!, style = starholdType.small, color = starholdColors.danger)
        }
        Spacer(Modifier.height(18.dp))
        PrimaryButton(
            text = if (busy) "Signing in…" else "Sign in",
            onClick = ::submit,
            enabled = !busy && username.isNotBlank() && password.isNotBlank(),
            large = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SignupScreen(
    app: AppState,
    token: String,
    code: String,
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
) {
    var invite by remember { mutableStateOf(token.ifBlank { code }) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (busy) return
        error = null
        busy = true
        scope.launch {
            val client = app.api
            if (client == null) {
                error = "No server configured"
                busy = false
                return@launch
            }
            runCatching {
                client.redeemInvite(
                    token = if (token.isNotBlank()) token else invite.takeIf { it.isNotBlank() },
                    code = if (code.isNotBlank()) code else null,
                    username = username.trim(),
                    password = password,
                )
            }.fold(
                onSuccess = { result ->
                    if (result.pendingApproval || result.accessToken == null) {
                        busy = false
                        pending = true
                    } else {
                        // Redeem returns a token, but signing in through the normal
                        // path keeps session policy (role, download) authoritative.
                        app.login(username.trim(), password).fold(
                            onSuccess = { busy = false; onSignedIn() },
                            onFailure = { error = errorText(it); busy = false },
                        )
                    }
                },
                onFailure = { error = errorText(it); busy = false },
            )
        }
    }

    AuthCard(title = "Join this server", subtitle = "An invite is required", onBack = onBack) {
        if (pending) {
            Text(
                "Your account was created and is waiting for an administrator to approve it.",
                style = starholdType.body,
                color = starholdColors.muted,
            )
            Spacer(Modifier.height(16.dp))
            SecondaryButton("Back to sign in", onBack, modifier = Modifier.fillMaxWidth())
        } else {
            if (token.isBlank() && code.isBlank()) {
                StarholdTextField(
                    value = invite,
                    onValueChange = { invite = it },
                    label = "Invite token or code",
                )
                Spacer(Modifier.height(12.dp))
            }
            StarholdTextField(
                value = username,
                onValueChange = { username = it },
                label = "Username",
            )
            Spacer(Modifier.height(12.dp))
            StarholdTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
            )
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, style = starholdType.small, color = starholdColors.danger)
            }
            Spacer(Modifier.height(18.dp))
            PrimaryButton(
                text = if (busy) "Creating…" else "Create account",
                onClick = ::submit,
                enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AuthCard(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Tokens.Bg, Tokens.BgDeep))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 430.dp)
                .clip(RoundedCornerShape(Tokens.Radius.lg))
                .background(starholdColors.surface)
                .padding(26.dp),
        ) {
            if (onBack != null) {
                IconButton(AppIcon.ArrowLeft, onBack, contentDescription = "Back", tint = starholdColors.muted)
                Spacer(Modifier.height(12.dp))
            }
            Wordmark()
            Spacer(Modifier.height(20.dp))
            Text(title, style = starholdType.h2, color = starholdColors.text)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = starholdType.caption, color = starholdColors.muted)
            Spacer(Modifier.height(22.dp))
            content()
        }
    }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(
    this.toFloat(),
    androidx.compose.ui.unit.TextUnitType.Sp,
)
