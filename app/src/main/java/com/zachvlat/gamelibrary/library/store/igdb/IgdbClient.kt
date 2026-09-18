package com.zachvlat.gamelibrary.library.store.igdb

import android.util.Log
import com.zachvlat.gamelibrary.library.model.GameInfo
import com.zachvlat.gamelibrary.library.model.Store
import com.zachvlat.gamelibrary.library.util.IgdbConstants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class IgdbClient(
    private val httpClient: HttpClient,
    private val clientId: String,
    private val clientSecret: String
) {
    companion object {
        private const val TAG = "IgdbClient"
        private const val MIN_REQUEST_INTERVAL_MS = 250L
        private val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
    }

    fun isConfigured(): Boolean = clientId.isNotBlank() && clientSecret.isNotBlank()

    private val mutex = Mutex()
    private var lastRequestAt = 0L
    private var accessToken: String? = null
    private var tokenExpiryAt = 0L

    private suspend fun acquireRateSlot() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val wait = MIN_REQUEST_INTERVAL_MS - (now - lastRequestAt)
            if (wait > 0) delay(wait)
            lastRequestAt = System.currentTimeMillis()
        }
    }

    private suspend fun getAccessToken(): String {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiryAt - 60_000) {
            return accessToken!!
        }
        acquireRateSlot()
        val response: HttpResponse = httpClient.post(IgdbConstants.TOKEN_URL) {
            parameter("client_id", clientId)
            parameter("client_secret", clientSecret)
            parameter("grant_type", "client_credentials")
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("IGDB token request failed: ${response.status}")
        }
        val token: IgdbTokenResponse = response.body()
        accessToken = token.access_token
        tokenExpiryAt = System.currentTimeMillis() + token.expires_in * 1000L
        return token.access_token
    }

    suspend fun enrich(games: List<GameInfo>): List<GameInfo> {
        if (!isConfigured()) return games
        return games.map { game ->
            try {
                enrichOne(game)
            } catch (e: Exception) {
                Log.w(TAG, "Enrich failed for '${game.title}': ${e.message}")
                game
            }
        }
    }

    suspend fun enrichOne(game: GameInfo): GameInfo {
        if (game.isComplete()) return game
        val igdbGame = searchGame(game.title) ?: return game
        return game.mergeFromIgdb(igdbGame)
    }

    private suspend fun searchGame(title: String): IgdbGame? {
        return try {
            val token = getAccessToken()
            acquireRateSlot()
            val query = buildString {
                append("fields name, summary, first_release_date, cover.url, involved_companies.company.name, involved_companies.developer, genres.name; ")
                append("search \"${title.replace("\"", "")}\"; ")
                append("limit 5;")
            }
            val response: HttpResponse = httpClient.post(IgdbConstants.GAMES_API) {
                header("Client-ID", clientId)
                header("Authorization", "Bearer $token")
                contentType(ContentType.Text.Plain)
                setBody(query)
            }
            if (!response.status.isSuccess()) return null
            val results: List<IgdbGame> = response.body()
            pickBestMatch(results, title)
        } catch (e: Exception) {
            Log.w(TAG, "IGDB search failed for '$title': ${e.message}")
            null
        }
    }

    private fun pickBestMatch(results: List<IgdbGame>, title: String): IgdbGame? {
        if (results.isEmpty()) return null
        return results.firstOrNull { it.name?.equals(title, ignoreCase = true) == true }
            ?: results.firstOrNull { it.name?.contains(title, ignoreCase = true) == true }
            ?: results.firstOrNull { title.contains(it.name ?: "", ignoreCase = true) }
            ?: results.first()
    }

    private fun GameInfo.isComplete(): Boolean =
        developer != null && description != null &&
            !genres.isNullOrEmpty() && releaseDate != null && artCover != null

    private fun GameInfo.mergeFromIgdb(igdb: IgdbGame): GameInfo {
        val igdbCover = igdb.cover?.url?.coverUrl()
        val effectiveCover = if (store == Store.EA) {
            igdbCover ?: artCover
        } else {
            artCover ?: igdbCover
        }
        return copy(
            developer = developer ?: igdb.developerName(),
            description = description ?: igdb.summary?.takeIf { it.isNotBlank() },
            genres = genres ?: igdb.genreNames(),
            releaseDate = releaseDate ?: igdb.releaseDateText(),
            artCover = effectiveCover
        )
    }

    private fun IgdbGame.developerName(): String? {
        val dev = involved_companies?.firstOrNull { it.developer }?.company?.name
        return dev?.takeIf { it.isNotBlank() }
            ?: involved_companies?.firstOrNull()?.company?.name?.takeIf { it.isNotBlank() }
    }

    private fun IgdbGame.genreNames(): List<String>? {
        val names = genres?.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }.orEmpty()
        return names.takeIf { it.isNotEmpty() }
    }

    private fun IgdbGame.releaseDateText(): String? {
        val epoch = first_release_date ?: return null
        return runCatching {
            dateFormatter.format(Instant.ofEpochSecond(epoch))
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun String.coverUrl(): String {
        return replace("//images.igdb.com", "https://images.igdb.com")
            .replace("t_thumb", "t_cover_big")
    }
}