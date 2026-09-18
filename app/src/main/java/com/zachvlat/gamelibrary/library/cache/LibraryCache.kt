package com.zachvlat.gamelibrary.library.cache

import com.zachvlat.gamelibrary.library.model.GameInfo
import com.zachvlat.gamelibrary.library.model.Store

class LibraryCache(private val database: AppDatabase) {

    companion object {
        private const val DEFAULT_TTL_MS = 7 * 24 * 60 * 60 * 1000L
    }

    private val dao = database.gameCacheDao()

    suspend fun getCachedGames(store: Store, ttlMs: Long = DEFAULT_TTL_MS): List<GameInfo>? {
        val lastFetched = dao.getLastFetchedAt(store.name) ?: return null
        if (System.currentTimeMillis() - lastFetched > ttlMs) {
            return null
        }
        return dao.getGamesForStore(store.name).map { it.toGameInfo() }
    }

    suspend fun getCachedAllGames(ttlMs: Long = DEFAULT_TTL_MS): Map<Store, List<GameInfo>>? {
        val games = dao.getAllGames()
        if (games.isEmpty()) return null
        val byStore = games.groupBy { Store.valueOf(it.store) }
        val allFresh = byStore.all { (_, list) ->
            val lastFetched = list.maxOfOrNull { it.lastFetchedAt } ?: return@all true
            System.currentTimeMillis() - lastFetched <= ttlMs
        }
        if (!allFresh) return null
        return byStore.mapValues { (_, list) -> list.map { it.toGameInfo() } }
    }

    suspend fun cacheGames(store: Store, games: List<GameInfo>) {
        val now = System.currentTimeMillis()
        val cachedGames = games.map { it.toCachedGame(now) }
        dao.replaceGamesForStore(store.name, cachedGames)
    }

    suspend fun updateGame(game: GameInfo) {
        dao.insertGame(game.toCachedGame(System.currentTimeMillis()))
    }

    suspend fun invalidate(store: Store) {
        dao.deleteGamesForStore(store.name)
    }

    suspend fun invalidateAll() {
        dao.deleteAll()
    }
}

private fun CachedGame.toGameInfo(): GameInfo {
    return GameInfo(
        store = Store.valueOf(store),
        appName = appName,
        title = title,
        developer = developer,
        description = description,
        artCover = artCover,
        artSquare = artSquare,
        artLogo = artLogo,
        artBackground = artBackground,
        releaseDate = releaseDate,
        genres = genres?.split(",")?.filter { it.isNotBlank() },
        canRunOffline = canRunOffline,
        storeUrl = storeUrl,
        isLinuxNative = isLinuxNative,
        isMacNative = isMacNative
    )
}

private fun GameInfo.toCachedGame(fetchedAt: Long): CachedGame {
    return CachedGame(
        id = "${store.name}_${appName}",
        store = store.name,
        appName = appName,
        title = title,
        developer = developer,
        description = description,
        artCover = artCover,
        artSquare = artSquare,
        artLogo = artLogo,
        artBackground = artBackground,
        releaseDate = releaseDate,
        genres = genres?.joinToString(","),
        canRunOffline = canRunOffline,
        storeUrl = storeUrl,
        isLinuxNative = isLinuxNative,
        isMacNative = isMacNative,
        lastFetchedAt = fetchedAt
    )
}
