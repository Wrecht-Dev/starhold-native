# 01 — UI specification (ported from the Starhold web SPA)

The web app is the design source. This spec is what the native UI executes; the
canonical numeric tokens live in `wyrmhoard-server/design/tokens.json` and are
mirrored in `core/.../design/Tokens.kt`.

## 1. Design tokens

| Token | Value |
|---|---|
| background | `#101010` |
| background deep | `#080808` |
| surface | `#1A1A1A` |
| surface-2 | `#252525` |
| card | `#202020` |
| line | `rgba(255,255,255,0.10)` |
| line strong | `rgba(255,255,255,0.18)` |
| text | `#F5F5F5` |
| muted | `#B3B3B3` |
| faint | `#999999` |
| accent | `#6A6CF0` (user-overridable per account) |
| danger | `#CF6679` |
| success | `#4CAF50` |
| warning | `#FFC107` |
| scrim | `rgba(0,0,0,0.80)` |
| glass | `rgba(26,26,26,0.82)`, border `rgba(255,255,255,0.12)` |

Radii: 4 / 8 / 12 / 16 / 28 / pill. Space scale: 2 / 4 / 8 / 12 / 16 / 24 / 32 /
48. Type: Manrope (bundled) — display 32/700/-0.5, h1 28/700/-0.3, h2 24/600,
h3 20/600, body 16/400/1.5, small 14/400, label 12/500/0.5, caption 12/400.
Controls: small 36, large 44. Motion 140–180 ms ease-out; honour reduced motion.

## 2. Layout

- Page max width 1600; side padding 16→40 (3vw); page bottom padding 80.
- Card width 150 in rails, 160 target; 2:3 poster; gap 8 in rails, 16 in grids.
- Grid: `auto-fill minmax(142, 1fr)` (mobile 108); rails scroll horizontally with
  42 px edge arrows that appear when scrollable.
- Hero: 62–66vh, minimum 440 (mobile 480); backdrop `object-position: center 28%`
  (mobile 68% + scale 1.35); dual scrim: horizontal (97 % → 5 % bg) and vertical
  (bg at bottom, 72 % at 18 %, transparent at 52 %).
- Detail hero: 52vh, backdrop center 30 %; poster 156 (mobile 108); copy column.
- Nav: 64 px; brand left; centered glass pill (surface-2 active, accent icon);
  actions right (search, 40 px avatar). Overlay mode on Home/Detail (transparent
  until 8 px scroll), sticky elsewhere, static on player/reader.
- Mobile: nav wraps to two rows, pill scrolls horizontally; grids 2-col.
- TV: 960 dp safe area, 48 dp overscan margin, focus ring = 2 dp accent with 4 dp
  offset; every interactive element focusable and reachable by D-pad.

## 3. Components

**Card** — poster, radius 8, 1 px line border; hover/focus border accent 55 %,
image brightness 1.08. Fallback tile (letter, surface-2, 26/32 icon for
circle/comic). Progress bar 6 px on poster bottom (accent). Corner badges 26 px
circle (star accent, check). Caption: title strong 14/600 ellipsis; sub line
12 muted per type; artists use circle variant (square art, round crop); music
uses 1:1 art.

**Buttons** — primary accent fill, 40 px, radius 8, 14/700; secondary 6 % white
fill + strong border; quiet text-only muted; icon buttons 36 px square; action
circle 40 px for favourite/download.

**Chips/tabs** — text tabs with 2 px accent underline, not pills (genre chips are
outlined pills with counts).

**Rows** — section title 20/700, arrows, 60-item pages, horizontal snap.

**Empty/error states** — centered notice with title + short explanation + retry;
never a blank screen. Skeletons for detail loading; spinner elsewhere.

## 4. Screen anatomy

### Home
Hero (backdrop, meta line `year · duration · genres[0..3]`, title clamp 2 lines,
plot clamp 3, Play + Details, Pause/Resume, dots with 8 s fill animation) over
page: Continue watching, Recently added, then one rail per library. No hero ⇒
"You library is empty" notice.

### Browse
Header: library name + `N titles` (or `N collections · M issues` for comics).
Sort select (Title / Year / Recently added) except music. Music: tabs Home,
Albums (count), Artists (count), Songs (count); Home = greeting by hour, 2×4
quick-pick tiles with play affordance, Recently played (music cards), Artists
(circles), Albums. Books/audiobooks: author cards (2×2 collage or single cover,
count badge) → author page with series shelves. Comics: franchise cards →
series cards → issue rack with shelf plank and issue tags. A–Z sticky rail for
movies/TV/mixed, jump-to-letter with repeated scroll correction. Infinite scroll
sentinel with spinner.

### Detail
Backdrop, poster, logo (fallback title), meta dot row (year, rating badge,
duration, "Ends at", seasons count), tagline, genres, plot. Actions: Play/Resume
(if playable), Read (book/comic), Watched/Unwatch, favourite circle, download
circle (if `can_download`). Bodies: series → season tabs + episode rows (thumb,
number, title, duration·plot, Details, play); season → episode rows; album →
Play album + numbered tracks (disc.track), now-playing glyph; artist → album
grid; audiobook → Play all + chapters with resume hint; podcast → Play latest +
episodes newest-first; book/comic → details + Read; otherwise metadata grid +
streams disclosure + Similar rail.

### Player
Top bar: back, title, container, mode tag (Direct / Stream copy / CPU), download,
"Try CPU transcode". Stage: video with custom controls; audio-only stage with
art/title/seek; unsupported card with reason, companion/download/copy-link/CPU
options. Controls: seek bar (buffer + played), −10 / play / +10, time, mute +
volume, audio menu (tracks + mode), subtitles menu (off, tracks, delay ±0.1 s,
size S/M/L, background on/off), settings (speed, mode, codecs, reason), help
(keyboard), PiP, fullscreen. Auto-hide 3 s while playing; tap/click wakes.
Keyboard: Space, ←/→ (±10, Shift ±30), J/L, M, F, C, ?, Esc. Up Next card with
8 s countdown after an episode ends.

### Reader
Top bar (back, title, chapter select). EPUB: scrollable chapter content,
previous/next chapter, theme background. Comic/PDF: page image fit-to-screen,
page counter, prev/next, jump-to-page field, zoom gesture.

### Live TV
Header + favourites filter + search + group filter; guide grid (channels rows ×
time columns, 30 min cells, now indicator); click channel → player. Health state
shown as subtle dot; empty state explains sources are configured in admin.

### Settings
Sections: Appearance (nav position, density, accent hex + reset, backdrops,
watched badges), Playback, Account (username/role, sign out, revoke other
sessions), Security (MFA enable/disable, recovery codes, app passwords), Devices
(companion pairing code + paired devices + revoke), Diagnostics (error reporting,
server URL, core version).

### Auth
Signed-out landing (starfield + accent glow, wordmark, "Everything you watch,
living at home", Create account / Sign in), Login (centered card, server origin,
username/password, MFA stage, error line), Signup (invite token/code, username,
password, pending approval state).

## 5. Interaction and accessibility

- Visible 2 px accent focus ring on all focusables; Escape closes menus.
- Search shortcut `/` on desktop; TV: search is a destination.
- All tappable targets ≥ 44 dp on touch/remote; icon buttons 36 dp with 44 dp
  hit padding on TV.
- Contrast: text `#F5F5F5` on `#101010` ≈ 17:1; muted `#B3B3B3` ≈ 8:1; accent
  used for state, never as the only signal (icons/labels accompany).
- Reduce motion: disable hero auto-rotation and route fades when the platform
  requests it.
- Content descriptions on every icon-only control; decorative art marked as such.
