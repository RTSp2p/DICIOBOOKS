package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedDefinitionDao {
    @Query("SELECT * FROM dictionary_cache WHERE LOWER(word) = LOWER(:word) AND targetLanguage = :targetLanguage AND searchMode = :searchMode LIMIT 1")
    suspend fun getCached(word: String, targetLanguage: String, searchMode: String): CachedDefinition?

    @Query("SELECT * FROM dictionary_cache WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun getAnyCached(word: String): CachedDefinition?

    @Query("SELECT COUNT(*) FROM dictionary_cache")
    fun getCacheCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cached: CachedDefinition)

    @Query("DELETE FROM dictionary_cache WHERE LOWER(word) = LOWER(:word)")
    suspend fun deleteByWord(word: String)

    @Query("DELETE FROM dictionary_cache")
    suspend fun clearCache()
}
