package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites WHERE word = :word LIMIT 1")
    suspend fun getFavoriteByWord(word: String): Favorite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)

    @Delete
    suspend fun deleteFavorite(favorite: Favorite)
    
    @Query("DELETE FROM favorites WHERE word = :word")
    suspend fun deleteFavoriteByWord(word: String)

    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()
}
