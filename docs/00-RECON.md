# 00 — Recon: what Starhold is and what we are reimplementing

Captured 2026-10-29 from `/home/mithral/wyrmhoard` (monorepo, read-only for this
work). This document is the shared context for the native repo.

## 1. Product

Starhold is a lightweight self-hosted media server: movies, TV, music, books,
comics, audiobooks, podcasts and Live TV. One Go binary serves:

| Surface | Base | Consumers |
|---|---|---|
| Native JSON API | `/api/v1` | bundled Svelte web SPA, admin, **this native client** |
| Jellyfin facade | `/` | Moonfin/Jellyfin clients; web SPA uses a handful of routes |
| OpenSubsonic facade | `/rest` | music clients |
| OPDS 1.2 | `/opds` | e-readers |

Deployment: Docker on `51.222.41.139`, public beta at
`https://beta.mithraic.cloud`, admin at `https://admin.mithraic.cloud`.
Server port in dev: `18901`; container maps `127.0.0.1:18950 → 8080`.

## 2. Components (and what we deliberately ignore)

| Piece | Path | Stack | Role in this project |
|---|---|---|---|
| Server + bundled SPA | `wyrmhoard-server/` | Go, SQLite, chi | **API provider only** — untouched |
| Web SPA | `wyrmhoard-server/web/` | Svelte 5 + Vite | **UI/behaviour reference** — untouched |
| Admin | `wyrmhoard-admin/` | Svelte 5 | out of scope (server setup stays in admin) |
| Flutter app | `wyrmhoard-app/` | Moonfin-Core fork, GPL-2.0 | reference only; **not** linked or copied |
| Moonfin upstream | `reference/moonfin/` | Flutter | not used |

**Hard constraint:** no edits to `wyrmhoard-server` (including `web/`) or
`wyrmhoard-admin`. The new repo is separate and consumes HTTP only.

## 3. Web SPA inventory (the thing being ported)

`wyrmhoard-server/web/src` — Svelte 5 runes, ~6.9k lines across 32 files.

Screens / routes (`lib/router.ts`):

| Route | Component | Notes |
|---|---|---|
| `/` | `Home.svelte` | hero rotation, resume/latest/library rails |
| `/library/:id` | `Browse.svelte` | grid/sort/A–Z, music tabs, books authors, comics franchise→series |
| `/item/:id` | `Detail.svelte` | per-type bodies (series/season/album/artist/audiobook/podcast/book/comic) |
| `/watch/:id` | `Player.svelte` | video + audio-only stage; capability decision; companion fallbacks |
| `/read/:id` | `Reader.svelte` | EPUB foliate iframe; CBZ/CBR/PDF page images |
| `/search?q=` | `Search.svelte` | result grid |
| `/genres` | `Genres.svelte` | chips + filtered grid |
| `/history` | `History.svelte` | recents with progress |
| `/livetv` | `LiveTV.svelte` | channels, guide, player |
| `/settings` | `Settings.svelte` | appearance, MFA, app passwords, companion, reporting, sessions |
| `/login`, `/signup`, `/` (signed out) | Login/Signup/Landing | invitation flow |
| `/plugin/:id` | `PluginView.svelte` | server plugin iframe (not portable; see parity matrix) |

Shell (`Shell.svelte`): fixed nav that is transparent over the hero and solid on
scroll; centered glass pill of destinations (Home, Live TV, one per library with
type icon); expanding search overlay (`/` shortcut); avatar menu (History,
Settings, Sign out); global route-enter fade; optional bottom mini-player for
audio that persists across routes.

State/UX details worth preserving (all found in source):

- Home hero: candidate pool = items with `plot && has_image && type in
  {movie,series}` from latest + rail items, de-duplicated, shuffled; first 12;
  8 s rotation; wrap re-shuffles; pause control; progress dot; next backdrop
  preloaded at `maxWidth=1280`.
