# 02 — API contract used by the native client

Source of truth: the server's native handler and the web SPA's client
(`wyrmhoard-server/web/src/lib/api.ts`). The native client mirrors that subset;
it does not invent endpoints.

Base URL: whatever the user enters, e.g. `https://beta.mithraic.cloud`.
All paths below are relative to it. JSON unless stated.

## 1. Authentication

```http
POST /api/v1/auth/login
Content-Type: application/json
{"username":"...","password":"...","mfa_code":"optional"}
```

Success `200`:

```json
{"access_token":"<opaque>","user":{"username":"addy","role":"admin","can_download":true}}
```

- MFA required: non-2xx with `{"error":"...","code":"mfa_required"}`; retry with
  `mfa_code`.
- Wrong TOTP: `code: "mfa_invalid"`.
- Token is opaque, SHA-256-stored server-side, 24 h expiry.

Authenticated requests:

```http
Authorization: Bearer <access_token>
```

`GET /api/v1/me` → `{id, username, role, can_download}`. Used to refresh cached
session policy at shell start.

`POST /api/v1/auth/logout` revokes the token (204).

`GET /api/v1/auth/media-cookie` — web-only cookie re-issue; **not needed** by
native (it sends the bearer header on media requests).

`POST /api/v1/auth/revoke-others` → `{revoked:n}`.

Signup/invites (public):

```text
GET  /api/v1/signup/status          -> {enabled, approval, server_name?}
POST /api/v1/signup/validate        {token?|code?} -> {valid, note?, expires_at?, requires_approval?}
POST /api/v1/signup/redeem          {token?|code?, username, password, display_name?}
                                    -> {access_token?, user?, pending_approval?}
```

Errors: `{ "error": "human message", "code": "machine_code" }`, non-2xx.
Any `401` on a non-auth route means the session is gone — clear it and show login.

## 2. Libraries and items

```text
GET  /api/v1/libraries                      -> [{id,name,type}]
GET  /api/v1/items?library_id=&type=&parent_id=&genre=&sort=name|year|added&limit=60&offset=0
                                            -> [Item]
GET  /api/v1/items/{id}                     -> Item
GET  /api/v1/items/{id}/similar             -> [Item]
GET  /api/v1/items/count?library_id=&type=  -> {count}
GET  /api/v1/libraries/{id}/count?type=     -> {count}
GET  /api/v1/items/genres?library_id=&type= -> [{name,count}]
GET  /api/v1/items/history                  -> [{id,title,type,position_seconds,played,updated_at}]
GET  /api/v1/search?q=                      -> [Item]
```

Native `Item` (snake_case), as parsed by the web's `fromNative`:

```json
{
  "id":"hex32","type":"movie|series|season|episode|album|artist|audio|audiobook|book|comic|video|game|podcast|podcastepisode",
  "title":"...","year":2026,"plot":"...","runtime_seconds":7200,
  "parent_id":"...","season_number":1,"episode_number":2,
  "genres":["..."],"sort_title":"...","series":"...","series_id":"...","series_index":1,
  "franchise":"...","issue":3,"artist":"...","album_artist":"...","track_number":1,
  "disc_number":1,"compilation":false,"has_image":true,"tagline":"...",
  "official_rating":"PG-13","played":false,"favorite":false,"position_seconds":1234
}
```

Missing fields are omitted; treat every optional field as nullable. `has_image`
is the gate for poster rendering.

## 3. Play state

```text
GET /api/v1/items/{id}/state  -> {position_seconds, played, favorite}
PUT /api/v1/items/{id}/state  {"position_seconds":n,"played":bool,"favorite":bool} -> 2xx
POST /api/v1/items/{id}/favorite -> toggles
```

Played semantics: web writes `position_seconds = runtime` + `played=true` on a
manual "watched"; the player writes `position_seconds=0, played=true` at
end-of-file and `position_seconds=position` otherwise. Progress is PUT every 5 s
(video) / 8 s (audio mini-player) and on dispose.

## 4. Playback

Server decides the mode from reported client capabilities:

```http
GET /api/v1/items/{id}/playback?containers=mp4,mkv&video=h264,hevc&audio=aac,flac&audio_index=-1
```

Response (as normalised by `api.ts`):

```json
{
  "id":"...","type":"movie","mode":"direct|remux|unsupported","reason":"...",
  "container":"mkv","video":{"index":0,"codec":"hevc","language":"","default":true},
  "audio_index":1,"audio_codec":"eac3",
  "audio_tracks":[{"index":1,"codec":"eac3","language":"eng","default":true,"channels":6,"title":"","forced":false,"external":false}],
  "subtitles":[{"index":2,"codec":"subrip","language":"eng","title":"","default":false,"forced":false,"external":true,"url":"..."}],
  "duration_seconds":7200,"size":123456789,"mime":"video/x-matroska",
  "can_transcode":false,
  "direct_url":"/api/v1/items/{id}/stream",
  "remux_url":"/api/v1/items/{id}/remux"
}
```

Stream URLs (all require the bearer header natively):

```text
GET /api/v1/items/{id}/stream                      original bytes, Range
GET /api/v1/items/{id}/remux?start=S&audio_index=A&containers=&video=&audio=
GET /api/v1/items/{id}/transcode?start=S&audio_index=A   (opt-in CPU)
GET /Items/{id}/Download                           attachment download
GET /Videos/{id}/{id}/Subtitles/{index}/Stream.vtt subtitle (WebVTT)
```

Subtitle bodies are absolute-timeline WebVTT. A remux starting at `S` restarts
the media timeline at 0, so cue times must be shifted by `-S` before display.

