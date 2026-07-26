package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(searchHistory: SearchHistory)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()

    @Query("DELETE FROM search_history WHERE word = :word")
    suspend fun deleteSearchByWord(word: String)
}
