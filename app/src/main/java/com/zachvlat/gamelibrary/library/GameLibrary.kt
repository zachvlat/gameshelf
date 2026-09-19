package com.zachvlat.gamelibrary.library

import android.content.Context
import android.content.SharedPreferences
import com.zachvlat.gamelibrary.BuildConfig
import com.zachvlat.gamelibrary.library.auth.EncryptedTokenStorage
import com.zachvlat.gamelibrary.library.auth.TokenStorage
import com.zachvlat.gamelibrary.library.cache.AppDatabase
import com.zachvlat.gamelibrary.library.cache.LibraryCache
import com.zachvlat.gamelibrary.library.model.GameInfo
import com.zachvlat.gamelibrary.library.model.NowPlayingInfo
import com.zachvlat.gamelibrary.library.model.Store
import com.zachvlat.gamelibrary.library.store.StoreClient
import com.zachvlat.gamelibrary.library.store.amazon.AmazonStoreClient
import com.zachvlat.gamelibrary.library.store.epic.EpicStoreClient
import com.zachvlat.gamelibrary.library.store.gog.GogStoreClient
import com.zachvlat.gamelibrary.library.store.ea.EaStoreClient
import com.zachvlat.gamelibrary.library.store.itch.ItchStoreClient
import com.zachvlat.gamelibrary.library.store.igdb.IgdbClient
import com.zachvlat.gamelibrary.library.store.manual.ManualStoreClient
import com.zachvlat.gamelibrary.library.store.steam.SteamStoreClient
import com.zachvlat.gamelibrary.library.store.ubisoft.UbisoftStoreClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class GameLibrary private constructor(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage,
    private var cache: LibraryCache,
    private val nowPlayingPrefs: SharedPreferences,
    private val completedPrefs: SharedPreferences,
    val epic: EpicStoreClient,
    val gog: GogStoreClient,
    val amazon: AmazonStoreClient,
    val steam: SteamStoreClient,
    val itch: ItchStoreClient,
    val ea: EaStoreClient,
    val manual: ManualStoreClient,
    val ubisoft: UbisoftStoreClient,
    val igdb: IgdbClient
) {
    private val npJson = Json { ignoreUnknownKeys = true }

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var _nowPlaying by mutableStateOf(readNowPlaying())
    val nowPlaying: List<NowPlayingInfo> get() = _nowPlaying

    fun updateNowPlaying(info: NowPlayingInfo) {
        if (info.completionPercent >= 100) {
            toggleCompleted(info.store, info.appName)
            removeNowPlaying(info.store, info.appName)
            return
        }
        val list = _nowPlaying.toMutableList()
        val idx = list.indexOfFirst { it.store == info.store && it.appName == info.appName }
        if (idx >= 0) {
            list[idx] = info
        } else {
            list.add(info)
        }
        saveNowPlaying(list)
    }

    fun removeNowPlaying(store: Store, appName: String) {
        val list = _nowPlaying.filterNot { it.store == store && it.appName == appName }
        saveNowPlaying(list)
    }

    private fun saveNowPlaying(list: List<NowPlayingInfo>) {
        nowPlayingPrefs.edit().putString("list", npJson.encodeToString(list)).apply()
        _nowPlaying = list
    }

    private fun readNowPlaying(): List<NowPlayingInfo> {
        val raw = nowPlayingPrefs.getString("list", null) ?: return emptyList()
        return try {
            npJson.decodeFromString<List<NowPlayingInfo>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val _completedKeys = mutableStateOf(readCompletedKeys())

    fun isCompleted(store: Store, appName: String): Boolean {
        return _completedKeys.value.contains("${store.name}:$appName")
    }

    fun toggleCompleted(store: Store, appName: String) {
        val key = "${store.name}:$appName"
        val set = _completedKeys.value.toMutableSet()
        if (set.contains(key)) set.remove(key) else set.add(key)
        completedPrefs.edit().putStringSet("completed", set).apply()
        _completedKeys.value = set
    }

    private fun readCompletedKeys(): Set<String> {
        return completedPrefs.getStringSet("completed", emptySet()) ?: emptySet()
    }

    val allClients: Map<Store, StoreClient> = mapOf(
        Store.EPIC to epic,
        Store.GOG to gog,
        Store.AMAZON to amazon,
        Store.STEAM to steam,
        Store.ITCH to itch,
        Store.EA to ea,
        Store.MANUAL to manual,
        Store.UBISOFT to ubisoft
    )

    private val enrichedGames = mutableMapOf<String, GameInfo>()

    suspend fun enrichGame(game: GameInfo): GameInfo {
        if (!igdb.isConfigured()) return game
        val key = "${game.store.name}:${game.appName}"
        enrichedGames[key]?.let { return it }
        val enriched = try {
            igdb.enrichOne(game)
        } catch (_: Exception) {
            game
        }
        enrichedGames[key] = enriched
        if (enriched != game) {
            cache.updateGame(enriched)
        }
        return enriched
    }

    private suspend fun fetchAndCache(store: Store, client: StoreClient): List<GameInfo> {
        val games = client.refreshLibrary()
        if (games.isEmpty()) return games
        cache.cacheGames(store, games)
        backgroundScope.launch {
            val enriched = try {
                igdb.enrich(games)
            } catch (_: Exception) {
                games
            }
            cache.cacheGames(store, enriched)
        }
        return games
    }

    fun getClient(store: Store): StoreClient = allClients[store]
        ?: throw IllegalArgumentException("Unknown store: $store")

    suspend fun getAllGames(forceRefresh: Boolean = false): Map<Store, List<GameInfo>> {
        if (!forceRefresh) {
            val cached = cache.getCachedAllGames()
            if (cached != null) return cached
        }

        val result = mutableMapOf<Store, List<GameInfo>>()
        for ((store, client) in allClients) {
            try {
                if (client.isLoggedIn()) {
                    val games = fetchAndCache(store, client)
                    result[store] = games
                }
            } catch (_: Exception) {
            }
        }
        return result
    }

    suspend fun getGamesForStore(
        store: Store,
        forceRefresh: Boolean = false
    ): List<GameInfo> {
        if (!forceRefresh) {
            val cached = cache.getCachedGames(store)
            if (cached != null) return cached
        }

        val client = getClient(store)
        val games = fetchAndCache(store, client)
        if (games.isNotEmpty()) {
            return games
        }

        val cached = cache.getCachedGames(store)
        return cached ?: games
    }

    suspend fun isLoggedIn(store: Store): Boolean {
        return getClient(store).isLoggedIn()
    }

    suspend fun logoutAll() {
        for (client in allClients.values) {
            try {
                client.logout()
            } catch (_: Exception) { }
        }
        cache.invalidateAll()
    }

    suspend fun logout(store: Store) {
        getClient(store).logout()
        cache.invalidate(store)
    }

    suspend fun addManualGame(game: GameInfo) {
        val existing = cache.getCachedGames(Store.MANUAL, ttlMs = Long.MAX_VALUE) ?: emptyList()
        cache.cacheGames(Store.MANUAL, existing + game)
    }

    suspend fun updateManualGame(game: GameInfo) {
        val existing = cache.getCachedGames(Store.MANUAL, ttlMs = Long.MAX_VALUE) ?: emptyList()
        cache.cacheGames(Store.MANUAL, existing.filter { it.appName != game.appName } + game)
    }

    suspend fun deleteManualGame(appName: String) {
        val games = cache.getCachedGames(Store.MANUAL, ttlMs = Long.MAX_VALUE) ?: return
        cache.cacheGames(Store.MANUAL, games.filter { it.appName != appName })
    }

    fun destroy() {
        backgroundScope.cancel()
        httpClient.close()
    }

    fun recreateCache(context: Context) {
        AppDatabase.closeAndClearInstance()
        val database = AppDatabase.getInstance(context)
        cache = LibraryCache(database)
    }

    companion object {
        fun create(context: Context): GameLibrary {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            }

            val httpClient = HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 15_000
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 3)
                    retryOnException(maxRetries = 3, retryOnTimeout = true)
                    exponentialDelay()
                }
                install(ContentNegotiation) {
                    json(json)
                }
                install(Logging) {
                    level = LogLevel.NONE
                }
            }

            val tokenStorage: TokenStorage = EncryptedTokenStorage(context)
            val database = AppDatabase.getInstance(context)
            val cache = LibraryCache(database)
            val nowPlayingPrefs = context.getSharedPreferences("now_playing", Context.MODE_PRIVATE)
            val completedPrefs = context.getSharedPreferences("game_completed", Context.MODE_PRIVATE)

            return GameLibrary(
                httpClient = httpClient,
                tokenStorage = tokenStorage,
                cache = cache,
                nowPlayingPrefs = nowPlayingPrefs,
                completedPrefs = completedPrefs,
                epic = EpicStoreClient(httpClient, tokenStorage),
                gog = GogStoreClient(httpClient, tokenStorage),
                amazon = AmazonStoreClient(httpClient, tokenStorage),
                steam = SteamStoreClient(httpClient, tokenStorage),
                itch = ItchStoreClient(httpClient, tokenStorage),
                ea = EaStoreClient(httpClient, tokenStorage),
                manual = ManualStoreClient(cache),
                ubisoft = UbisoftStoreClient(httpClient, tokenStorage),
                igdb = IgdbClient(
                    httpClient = httpClient,
                    clientId = BuildConfig.TWITCH_CLIENT_ID,
                    clientSecret = BuildConfig.TWITCH_CLIENT_SECRET
                )
            )
        }
    }
}
