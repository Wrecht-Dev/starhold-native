# 04 — Platform matrix, packaging and media engines

## 1. Targets

| Target | Kotlin target | UI | Entry point | Media engine | Package |
|---|---|---|---|---|---|
| Android phone/tablet | `androidTarget()` | Compose | `MainActivity` | Media3 ExoPlayer | APK / AAB |
| Android TV | same APK | Compose | same + leanback filter | Media3 ExoPlayer | APK / AAB |
| iOS / iPadOS | `iosArm64`, `iosSimulatorArm64`, `iosX64` | Compose | `MainViewController()` | AVPlayer | `.ipa` (Xcode) |
| macOS | `jvm("desktop")` | Compose Desktop | `dev.starhold.app.MainKt` | mpv (external, IPC) | `.dmg` |
| Windows | `jvm("desktop")` | Compose Desktop | same | mpv (external, IPC) | `.msi` |
| Linux | `jvm("desktop")` | Compose Desktop | same | mpv (external, IPC) | `.deb` / AppImage |

Compose Desktop is the macOS/Windows/Linux target: a real native desktop
application (JVM + Skiko), not a webview.

## 2. Android

- `minSdk 26`, `targetSdk 36`, `compileSdk 36`, AGP 8.13, JDK 21.
- Cleartext HTTP is enabled in the manifest because self-hosted servers commonly
  run on a LAN with plain HTTP. HTTPS is recommended and used automatically when
  the address starts with `https://`.
- The same APK is the TV app: `LEANBACK_LAUNCHER`, `android.software.leanback`
  (not required), touchscreen not required. TV banner is generated from the
  wordmark (`tools/make-tv-banner.sh`).
- Media3: `media3-exoplayer`, `media3-exoplayer-hls`, `media3-ui`. Auth headers
  are applied through a `DefaultHttpDataSource.Factory`.
- Build: `./gradlew :composeApp:assembleDebug`; release needs a keystore
  configured in `~/.gradle/gradle.properties` (see `docs/05-BUILD.md`).

## 3. iOS / iPadOS

- Kotlin/Native framework `StarholdApp.framework` (static), embedded by the
  `iosApp/` Xcode project.
- `MainViewController()` returns a `ComposeUIViewController`; the SwiftUI host
  wraps it in a `UIViewControllerRepresentable`.
- AVPlayer is the engine. HLS and progressive MP4/MOV play natively. Matroska /
  HEVC-in-MKV depends on the container and codec support of the OS; when the
  server says "unsupported", show the same honest error as the web player and
  offer download. (`remux` mode produces MP4/TS-compatible output, which iOS can
  play.)
- Background audio: `UIBackgroundModes` audio (add to `Info.plist`), Now Playing
  via `MPNowPlayingInfoCenter` (roadmap).
- ATS: `NSAllowsArbitraryLoads` is enabled for self-hosted HTTP servers; the
  App Store review justification is "user-supplied media server address".
- Build requires macOS with Xcode 16+ and Java 21:
  `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`, then open
  `iosApp/Starhold.xcodeproj`.

## 4. Desktop (Windows, macOS, Linux)

### 4.1 Playback

Because Compose Desktop runs on the JVM, AVPlayer/Media Foundation are not
directly reachable. The desktop engine launches **mpv** (a mature native player
available on all three OSes) and controls it:

- URL and headers: `--http-header-fields=Authorization: Bearer …`
- Start position: `--start=<seconds>`; audio track: `--aid=<n>`
- Subtitles: the Compose overlay renders the server's WebVTT, so mpv subtitles
  stay off (`--sid=no`).
- Control channel: JSON IPC via `--input-ipc-server=<unix socket>` on Linux and
  macOS. Windows uses a named pipe; the IPC implementation there is
  best-effort/untested in this environment, and the app falls back to launch-only
  control (the mpv window keeps its own keyboard controls).
- If mpv is missing, the player screen explains how to install it and offers
  "Open stream in browser" (the stream URL requires the bearer header, so this
  is a last resort and is labelled as such).

Why not embed? Software-rendering libmpv into Compose costs a full RGBA copy per
frame and is not viable at 1080p; GPU interop is platform-specific. mpv's own
window keeps hardware decoding and HDR intact. A VLCJ-embedded engine is a
documented future option (requires libvlc).

### 4.2 Packaging

```bash
./gradlew :composeApp:packageDmg      # macOS (notarisation is a separate step)
./gradlew :composeApp:packageMsi      # Windows (WiX toolset on PATH)
./gradlew :composeApp:packageDeb      # Linux
./gradlew :composeApp:createDistributable
```

The JVM runtime is bundled by Compose Desktop (`jpackage`), so end users do not
install Java. Set `nativeDistributions.packageVersion` (currently 1.0.0; DMG
requires a major version ≥ 1) and icons under
`composeApp/src/desktopMain/resources/icons/`.

## 5. Android TV specifics

- 10-foot UI: 960 dp content width, 48 dp overscan, focus ring 2 dp accent.
- Default nav is the left rail on TV (derived from `isTelevision()`; the user can
  change it in Settings).
- Focus order: nav → hero actions → first rail → next rail; `Back` pops the
  stack; `Play/Pause` media keys map to the player.
- The leanback launcher needs a 320×180 banner: generate from the wordmark with
  `tools/make-tv-banner.sh` (ffmpeg/ImageMagick), referenced by `android:banner`.

## 6. Signing and release notes

| Platform | Requirement |
|---|---|
| Android | upload keystore, `signingConfigs.release` from env vars |
| iOS | Apple Developer team, provisioning profile, `DEVELOPMENT_TEAM` in Xcode |
| macOS | Developer ID certificate + notarisation (`notarytool`) for distribution |
| Windows | code-signing certificate for SmartScreen reputation |
| Linux | none; publish `.deb` + tarball, optional AppImage |

Secrets never live in the repo. Build hosts inject them via environment.

## 7. Verification in this environment (Linux)

| Check | Command | Status |
|---|---|---|
| Core JVM tests | `./gradlew :core:desktopTest` | passing |
| Android APK | `./gradlew :composeApp:assembleDebug` | passing |
| Desktop compile/run | `./gradlew :composeApp:run` | build verified; audio/video needs a display |
| iOS link | needs macOS | not runnable here |
| Windows MSI | needs Windows | not runnable here |

## 8. Known platform constraints

1. No casting/session control (server has no reliable session API for this).
2. iOS cannot play raw MPEG-TS Live TV streams; the UI reports it.
3. macOS/Windows/Linux need `mpv` for in-app playback (documented dependency).
4. `libmpv` GPU embedding and VLCJ embedding are future work.
5. Plugin pages are web-only by design.
