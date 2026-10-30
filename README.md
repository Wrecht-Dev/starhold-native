# Starhold Native

[![CI](https://github.com/Wrecht-Dev/starhold-native/actions/workflows/ci.yml/badge.svg)](https://github.com/Wrecht-Dev/starhold-native/actions/workflows/ci.yml)
[![Release](https://github.com/Wrecht-Dev/starhold-native/actions/workflows/release.yml/badge.svg)](https://github.com/Wrecht-Dev/starhold-native/actions/workflows/release.yml)

Native client applications for **Starhold**, the self-hosted media server in
`Wrecht-Dev/starhold` (locally at `/home/mithral/wyrmhoard`). Built with
**Kotlin Multiplatform + Compose Multiplatform**: one shared UI and one shared
API client, thin platform entry points and player engines.

| Target | Artifact | Player engine |
|---|---|---|
| Android phones/tablets | APK / AAB | AndroidX Media3 (ExoPlayer) |
| Android TV | same APK, leanback launcher | AndroidX Media3 (ExoPlayer) |
| iOS / iPadOS | Xcode app embedding `StarholdApp.framework` | AVPlayer |
| macOS | `.dmg` (JVM desktop) | external mpv, JSON IPC |
| Windows | `.msi` (JVM desktop) | external mpv (no IPC on Windows) |
| Linux | `.deb` (JVM desktop) | external mpv, JSON IPC |

> This repository is **independent**. It does not modify, vendor, or link the
> existing Go server, the Svelte web SPA, the Svelte admin, or the Flutter
> Moonfin fork. It is a new client that speaks the same HTTP APIs.

## Download

Prebuilt binaries are attached to [GitHub Releases](https://github.com/Wrecht-Dev/starhold-native/releases):

| Platform | Asset |
|---|---|
| Android phone/tablet + Android TV | `Starhold-<version>.apk` |
| Google Play upload | `Starhold-<version>.aab` |
| Debian/Ubuntu | `starhold_<version>_amd64.deb` |
| Fedora/RHEL | `starhold-<version>-1.x86_64.rpm` |
| macOS | `Starhold-<version>.dmg` |
| Windows | `Starhold-<version>.msi` |
| iOS Simulator (unsigned) | `Starhold-<version>-ios-simulator.zip` |

The APK is universal (one file for phones, tablets and TV) and unsigned unless
Android signing secrets are configured. iOS device/IPA builds require your own
Apple team; see [`docs/GITHUB-RELEASES.md`](docs/GITHUB-RELEASES.md).

## What works

A native reimplementation of the web client's flow, against the same endpoints:

- **Auth** — server discovery/health check, password login, TOTP challenge,
  invite signup, session restore, 401 handling.
- **Shell** — top/bottom/left/right navigation from server-synced preferences,
  compact density, back stack, global search.
- **Home** — rotating backdrop hero (≤12 titles, 8 s, pause + dots), Continue
  watching, Recently added, a rail per library.
- **Browse** — adaptive poster grid, sort by name/added/year, genre chips,
  infinite scroll (60 per page).
- **Detail** — backdrop hero, poster, metadata, genres, plot, resume-aware Play,
  favourite, watched, series seasons/episodes, album tracks, similar rail,
  Read entry for books/comics.
- **Player** — server-side direct/remux decision with capability negotiation,
  remux start offset, explicit CPU conversion fallback, audio/subtitle track
  menus, Compose-rendered WebVTT subtitles with size options, speed, 5 s
  progress sync, Up Next countdown, live channel mode.
- **Readers** — EPUB chapters parsed to native text blocks (themes, font sizes,
  chapter navigation, saved position); comic/PDF page viewer with zoom, pan,
  tap-zones, page slider.
- **Search / Genres / History** — debounced search, genre counts to titles,
  history rows with progress.
- **Live TV** — channels, groups, favourites, now/next EPG, playback.
- **Settings** — account, appearance (nav/density/accent/backdrops/badges),
  playback, TOTP MFA, recovery codes, app passwords, companion devices,
  error-reporting toggle, sign-out/revoke.
- **Android TV** — leanback launcher, generated 320×180 banner, larger targets.

Released builds are produced by GitHub Actions on `v*` tags — see
[`docs/GITHUB-RELEASES.md`](docs/GITHUB-RELEASES.md).

The honest gap list lives in [`docs/06-PARITY-MATRIX.md`](docs/06-PARITY-MATRIX.md).
Main known omissions: downloads, full EPG time grid, A–Z index, music tabs and
book/comic grouping, inline EPUB images, PiP button, pairing-code UI.

## Repository layout

```text
starhold-native/
  docs/                    Recon, UI spec, API contract, architecture, platforms, build, parity
  core/                    KMP library: models, HTTP client, session/preferences, formatting,
                           WebVTT parser, HTML→text; unit tests; no Compose
  composeApp/              Compose Multiplatform UI + platform entry points
    src/commonMain/        theme, icons, navigation, all screens, player abstraction
    src/androidMain/       MainActivity, Media3 engine, TV bits, manifest/icons/banner
    src/desktopMain/       main(), mpv IPC engine, Preferences store, packaging icon
    src/iosMain/           MainViewController, AVPlayer engine, NSUserDefaults store
  iosApp/                  Xcode project embedding the StarholdApp framework
  tools/                   Icon/banner source SVG and generated masters
```

## Build

Requirements: JDK 21 (this machine: `~/.jdks/temurin-21`), Android SDK with
platform 36 for Android builds; macOS + Xcode 16 for iOS; Windows + WiX for MSI.

```bash
export JAVA_HOME=$HOME/.jdks/temurin-21

./gradlew :core:desktopTest                 # 24 unit tests
./gradlew :composeApp:assembleDebug         # APK
./gradlew :composeApp:run                   # desktop (needs a display)
./gradlew :composeApp:packageDeb            # Linux .deb (needs dpkg)
./gradlew :composeApp:packageRpm            # Linux .rpm (needs rpmbuild)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # iOS framework (macOS)
```

APK output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

For iOS, open `iosApp/Starhold.xcodeproj`; its build phase runs
`:composeApp:embedAndSignAppleFrameworkForXcode`. `FRAMEWORK_SEARCH_PATHS`
points at `composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.

The first screen asks for the server URL (e.g. `https://media.example.com` or
`http://192.168.1.10:18901`), then username/password, then TOTP if enabled.

## Server compatibility

Uses the native `/api/v1` surface like the web SPA, plus the Jellyfin endpoints
the web client already relies on (resume, latest, seasons/episodes, image URLs,
playback info). Requests authenticate with `Authorization: Bearer <token>`;
media URLs carry the same header rather than a token query parameter. See
[`docs/02-API-CONTRACT.md`](docs/02-API-CONTRACT.md).

## Verified here

- `:core:desktopTest` — 24 tests green.
- `:composeApp:assembleDebug` — APK built.
- `:composeApp:compileKotlinDesktop`, `packageRpm`, headless smoke launch under
  Xvfb (`.deb` needs dpkg, absent on this Fedora host).
- GitHub Actions release run: Android APK/AAB, deb, rpm, dmg, msi and the
  unsigned iOS simulator app all build on hosted runners.
- Not executed: iOS link/run (no macOS), MSI (no Windows), real playback against
  a live server (no credentials), physical TV/phone device testing.

## Licence

No licence has been chosen for this repository yet. The Starhold server and web
client remain separate works under their own terms; nothing from the GPL-2.0
Flutter fork is copied into this codebase.