- Rails: only items with artwork (`has_image`), episodes excluded from resume
  rails (series page owns them); 60-item pages; horizontal scroll with edge
  arrows; card progress bar from `position_seconds`.
- Cards: 2:3 posters, 8 px radius, corner favourite/watched badges, caption
  rules per type (episode S#E#, album artist·year, circle for artists).
- Browse: sort name/year/added; infinite scroll with 600 px root margin;
  A–Z rail sticky under nav, scroll-to-letter with retry loop; music Home tab
  (quick-pick grid, recently played, artists, albums); songs table with inline
  play; books author collage → series shelves; comics franchise → series →
  issue rack with issue numbers.
- Detail: backdrop 52vh with dual scrim; 156 px poster; logo image over title
  with text fallback; metadata dot row including "Ends at HH:MM" for resume;
  circular favourite/download actions; Watched toggle writes full runtime;
  season tabs, 96 px episode thumbnails; album/track table; artist albums;
  audiobook chapters with resume hints; `PlaybackInfo` stream disclosure.
- Player: capability probe (`canPlayType` + MSE), server decision
  `direct|remux|cpu|unsupported`, audio-track switch restarts remux at position,
  WebVTT fetch and time-shift by `-offset`, subtitle delay ±10 s, cue size/background,
  speed 0.5–2×, volume persisted, 5 s playstate PUT, 95 % ⇒ played, Up Next
  countdown 8 s for next episode, keyboard shortcuts.
- Audio mini-player: queue, shuffle, repeat off/all/one, speed cycle, ±15 s for
  talk content, progress save every 8 s.
- Settings: nav position top/bottom/left/right, density, accent hex, backdrops
  on/off, watched badges on/off (server-synced `ui-preferences`); TOTP
  begin/confirm/disable with recovery codes; revoke other sessions; app
  passwords create/revoke; companion pair/status/revoke; error reporting toggle.

## 4. Server facts that shape the client

- Sessions: opaque bearer token, 24 h expiry, `Authorization: Bearer`.
  `POST /api/v1/auth/login` → `{access_token, user:{username, role, can_download}}`.
  MFA returns HTTP 4xx with `code: mfa_required` then needs `mfa_code`.
- Media GETs authenticate by header (native) — the web used a `wh_media` cookie
  for `<img>`/`<video>`; native requests can simply send the bearer header.
- IDs are 32-char lowercase hex. Jellyfin responses use Ticks (100 ns).
- Playback decision is server-side: `GET /api/v1/items/{id}/playback` with
  reported `containers`, `video`, `audio` codec capabilities returns
  `mode`, `reason`, `direct_url`, `remux_url`, audio tracks and subtitle URLs.
- No transcoding by default; `remux` is container-only stream copy; CPU
  transcode exists but is explicit opt-in.
- Server may have no images (`has_image=false`); clients must fall back.
- Some Jellyfin endpoints are intentionally implemented; many are stubs. The web
  SPA uses a specific subset (documented in `02-API-CONTRACT.md`).
- Features that are server-limited: filters/genres for some types, casting,
  trailers, theme media, session control, QuickConnect, image resize (depends on
  version), plugin pages.

## 5. Constraints and decisions

1. **No server/web changes.** If an API is missing, the native client degrades
   visibly rather than patching the server.
2. **API parity first.** Same endpoints and payload semantics as the web SPA, so
   server upgrades keep both clients aligned.
3. **No GPL code reuse.** The Flutter Moonfin fork is reference only. This repo
   is written from the web SPA's behaviour and the HTTP contract.
4. **Native, not a webview.** Compose Multiplatform rendering + platform media
   engines; no embedded browser shell.
5. **TV is a first-class target.** Focus handling and overscan are designed in,
   not retrofitted.

## 6. Where to look next

- `01-UI-SPEC.md` — exact visual system and screen anatomy.
- `02-API-CONTRACT.md` — endpoints, shapes, headers, error codes.
- `03-ARCHITECTURE.md` — how the native code is organised.
- `04-PLATFORMS.md` — per-OS build/package/player details.
