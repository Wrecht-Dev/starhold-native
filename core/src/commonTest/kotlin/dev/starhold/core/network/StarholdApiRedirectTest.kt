package dev.starhold.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StarholdApiRedirectTest {

    private fun clientFor(engine: MockEngine): HttpClient = HttpClient(engine) {
        followRedirects = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private fun bodyText(body: OutgoingContent): String = when (body) {
        is TextContent -> body.text
        is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
        else -> ""
    }

    @Test
    fun loginRepostsToHttpsAfter301() = runTest {
        val calls = mutableListOf<Pair<String, String>>()
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            calls += request.method.value to request.url.toString()
            bodies += bodyText(request.body)
            if (request.url.protocol == URLProtocol.HTTP) {
                respond(
                    content = "",
                    status = HttpStatusCode.MovedPermanently,
                    headers = headersOf(HttpHeaders.Location, "https://media.example.com${request.url.encodedPath}"),
                )
            } else {
                respond(
                    content = """{"access_token":"tok","user":{"username":"alice","role":"user"}}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val api = StarholdApi("http://media.example.com", tokenProvider = { null }, http = clientFor(engine))

        val session = api.login("alice", "secret")

        assertEquals("tok", session.token)
        assertEquals("https://media.example.com", api.baseUrl)
        assertEquals(2, calls.size)
        assertEquals("POST" to "http://media.example.com/api/v1/auth/login", calls[0])
        assertEquals("POST" to "https://media.example.com/api/v1/auth/login", calls[1])
        assertEquals(bodies[0], bodies[1])
        assertTrue(bodies[0].contains("\"password\":\"secret\""))
    }

    @Test
    fun healthGetsAdoptHttpsOrigin() = runTest {
        val engine = MockEngine { request ->
            if (request.url.protocol == URLProtocol.HTTP) {
                respond(
                    content = "",
                    status = HttpStatusCode.PermanentRedirect,
                    headers = headersOf(HttpHeaders.Location, "https://media.example.com/api/v1/health"),
                )
            } else {
                respond(
                    content = """{"status":"ok","version":"1.0","name":"Starhold"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val api = StarholdApi("http://media.example.com", tokenProvider = { null }, http = clientFor(engine))

        val health = api.health()

        assertEquals("ok", health.status)
        assertEquals("https://media.example.com", api.baseUrl)
    }
}
