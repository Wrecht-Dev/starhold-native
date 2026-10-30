# 05 — Building and releasing

## 1. Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **21** (Temurin recommended) | AGP 8.13 + Kotlin 2.2; JDK 25 is not supported by AGP |
| Gradle | via wrapper 8.14.3 | do not use a system Gradle |
| Android SDK | platform 36, build-tools 36.x | `ANDROID_HOME` or `local.properties` |
| Xcode | 16+ (macOS only) | for iOS apps |
| mpv | 0.3x+ | desktop playback (runtime dependency) |
| WiX 3.14 | Windows only | for `.msi` |
| `jpackage` | bundled with JDK 21 | for desktop packages |

`local.properties` (not committed):

```properties
sdk.dir=/home/you/Android/Sdk
```

`~/.gradle/gradle.properties` for release signing (Android):

```properties
STARHOLD_STORE_FILE=/secure/starhold-release.jks
STARHOLD_STORE_PASSWORD=…
STARHOLD_KEY_ALIAS=starhold
STARHOLD_KEY_PASSWORD=…
```

## 2. Everyday commands

```bash
export JAVA_HOME=~/.jdks/temurin-21

./gradlew :core:desktopTest                 # domain tests (24)
./gradlew :composeApp:assembleDebug         # Android APK (phone + TV)
./gradlew :composeApp:assembleRelease       # signed APK/AAB (needs keystore)
./gradlew :composeApp:run                   # desktop app (needs a display)
./gradlew :composeApp:packageDeb            # Linux .deb (host needs dpkg)
./gradlew :composeApp:packageRpm            # Linux .rpm (host needs rpmbuild)
./gradlew :composeApp:packageMsi            # Windows .msi (run on Windows)
./gradlew :composeApp:packageDmg            # macOS .dmg (run on macOS)
./gradlew :composeApp:createDistributable   # unpacked app image for the host OS
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # iOS framework (macOS)
```

Output locations:

- APK: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
- AAB: `composeApp/build/outputs/bundle/release/`
- Desktop installers: `composeApp/build/compose/binaries/main/{deb,rpm,msi,dmg}/`
- iOS framework: `composeApp/build/bin/iosSimulatorArm64/debugFramework/StarholdApp.framework`

## 3. iOS build

```bash
# On macOS with Xcode selected:
xcode-select -p
./gradlew :composeApp:linkDebugFrameworkIosArm64
open iosApp/Starhold.xcodeproj
# Set DEVELOPMENT_TEAM, bundle id dev.starhold.app, then Run.
```

The Xcode project is a thin host: it embeds `StarholdApp.framework`, calls
`MainViewController()` from `ComposeApp`, and declares the ATS/background-audio
plist keys documented in `04-PLATFORMS.md`.

## 4. CI outline

```yaml
# .github/workflows/build.yml (illustrative)
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: ./gradlew :core:desktopTest
      - run: ./gradlew :composeApp:assembleDebug
  desktop:
    strategy: { matrix: { os: [ubuntu-latest, windows-latest, macos-latest] } }
    runs-on: ${{ matrix.os }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 21 }
      - run: ./gradlew :composeApp:packageDeb
        if: runner.os == 'Linux'
      - run: ./gradlew :composeApp:packageMsi
        if: runner.os == 'Windows'
      - run: ./gradlew :composeApp:packageDmg
        if: runner.os == 'macOS'
```

## 5. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Unsupported class file major version 69` | Gradle ran on JDK 25. Set `JAVA_HOME` to Temurin 21. |
| `Android Gradle plugin requires Java 17+` | Same; 21 works. |
| `SDK location not found` | `local.properties` missing `sdk.dir`. |
| Compose compiler mismatch | Kotlin and Compose plugin are pinned in `gradle/libs.versions.toml`; keep them in sync. |
| `Illegal version for 'Dmg'` | `packageVersion` must be ≥ 1.0.0; don't use 0.x. |
| Desktop playback prints "Could not start mpv" | Install `mpv` or set `STARHOLD_MPV=/path/to/mpv`. |
| iOS framework won't link | Apple targets need a macOS host; Linux cannot cross-link. |
| APK installs but no TV entry | TV banner/leanback filter requires a fresh install; Android caches launcher entries. |

## 6. Repository layout

```text
starhold-native/
  core/                 KMP domain + network + stores (tests)
  composeApp/           Compose Multiplatform app + platform actuals
    src/commonMain/     shared UI, player contract, themes
    src/androidMain/    Activity, Media3, TV detection
    src/desktopMain/    main(), mpv engine, Preferences store
    src/iosMain/        UIViewController host, AVPlayer
  iosApp/               Xcode host project
  docs/                 recon, UI spec, API contract, architecture, platforms, parity
  tools/                TV banner, icon generation
  gradle/               wrapper + version catalog
```

## 7. Verification evidence

Recorded on Linux, 2026-10-29:

| Command | Result |
|---|---|
| `./gradlew :core:desktopTest` | 24 tests, 0 failures |
| `./gradlew :composeApp:assembleDebug` | APK produced |
| `./gradlew :composeApp:compileKotlinDesktop` | success |
| `./gradlew :composeApp:packageDeb` | unsupported: this Fedora JDK has no dpkg support |
| `./gradlew :composeApp:packageRpm` | success (see dist) — rpmbuild present |
| Xvfb + software Skiko smoke run | window opens, no crash |
| `mpv --version` | mpv 0.41.0 present |

iOS/Windows/macOS builds must be produced on their native hosts.
