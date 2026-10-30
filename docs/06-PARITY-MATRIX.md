# 06 — Parity matrix

Status legend: **Done** = implemented and compiles on the verified targets;
**Partial** = works with known gaps listed; **Planned** = not in this build.

Runtime verification note: this environment has no reachable Starhold account and
no display, so screens were compiled and the desktop app was smoke-launched
headless; end-to-end playback against a live library still needs a real server.

## 1. Foundation

| Capability | Status | Notes |
|---|---|---|
| Server address entry + health probe + normalisation | Done | `http://` assumed when no scheme; trailing slash removed |
| Password login, session restore, 24 h token | Done | token in KeyValueStore; `Authorization: Bearer` |
| TOTP challenge (`mfa_required`, `mfa_invalid`) | Done | code field appears after the first attempt |
| Invite signup with pending-approval state | Done | redeem then sign in |
| 401 → sign out → login | Done | `ApiException` mapping |
| Session expiry, revoke others | Done | Settings |
| UI preferences synced with server | Done | local mirror first, then PUT |
| Volume remembered locally | Done | 0–1 |
| Offline/error copy | Done | no raw exceptions in UI |
| Native API + Jellyfin facade split | Done | same endpoints as the web client |

## 2. Shell and navigation

| Capability | Status | Notes |
|---|---|---|
| Top / bottom / left / right navigation | Done | from `ui-preferences` |
| Responsive compact mode | Done | icons-only rails under 760 dp |
| Back stack, Android back, Esc | Done | `PlatformBackHandler` |
| Global search entry | Done | search screen with debounce |
| Account avatar → settings | Done | |
| Library entries with type icons | Done | movies, TV, music, books, comics, games |
| Live TV nav entry | Done | always present; empty state explains setup |

## 3. Home

| Capability | Status | Notes |
|---|---|---|
| Rotating backdrop hero (≤12, 8 s) | Done | pause/resume + dots, dark dual scrim |
| Hero Play / Resume / Details | Done | series goes to detail, films to player |
| Continue watching rail | Done | server resume list, progress bars |
| Recently added rail | Done | `/Items/Latest` |
| One rail per library | Done | 14 items, sort=added |
| Rail arrows, see-all | Done | LazyRow scroll |
| Poster/album/comic art, watched + favourite badges | Done | authenticated image loader, 128-entry cache |
| Empty/error/loading states | Done | |
| Editorial rows (because you watched, collections) | Planned | server exposes no such surface |

## 4. Browse

| Capability | Status | Notes |
|---|---|---|
| Poster grid, adaptive columns | Done | 142 dp cells |
| Infinite scroll (60/page) | Done | triggers 12 items before the end |
| Sort: name / added / year | Done | |
| Genre chips with counts | Done | up to 40 genres |
| Item count header | Done | `/libraries/{id}/count` |
| A–Z jump rail | Planned | sort=name exists; letter index not built |
| Music tabs (albums/artists/tracks) | Partial | album/artist grids render; no tabs |
| Books author → series grouping | Planned | flat grid |
| Comics franchise → series rack | Planned | flat grid |

## 5. Detail

| Capability | Status | Notes |
|---|---|---|
| Backdrop hero, poster card | Done | wide layout; narrow shows backdrop only |
| Title, tagline, type/year/runtime/rating/SxxExx | Done | |
| Genres badges, plot, credits | Done | |
| Play / Resume with saved position | Done | |
| Favourite toggle | Done | optimistic, server confirms |
| Watched toggle | Done | resets position when unwatching |
| Series seasons + episode rows | Done | season chips, 16:9 thumbs, progress |
| Season episode list | Done | |
| Album track list with numbers/durations | Done | |
| Artist albums | Partial | artist page shows bio only |
| Book/comic detail + Read action | Done | chapter/page counts |
| Similar rail | Done | |
| Download | Planned | needs authenticated save-to-disk/SAT |
| Media-stream disclosure table | Planned | |
| Logo art | Planned | |

## 6. Player

