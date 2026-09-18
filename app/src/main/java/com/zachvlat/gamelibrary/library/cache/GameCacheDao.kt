package com.zachvlat.gamelibrary.library.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GameCacheDao {

    @Query("SELECT * FROM cached_games WHERE store = :store ORDER BY title ASC")
    suspend fun getGamesForStore(store: String): List<CachedGame>

    @Query("SELECT * FROM cached_games ORDER BY title ASC")
    suspend fun getAllGames(): List<CachedGame>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<CachedGame>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: CachedGame)

    @Query("DELETE FROM cached_games WHERE store = :store")
    suspend fun deleteGamesForStore(store: String)

    @Query("DELETE FROM cached_games")
    suspend fun deleteAll()

    @Query("SELECT MAX(last_fetched_at) FROM cached_games WHERE store = :store")
    suspend fun getLastFetchedAt(store: String): Long?

    @Transaction
    suspend fun replaceGamesForStore(store: String, games: List<CachedGame>) {
        deleteGamesForStore(store)
        insertGames(games)
    }
}
