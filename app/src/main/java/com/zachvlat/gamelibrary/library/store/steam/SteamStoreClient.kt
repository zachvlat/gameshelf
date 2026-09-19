package com.zachvlat.gamelibrary.library.store.steam

import android.util.Log
import com.zachvlat.gamelibrary.library.auth.TokenStorage
import com.zachvlat.gamelibrary.library.model.GameInfo
import com.zachvlat.gamelibrary.library.model.LoginData
import com.zachvlat.gamelibrary.library.model.Store
import com.zachvlat.gamelibrary.library.store.StoreClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class SteamStoreClient(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : StoreClient {

    companion object {
        private const val TAG = "SteamStoreClient"
        private const val KEY_PROFILE_URL = "profile_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_STEAM_ID = "steam_id"
        private const val STEAM_API_BASE = "https://api.steampowered.com"
    }

    override val store: Store get() = Store.STEAM

    private var cachedGames: List<GameInfo>? = null

    override suspend fun isLoggedIn(): Boolean {
        return getSteamId() != null
    }

    suspend fun getProfileUrl(): String? {
        return tokenStorage.getToken(store.name, KEY_PROFILE_URL)
    }

    suspend fun hasApiKey(): Boolean {
        return tokenStorage.getToken(store.name, KEY_API_KEY) != null
    }

    suspend fun getApiKey(): String? {
        return tokenStorage.getToken(store.name, KEY_API_KEY)
    }

    suspend fun setApiKey(apiKey: String) {
        tokenStorage.saveToken(store.name, KEY_API_KEY, apiKey)
    }

    suspend fun getSteamId(): String? {
        return tokenStorage.getToken(store.name, KEY_STEAM_ID)
    }

    override suspend fun getLoginData(): LoginData {
        return LoginData(url = "https://steamcommunity.com/my/games/?tab=all")
    }

    override suspend fun completeLogin(authCode: String): Boolean {
        return false
    }

    override suspend fun refreshLibrary(): List<GameInfo> {
        if (hasApiKey() && getSteamId() != null) {
            val apiGames = fetchGamesViaApi()
            if (apiGames.isNotEmpty()) {
                cachedGames = apiGames
                return apiGames
            }
        }
        return cachedGames ?: emptyList()
    }

    override suspend fun logout() {
        tokenStorage.clearTokens(store.name)
        cachedGames = null
    }

    suspend fun resolveAndSaveSteamId(profileUrl: String): String? {
        tokenStorage.saveToken(store.name, KEY_PROFILE_URL, profileUrl)
        val steamId = resolveSteamId(profileUrl)
        if (steamId != null) {
            tokenStorage.saveToken(store.name, KEY_STEAM_ID, steamId)
            Log.d(TAG, "Resolved and saved Steam ID: $steamId")
        }
        return steamId
    }

    private suspend fun resolveSteamId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.matches(Regex("""7656119\d{10}"""))) {
            return trimmed
        }
        val xmlUrl = buildProfileXmlUrl(trimmed)
        return try {
            val response = httpClient.get(xmlUrl)
            val xml = response.bodyAsText()
            val match = """<steamID64>(\d+)</steamID64>""".toRegex().find(xml)
            match?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve Steam ID: ${e.message}", e)
            null
        }
    }

    private fun buildProfileXmlUrl(input: String): String {
        return if (input.contains("steamcommunity.com")) {
            input
                .replace("/games/?tab=all", "")
                .replace("/games/", "")
                .trimEnd('/') + "/?xml=1"
        } else {
            "https://steamcommunity.com/id/${input.trim('/')}/?xml=1"
        }
    }

    private suspend fun fetchGamesViaApi(): List<GameInfo> {
        return try {
            val apiKey = getApiKey() ?: return emptyList()
            val steamId = getSteamId() ?: return emptyList()
            val url = "$STEAM_API_BASE/IPlayerService/GetOwnedGames/v1/"
            val response = httpClient.get(url) {
                parameter("key", apiKey)
                parameter("steamid", steamId)
                parameter("include_appinfo", 1)
                parameter("format", "json")
            }
            val text = response.bodyAsText()
            if (text.trimStart().startsWith("<")) {
                val friendly = if (text.contains("Unauthorized")) {
                    "Steam rejected your API key. Verify it at steamcommunity.com/dev/apikey and re-enter it without extra spaces."
                } else {
                    "Steam returned an unexpected page instead of your game list. Try syncing again."
                }
                throw IllegalStateException(friendly)
            }
            val apiResponse = Json { ignoreUnknownKeys = true }.decodeFromString<SteamApiGamesResponse>(text)
            val games = apiResponse.response.games ?: emptyList()
            val gameInfos = games.map { it.toGameInfo() }
            Log.d(TAG, "Fetched ${gameInfos.size} games via Steam API")
            gameInfos
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch games via Steam API: ${e.message}", e)
            throw e
        }
    }

    private fun SteamApiGame.toGameInfo(): GameInfo {
        val appId = this.appid.toString()
        val artCover = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/library_600x900.jpg"

        return GameInfo(
            store = Store.STEAM,
            appName = appId,
            title = this.name,
            developer = null,
            description = null,
            artCover = artCover,
            artSquare = artCover,
            artLogo = null,
            artBackground = null,
            releaseDate = null,
            genres = null,
            canRunOffline = false,
            storeUrl = "https://store.steampowered.com/app/$appId",
            isLinuxNative = false,
            isMacNative = false
        )
    }
}
