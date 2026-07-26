package com.example.data

import kotlinx.coroutines.flow.Flow

class SearchHistoryRepository(private val searchHistoryDao: SearchHistoryDao) {
    val recentSearches: Flow<List<SearchHistory>> = searchHistoryDao.getRecentSearches()

    suspend fun insert(searchHistory: SearchHistory) {
        searchHistoryDao.insertSearch(searchHistory)
    }

    suspend fun clearHistory() {
        searchHistoryDao.clearHistory()
    }

    suspend fun deleteByWord(word: String) {
        searchHistoryDao.deleteSearchByWord(word)
    }
}