Jellyfin routes the web SPA also uses:

```text
POST /Items/{id}/PlaybackInfo                 -> MediaSources[0].{Id,Container,Size,MediaStreams[]}
GET  /Items/Resume                            -> {Items:[JellyfinItem]}
GET  /Items/Latest                            -> [JellyfinItem]
GET  /Shows/{id}/Seasons                      -> {Items:[...]}
GET  /Shows/{id}/Episodes[?SeasonId=]         -> {Items:[...]}
GET  /Items/{id}/Images/Primary?maxWidth=     image bytes
GET  /Items/{id}/Images/Backdrop?maxWidth=    image bytes
GET  /Items/{id}/Images/Logo                  transparent logo
```

Jellyfin fields used: `Id, Name, Type, ProductionYear, Overview, RunTimeTicks,
ParentId, SeriesId, SeriesName, ParentIndexNumber, IndexNumber, AlbumArtist,
Artists[], Taglines[], OfficialRating, UserData{Played,IsFavorite,
PlaybackPositionTicks}, HasPrimaryImage`. Ticks ÷ 10,000,000 = seconds.

## 5. Books, comics, podcasts

```text
GET /api/v1/items/{id}/epub                 -> {title,author,language,chapters:[{index,title}]}
GET /api/v1/items/{id}/epub/{n}             -> sanitised chapter HTML (same-origin)
GET /api/v1/items/{id}/epub/resource?path=  -> OPF manifest resource (images/css)
GET /api/v1/items/{id}/pages                -> {pages:n, kind:"comic|pdf", format:"..."}
GET /api/v1/items/{id}/pages/{n}            -> rendered page image
GET /api/v1/items/{id}/file                 -> original download (MIME, attachment, Range)
GET /api/v1/podcasts/shows                  -> shows
GET /api/v1/podcasts/episodes/{id}          -> episode detail
GET /api/v1/podcasts/{id}/audio             -> enclosure proxy (Range)
```

Chapter HTML is sanitised by the server; the native reader parses a safe subset
(headings, paragraphs, emphasis, blockquote, lists, images, links) and never
executes scripts.

## 6. Live TV

```text
GET    /api/v1/livetv/channels?group=&q=    -> [{id,name,number,logo?,group?,favorite,health}]
GET    /api/v1/livetv/groups                -> [string]
GET    /api/v1/livetv/guide?from=ISO&to=ISO&channel_id=... -> [Programme]
GET    /api/v1/livetv/channels/{id}/stream  -> HLS/TS stream (auth header)
PUT    /api/v1/livetv/favorites/{id}
DELETE /api/v1/livetv/favorites/{id}
```

`Programme`: `{id, channel_id, title, subtitle?, description?, start_at, stop_at,
categories?}` with RFC 3339 timestamps. Guide range in the web UI is built with
ISO strings; the native client requests ~6 h forward from now.

Live streams may be HLS (`.m3u8`, supported by Media3/AVPlayer/mpv) or direct
MPEG-TS (Media3/mpv; AVPlayer generally cannot play raw TS — show an honest
error on iOS instead of a spinner).

## 7. Account / security / settings

```text
GET  /api/v1/mfa                          -> {enabled}
POST /api/v1/mfa/begin                    -> {secret, uri}      (otpauth://)
POST /api/v1/mfa/confirm {code}           -> {enabled:true, recovery_codes:[...]}
POST /api/v1/mfa/disable {password,code}  -> {enabled:false}

GET  /api/v1/me/app-passwords             -> [{id,name,prefix,created_at,last_used_at?,revoked_at?}]
POST /api/v1/me/app-passwords {name}      -> {password, app_password}
DEL  /api/v1/me/app-passwords/{id}

GET  /api/v1/me/error-reporting           -> {enabled}
PUT  /api/v1/me/error-reporting {enabled}

GET  /api/v1/me/ui-preferences            -> {preferences:{navPosition,density,accentColor,showBackdrops,showWatchedBadges}}
PUT  /api/v1/me/ui-preferences            same envelope
POST /api/v1/companion/pair/start         -> {code, expires_at}
GET  /api/v1/companion/pair/status?code=  -> {status, device?}
GET  /api/v1/me/devices                   -> [{id,name,kind?,last_seen?}]
DEL  /api/v1/me/devices/{id}
GET  /api/v1/capabilities                 -> {transcode, remux, cpu}
```

`ui-preferences` allowed values (server mirrors the web client):
`navPosition ∈ top|bottom|left|right`, `density ∈ comfortable|compact`,
`accentColor` = `""` or `#rrggbb`, booleans.

## 8. Errors, limits, headers

- JSON error envelope `{error, code}`; 401 clears the session.
- Request bodies are capped by the server (errors endpoint 64 KiB); the client
  truncates diagnostics.
- No CORS concerns natively, but the server may reject odd User-Agents only for
  setup flows; send a normal UA such as `StarholdNative/0.1.0 (<platform>)`.
- Image requests may return 404/500 when art is absent or unrenderable; the UI
  must fall back to lettered tiles without looking broken.
- Range requests are required for seeking; Ktor streams the response body, the
  platform player performs its own ranged fetches with the auth header.

## 9. Deliberate non-goals for v1

- `/plugin/*` pages are server-hosted HTML/JS; no webview shell in the native
  client. Shown as "available in the web app" where a plugin nav entry exists.
- Casting/session control, trailers, theme media, filters, persons/studios.
- Server setup wizard and admin console stay in the admin web app.
