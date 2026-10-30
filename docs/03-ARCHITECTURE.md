# 03 — Native client architecture

## 1. Modules

```text
:core                         Kotlin Multiplatform library (no Compose)
  model/                      Item, Library, PlaybackDecision, LiveChannel, …
  network/                    StarholdApi (Ktor), ApiException, JSON helpers
  store/                      KeyValueStore, SessionStore, URL normalisation
  format/                     duration/clock/date helpers (pure, tested)
  playback/                   WebVTT parser and cue timing (pure, tested)

:composeApp                   Compose Multiplatform application
  commonMain/
    design/                   Tokens, Theme (Manrope), AppIcon set
    ui/                       Screens + shared components + RemoteImage
    player/                   PlayerEngine contract, controls, subtitle overlay
    Navigation.kt             Screen sealed interface + Navigator back stack
    AppState.kt               Session, server URL, libraries, preferences
    Shell.kt                  Responsive nav + screen dispatch
    App.kt                    Root: splash → server setup → auth → shell
  androidMain/                MainActivity, Media3 engine, TV detection
  desktopMain/                main(), mpv engine, Preferences store
  iosMain/                    UIViewController factory, AVPlayer engine
```

`core` has JVM (`desktopTest`) tests and no Compose dependency, so domain logic
is fast to test and reusable by future clients (e.g. a CLI or watchOS shell).

## 2. Layers and state

```text
Screen (Composable)
   │  reads state, calls suspend functions in a screen model
   ▼
ScreenModel / load()            (remember + mutableStateOf / LaunchedEffect)
   │
   ├── AppState                 session, server URL, preferences (Compose state)
   └── StarholdApi              typed HTTP, auth header, error mapping
          │
          ▼
        core models (immutable data classes)
```

- No DI framework. `AppState` is created once in `App.kt` and passed down; the
  API client is built from the current server URL and a token provider.
- Screens hold ephemeral state in `remember`; anything that must survive
  navigation is in `AppState` (session, preferences) or refetched.
- Every network call maps failures through `errorText()`; a `401` calls
  `AppState.sessionExpired()` which clears the token and returns to login.
- Loading, error and empty states are explicit in every screen. A screen never
  renders a blank page or an endless spinner.

## 3. Navigation

`Navigator` is a back stack of `Screen` sealed cases:

```text
Home
Library(id)  Detail(id)  Watch(id)  Read(id)
Search(query)  Genres  History  LiveTv  Settings
```

Shell destinations (Home, Live TV, libraries) reset the stack; drill-downs push.
Android hardware back is wired through `PlatformBackHandler`; desktop uses the
on-screen back arrow and Esc; iOS uses the in-app back arrow.

## 4. Playback architecture

The server decides *how* to play (`direct`, `remux`, `unsupported`) from the
capabilities the client reports. The UI never guesses a codec.

```text
PlaybackScreen (common)
  ├─ StarholdApi.playbackDecision(id, caps, audioIndex)
  ├─ decides mode: direct | remux | cpu (explicit user action)
  ├─ PlayerEngine (expect/actual, one per platform)
  └─ SubtitleOverlay (common, parses WebVTT and shifts by start offset)
```

`PlayerEngine` contract (common):

```kotlin
interface PlayerEngine {
    val state: StateFlow<PlaybackState>   // positionMs, durationMs, playing, buffering, error
    fun load(request: MediaRequest, startMs: Long, audioIndex: Int)
    fun play(); fun pause(); fun seekTo(ms: Long)
    fun setVolume(v: Float); fun setRate(r: Float)
    fun selectAudio(index: Int)
    fun release()
}
```

Platform implementations:

| Platform | Engine | Notes |
|---|---|---|
| Android / Android TV | AndroidX Media3 ExoPlayer | HLS + progressive; hardware decode |
| iOS / iPadOS | AVPlayer | HLS + progressive; PiP/fullscreen via AVPlayerViewController |
| macOS / Windows / Linux | mpv (libmpv) | See `04-PLATFORMS.md`; subtitle overlay is drawn by Compose, not mpv |

Subtitles are **always rendered by Compose** from the server's WebVTT, which
keeps cue size/background/delay identical on every platform and avoids
per-engine subtitle quirks. Audio-track selection re-requests a remux at the
current position when the container has to change.

Progress is PUT every 5 s while playing and on pause/exit; at ≥95 % the item is
marked played with position reset. Up Next offers the next episode after an
episode ends (8 s countdown, no autoplay unless the user lets it run).

## 5. Images

`RemoteImage` fetches bytes through `StarholdApi` (so the bearer token is sent),
decodes on `Dispatchers.Default` via a platform `decodeImage`, and stores results
in a 128-entry LRU. Missing art renders a flat surface tile; cards never show a
broken-image glyph. Backdrops request `maxWidth=1280`, posters `maxWidth=240–320`.

## 6. Design system

`design/Tokens.kt` mirrors the server's `design/tokens.json`. `Theme.kt` builds a
Material 3 dark scheme plus `StarholdColors`/`StarholdType`/spacing scale locals.
Accent comes from the account's `ui-preferences` (validated `#rrggbb`). Manrope
400/600/700/800 is bundled as a Compose resource. `AppIcon` is a self-contained
24×24 path set rendered with Canvas, so there is no icon-font or Material Icons
dependency and every glyph matches the web client's stroke language.

## 7. Android TV

- Same APK; `LEANBACK_LAUNCHER` intent filter and `android.software.leanback`
  use-feature.
- `isTelevision()` detects leanback/UI mode; `LocalIsTelevision` drives larger
  focus targets, a left nav rail by default, and 48 dp overscan.
- Every interactive element is focusable. Cards and rows use
  `onFocusChanged`-driven borders; the focused card scrolls into view.
- Player chrome is D-pad first: play/pause, seek, tracks, subs reachable in ≤3
  presses.

## 8. Testing

- `core` unit tests: URL normalisation, session persistence, formatting, WebVTT
  parsing/shifting, Jellyfin→native mapping, playback-mode mapping.
- Ktor `MockEngine` tests for the API client (headers, 401 mapping, query
  building) — no live server required.
- Compose UI tests are Android-instrumentation only; visual parity is checked by
  running the desktop app against a real server.
- Build gates: `:core:desktopTest`, `:composeApp:assembleDebug`,
  `:composeApp:run` (smoke), `ktlint`/`detekt` optional.

## 9. Error and offline behaviour

- Connection failures show "Cannot reach the server…" with retry, never a crash.
- Image and backdrop failures are silent (placeholder tile).
- A 401 anywhere clears the session once and returns to Login.
- Reader/player failures show the server's reason (e.g. unsupported container)
  and offer download / retry / web fallback where meaningful.
