package dev.starhold.core.network

import dev.starhold.core.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** URL + headers a platform player needs to fetch media with auth. */
data class MediaRequest(val url: String, val headers: Map<String, String>)

private val StarholdJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
}

/** Builds the shared HTTP client. Platform engines are picked up from the classpath. */
fun defaultHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(StarholdJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 12_000
        socketTimeoutMillis = 30_000
    }
}

/**
 * The complete HTTP surface the native UI uses. Mirrors
 * `wyrmhoard-server/web/src/lib/api.ts`; see `docs/02-API-CONTRACT.md`.
 */
class StarholdApi(
    serverUrl: String,
    private val tokenProvider: () -> String?,
    private val http: HttpClient = defaultHttpClient(),
    private val userAgent: String = "StarholdNative/0.1.0",
) {
    val baseUrl: String = serverUrl.trim().trimEnd('/')

    fun absolute(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path else baseUrl + path

    fun authHeaders(): Map<String, String> =
        tokenProvider()?.takeIf { it.isNotBlank() }?.let { mapOf(HttpHeaders.Authorization to "Bearer $it") }
            ?: emptyMap()

    fun mediaRequest(path: String): MediaRequest = MediaRequest(absolute(path), authHeaders())

    // ---------------------------------------------------------------- plumbing

    private fun url(path: String, params: List<Pair<String, String>> = emptyList()): String {
        val builder = URLBuilder(absolute(path))
        params.forEach { (key, value) -> builder.parameters.append(key, value) }
        return builder.buildString()
    }

    private suspend fun request(
        method: String,
        path: String,
        params: List<Pair<String, String>> = emptyList(),
        body: String? = null,
        authenticated: Boolean = true,
    ): HttpResponse {
        val target = url(path, params)
        val response = try {
            when (method) {
                "GET" -> http.get(target) { configure(authenticated) }
                "POST" -> http.post(target) {
                    configure(authenticated)
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }
                "PUT" -> http.put(target) {
                    configure(authenticated)
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }
                "DELETE" -> http.delete(target) { configure(authenticated) }
                else -> error("Unsupported method $method")
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: Throwable) {
            throw offlineException(e)
        }
        ensureSuccess(response)
        return response
    }

    private fun HttpRequestBuilder.configure(authenticated: Boolean) {
        header(HttpHeaders.UserAgent, userAgent)
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        if (authenticated) {
            tokenProvider()?.takeIf { it.isNotBlank() }?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        var message = "HTTP ${response.status.value}"
        var code = ""
        try {
            val text = response.bodyAsText()
            if (text.isNotBlank()) {
                val obj = runCatching { StarholdJson.parseToJsonElement(text) }.getOrNull()
                if (obj != null) {
                    val error = obj.objectOrNull()?.get("error")?.stringOrNull()
                    val parsedCode = obj.objectOrNull()?.get("code")?.stringOrNull()
                    if (!error.isNullOrBlank()) message = error
                    if (!parsedCode.isNullOrBlank()) code = parsedCode
                }
            }
        } catch (_: Throwable) {
            // Body was binary or empty; keep the status text.
        }
        throw ApiException(message, response.status.value, code)
    }

    private suspend inline fun <reified T> getJson(
        path: String,
        params: List<Pair<String, String>> = emptyList(),
    ): T = request("GET", path, params).body()

    private suspend inline fun <reified T> sendJson(
        method: String,
        path: String,
        body: String? = null,
        params: List<Pair<String, String>> = emptyList(),
        authenticated: Boolean = true,
    ): T {
        val response = request(method, path, params, body, authenticated)
        if (response.status.value == 204) {
            @Suppress("UNCHECKED_CAST")
            return Unit as T
        }
        return response.body()
    }

    // ------------------------------------------------------------------- auth

    @Serializable
    private data class LoginResponse(
        @kotlinx.serialization.SerialName("access_token") val accessToken: String = "",
        val user: MeUser = MeUser(),
    )

    suspend fun login(username: String, password: String, mfaCode: String? = null): Session {
        val payload = buildString {
            append("{\"username\":").append(quote(username))
            append(",\"password\":").append(quote(password))
            if (!mfaCode.isNullOrBlank()) append(",\"mfa_code\":").append(quote(mfaCode))
            append('}')
        }
        val response: LoginResponse = sendJson("POST", "/api/v1/auth/login", payload, authenticated = false)
        require(response.accessToken.isNotBlank()) { "Server returned no token" }
        return Session(
            token = response.accessToken,
            username = response.user.username.ifBlank { username },
            role = response.user.role.ifBlank { "user" },
            canDownload = response.user.canDownload ?: true,
        )
    }

    suspend fun me(): MeUser = getJson("/api/v1/me")

    @Serializable
    data class ServerHealth(val status: String = "", val version: String = "", val name: String = "")

    /** Public reachability probe used before a session exists. */
    suspend fun health(): ServerHealth =
        request("GET", "/api/v1/health", authenticated = false).body()

    suspend fun logout() {
        runCatching { request("POST", "/api/v1/auth/logout") }
    }

    suspend fun revokeOtherSessions(): Int {
        val body = request("POST", "/api/v1/auth/revoke-others").bodyAsText()
        return runCatching {
            StarholdJson.parseToJsonElement(body).objectOrNull()?.get("revoked")?.contentOrNull()?.toIntOrNull()
        }.getOrNull() ?: 0
    }

    suspend fun capabilities(): Capabilities = getJson("/api/v1/capabilities")

    // -------------------------------------------------------------- libraries

    suspend fun libraries(): List<Library> = getJson("/api/v1/libraries")

    suspend fun items(query: ItemQuery = ItemQuery()): List<Item> {
        val params = buildList {
            query.libraryId?.let { add("library_id" to it) }
            query.type?.let { add("type" to it) }
            query.parentId?.let { add("parent_id" to it) }
            query.genre?.let { add("genre" to it) }
            if (query.sort != "name") add("sort" to query.sort)
            add("limit" to query.limit.toString())
            add("offset" to query.offset.toString())
        }
        return getJson("/api/v1/items", params)
    }

    suspend fun item(id: String): Item = getJson("/api/v1/items/$id")

    suspend fun similar(id: String): List<Item> = getJson("/api/v1/items/$id/similar")

    suspend fun search(query: String): List<Item> =
        getJson("/api/v1/search", listOf("q" to query))

    suspend fun genres(libraryId: String? = null, type: String? = null): List<GenreCount> = getJson(
        "/api/v1/items/genres",
        buildList {
            libraryId?.let { add("library_id" to it) }
            type?.let { add("type" to it) }
        },
    )

    suspend fun history(): List<HistoryEntry> = getJson("/api/v1/items/history")

    suspend fun libraryCount(libraryId: String, type: String? = null): Int =
        getJson<CountResponse>(
            "/api/v1/libraries/$libraryId/count",
            type?.let { listOf("type" to it) } ?: emptyList(),
        ).count

    suspend fun itemsCount(libraryId: String? = null, type: String? = null): Int =
        getJson<CountResponse>(
            "/api/v1/items/count",
            buildList {
                libraryId?.let { add("library_id" to it) }
                type?.let { add("type" to it) }
            },
        ).count

    // ------------------------------------------------------------- play state

    suspend fun state(id: String): PlaybackState = getJson("/api/v1/items/$id/state")

    suspend fun setState(
        id: String,
        positionSeconds: Int? = null,
        played: Boolean? = null,
        favorite: Boolean? = null,
    ) {
        val payload = buildString {
            append('{')
            val fields = buildList {
                positionSeconds?.let { add("\"position_seconds\":$it") }
                played?.let { add("\"played\":$it") }
                favorite?.let { add("\"favorite\":$it") }
            }
            append(fields.joinToString(","))
            append('}')
        }
        request("PUT", "/api/v1/items/$id/state", body = payload)
    }

    suspend fun favorite(id: String) {
        request("POST", "/api/v1/items/$id/favorite")
    }

    // --------------------------------------------------------------- playback

    suspend fun playbackDecision(
        id: String,
        caps: ClientCapabilities,
        audioIndex: Int = -1,
    ): PlaybackDecision = getJson(
        "/api/v1/items/$id/playback",
        buildList {
            add("containers" to caps.containers.joinToString(","))
            add("video" to caps.video.joinToString(","))
            add("audio" to caps.audio.joinToString(","))
            if (audioIndex >= 0) add("audio_index" to audioIndex.toString())
        },
    )

    suspend fun playbackInfo(id: String): PlaybackInfo {
        val raw: JellyfinPlaybackInfoResponse = sendJson("POST", "/Items/$id/PlaybackInfo")
        val source = raw.mediaSources.firstOrNull() ?: JellyfinMediaSource()
        return PlaybackInfo(
            id = source.id,
            container = source.container,
            size = source.size,
            streams = source.mediaStreams.map {
                MediaStream(
                    index = it.index,
                    codec = it.codec,
                    type = it.type,
                    language = it.language,
                    isDefault = it.isDefault,
                )
            },
        )
    }

    suspend fun resume(): List<Item> =
        getJson<JellyfinList>("/Items/Resume").Items.map { it.toItem() }
            .filter { (it.positionSeconds ?: 0) > 0 }

    suspend fun latest(): List<Item> = getJson<List<JellyfinItem>>("/Items/Latest").map { it.toItem() }

    suspend fun seasons(seriesId: String): List<Item> =
        getJson<JellyfinList>("/Shows/$seriesId/Seasons").Items.map { it.toItem() }

    suspend fun episodes(seriesId: String, seasonId: String? = null): List<Item> =
        getJson<JellyfinList>(
            "/Shows/$seriesId/Episodes",
            seasonId?.let { listOf("SeasonId" to it) } ?: emptyList(),
        ).Items.map { it.toItem() }

    // ------------------------------------------------------------------- urls

    fun streamUrl(id: String): String = "/api/v1/items/$id/stream"

    fun remuxUrl(id: String, start: Int, audioIndex: Int, caps: ClientCapabilities): String {
        val params = buildList {
            add("start" to start.coerceAtLeast(0).toString())
            add("containers" to caps.containers.joinToString(","))
            add("video" to caps.video.joinToString(","))
            add("audio" to caps.audio.joinToString(","))
            if (audioIndex >= 0) add("audio_index" to audioIndex.toString())
        }
        return "/api/v1/items/$id/remux?" + params.joinToString("&") { (k, v) -> "$k=$v" }
    }

    fun transcodeUrl(id: String, start: Int = 0, audioIndex: Int = -1): String {
        val params = buildList {
            add("start" to start.toString())
            if (audioIndex >= 0) add("audio_index" to audioIndex.toString())
        }
        return "/api/v1/items/$id/transcode?" + params.joinToString("&") { (k, v) -> "$k=$v" }
    }

    fun subtitleUrl(id: String, index: Int): String = "/Videos/$id/$id/Subtitles/$index/Stream.vtt"

    fun imageUrl(id: String, width: Int? = null): String =
        "/Items/$id/Images/Primary" + (width?.let { "?maxWidth=$it" } ?: "")

    fun backdropUrl(id: String, width: Int? = null): String =
        "/Items/$id/Images/Backdrop" + (width?.let { "?maxWidth=$it" } ?: "")

    fun logoUrl(id: String): String = "/Items/$id/Images/Logo"

    fun downloadUrl(id: String): String = "/Items/$id/Download"

    fun readerFileUrl(id: String): String = "/api/v1/items/$id/file"

    fun epubChapterUrl(id: String, chapter: Int): String = "/api/v1/items/$id/epub/$chapter"

    fun readerPageUrl(id: String, page: Int): String = "/api/v1/items/$id/pages/$page"

    fun liveStreamUrl(channelId: String): String = "/api/v1/livetv/channels/$channelId/stream"

    fun podcastAudioUrl(id: String): String = "/api/v1/podcasts/$id/audio"

    // -------------------------------------------------------------- binary/raw

    suspend fun bytes(path: String): ByteArray? {
        return try {
            val response = request("GET", path)
            response.body<ByteArray>()
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun text(path: String): String? {
        return try {
            request("GET", path).bodyAsText()
        } catch (_: Throwable) {
            null
        }
    }

    // ------------------------------------------------------------------ books

    suspend fun epubInfo(id: String): EpubInfo = getJson("/api/v1/items/$id/epub")

    suspend fun pageInfo(id: String): PageInfo = getJson("/api/v1/items/$id/pages")

    suspend fun epubChapterHtml(id: String, chapter: Int): String =
        request("GET", "/api/v1/items/$id/epub/$chapter").bodyAsText()

    // ---------------------------------------------------------------- live tv

    suspend fun liveChannels(group: String = "", query: String = ""): List<LiveChannel> = getJson(
        "/api/v1/livetv/channels",
        buildList {
            if (group.isNotBlank()) add("group" to group)
            if (query.isNotBlank()) add("q" to query)
        },
    )

    suspend fun liveGroups(): List<String> = getJson("/api/v1/livetv/groups")

    suspend fun liveGuide(from: String, to: String, channelIds: List<String> = emptyList()): List<LiveProgramme> =
        getJson(
            "/api/v1/livetv/guide",
            buildList {
                add("from" to from)
                add("to" to to)
                channelIds.forEach { add("channel_id" to it) }
            },
        )

    suspend fun setLiveFavorite(id: String, favorite: Boolean) {
        request(if (favorite) "PUT" else "DELETE", "/api/v1/livetv/favorites/$id")
    }

    // ---------------------------------------------------------------- account

    suspend fun uiPreferences(): UiPreferences =
        getJson<UiPreferencesEnvelope>("/api/v1/me/ui-preferences").preferences

    suspend fun saveUiPreferences(prefs: UiPreferences): UiPreferences =
        sendJson<UiPreferencesEnvelope>(
            "PUT",
            "/api/v1/me/ui-preferences",
            StarholdJson.encodeToString(UiPreferencesEnvelope.serializer(), UiPreferencesEnvelope(prefs)),
        ).preferences

    suspend fun errorReporting(): Boolean =
        getJson<BooleanHolder>("/api/v1/me/error-reporting").enabled

    suspend fun setErrorReporting(enabled: Boolean) {
        request("PUT", "/api/v1/me/error-reporting", body = "{\"enabled\":$enabled}")
    }

    @Serializable
    private data class BooleanHolder(val enabled: Boolean = false)

    suspend fun mfaStatus(): MfaStatus = getJson("/api/v1/mfa")

    suspend fun mfaBegin(): MfaBegin = sendJson("POST", "/api/v1/mfa/begin")

    suspend fun mfaConfirm(code: String): MfaConfirm =
        sendJson("POST", "/api/v1/mfa/confirm", "{\"code\":${quote(code)}}")

    suspend fun mfaDisable(password: String, code: String): MfaStatus =
        sendJson(
            "POST",
            "/api/v1/mfa/disable",
            "{\"password\":${quote(password)},\"code\":${quote(code)}}",
        )

    suspend fun appPasswords(): List<AppPassword> = getJson("/api/v1/me/app-passwords")

    suspend fun createAppPassword(name: String): CreatedAppPassword =
        sendJson("POST", "/api/v1/me/app-passwords", "{\"name\":${quote(name)}}")

    suspend fun revokeAppPassword(id: String) {
        request("DELETE", "/api/v1/me/app-passwords/$id")
    }

    suspend fun companionDevices(): List<CompanionDevice> = getJson("/api/v1/me/devices")

    suspend fun revokeDevice(id: String) {
        request("DELETE", "/api/v1/me/devices/$id")
    }

    suspend fun startCompanionPairing(): PairingStart = sendJson("POST", "/api/v1/companion/pair/start")

    suspend fun companionPairStatus(code: String): PairingStatus =
        getJson("/api/v1/companion/pair/status", listOf("code" to code))

    // ----------------------------------------------------------------- signup

    suspend fun signupStatus(): SignupStatus =
        sendJson("GET", "/api/v1/signup/status", authenticated = false)

    suspend fun validateInvite(token: String?, code: String?): InviteValidation {
        val payload = buildString {
            append('{')
            val parts = buildList {
                if (!token.isNullOrBlank()) add("\"token\":${quote(token)}")
                if (!code.isNullOrBlank()) add("\"code\":${quote(code)}")
            }
            append(parts.joinToString(","))
            append('}')
        }
        return sendJson("POST", "/api/v1/signup/validate", payload, authenticated = false)
    }

    suspend fun redeemInvite(
        token: String?,
        code: String?,
        username: String,
        password: String,
        displayName: String? = null,
    ): RedeemResult {
        val payload = buildString {
            append('{')
            val parts = buildList {
                if (!token.isNullOrBlank()) add("\"token\":${quote(token)}")
                if (!code.isNullOrBlank()) add("\"code\":${quote(code)}")
                add("\"username\":${quote(username)}")
                add("\"password\":${quote(password)}")
                if (!displayName.isNullOrBlank()) add("\"display_name\":${quote(displayName)}")
            }
            append(parts.joinToString(","))
            append('}')
        }
        return sendJson("POST", "/api/v1/signup/redeem", payload, authenticated = false)
    }

    // ------------------------------------------------------------------ misc

    private fun quote(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }
}

data class ItemQuery(
    val libraryId: String? = null,
    val type: String? = null,
    val parentId: String? = null,
    val genre: String? = null,
    val sort: String = "name",
    val limit: Int = 60,
    val offset: Int = 0,
)