| Capability | Status | Notes |
|---|---|---|
| Server playback decision (direct / remux / unsupported) | Done | capabilities reported per platform |
| Remux start offset + cue shift | Done | cues shifted by `-start` |
| Explicit CPU conversion (when allowed) | Done | confirm button on failure |
| Audio track list + switch | Done | switch re-requests remux at current position |
| Subtitle list + selection + Off | Done | Compose-rendered WebVTT |
| Subtitle size (S/M/L) | Done | |
| Playback speed | Done | engine-dependent |
| Progress sync 5 s / 8 s audio | Done | pause/exit also save |
| 95 % ⇒ played | Done | |
| Up Next countdown (8 s) | Done | next episode from season list |
| Buffering/error overlays | Done | clear reason + retry |
| Subtitle delay | Planned | parser already supports shifting |
| PiP / fullscreen button | Planned | Android/iOS native gestures only |
| Live channel playback | Done | Watch(live=true), LIVE badge, no seek |
| Audio-only stage artwork | Planned | audio plays with poster background |

## 7. Readers

| Capability | Status | Notes |
|---|---|---|
| EPUB chapter fetch + native text rendering | Done | headings/paragraphs/line breaks |
| Chapter list, prev/next, local progress | Done | chapter index remembered per item |
| Reader themes (dark/light/sepia) + text size | Done | |
| Inline chapter images | Planned | parser drops `<img>` |
| Comic/PDF page images + zoom/pan | Done | authenticated bytes, pinch zoom |
| Page slider + tap-zones + local progress | Done | |
| Two-page spread | Planned | |

## 8. Search, Genres, History

| Capability | Status | Notes |
|---|---|---|
| Debounced search (≥2 chars) | Done | grid results |
| Empty/no-results/error states | Done | |
| Genre list with counts → titles | Done | |
| History list with progress/relative time | Done | opens detail |
| Clear history | Planned | server has no delete endpoint |

## 9. Live TV

| Capability | Status | Notes |
|---|---|---|
| Channel list, number, logo, health | Done | |
| Group filter chips | Done | |
| Favourites | Done | |
| Now/next with progress | Done | EPG list |
| Channel playback | Done | |
| Full time-grid EPG | Planned | list style instead of columns × time |
| Channel search box | Planned | server supports `q` |
| Recording/timeshift | Planned | server has no DVR surface |

## 10. Settings

| Capability | Status | Notes |
|---|---|---|
| Account summary, sign out, revoke sessions | Done | |
| Nav position, density | Done | |
| Accent colour presets + custom hex | Done | validated, applied immediately |
| Backdrops / watched-badge toggles | Done | stored server-side |
| Player engine info, volume | Done | |
| TOTP enable (secret/URI, confirm, recovery codes) | Done | one-time code display |
| TOTP disable (password + code) | Done | |
| App passwords create/revoke (one-time reveal) | Done | |
| Companion devices list/revoke | Done | |
| Error-reporting toggle | Done | |
| Pairing-code flow (show/enter) | Planned | server endpoints exist |
| Plugin pages | Planned (web-only) | out of scope |

## 11. Android TV

| Capability | Status | Notes |
|---|---|---|
| Leanback launcher + banner 320×180 | Done | generated banner |
| Same APK phone/TV | Done | |
| TV detection → larger targets, left rail default | Partial | `isTelevision()` wired; default nav still user pref |
| D-pad focus rings on cards/buttons | Partial | cards and buttons focusable; no global focus graph/overscan |
| Media keys | Planned | |

## 12. Platforms

| Item | Status |
|---|---|
| Android APK build | Done (verified) |
| Desktop JVM build | Done (verified) |
| Desktop `.deb` packaging | Verified |
| Desktop smoke launch under Xvfb | Verified (window opens) |
| iOS framework + Xcode project | Scaffolded; needs macOS to link |
| Windows MSI | Configured; needs Windows/WiX |
| macOS DMG | Configured; needs macOS |
| mpv external playback (Linux) | Implemented; needs a real stream to verify |
| Media3 playback | Implemented; needs a device/emulator |
| AVPlayer playback | Implemented; needs a device |
| Signing/notarisation | Documented, not executed |

## 13. Deliberate gaps

1. No server or web code was touched; this repo is standalone.
2. Downloads, EPG grid, A–Z index, music tabs and grouping are the main visible
   differences from the web client.
3. Desktop playback deliberately uses an external mpv window; libmpv/VLC
   embedding is future work (`docs/04-PLATFORMS.md`).
4. No crash reporting SDK, telemetry or analytics.
