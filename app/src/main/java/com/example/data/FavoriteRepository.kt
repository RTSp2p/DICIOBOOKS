package com.example.data

import kotlinx.coroutines.flow.Flow

class FavoriteRepository(private val favoriteDao: FavoriteDao) {
    val allFavorites: Flow<List<Favorite>> = favoriteDao.getAllFavorites()

    suspend fun isFavorite(word: String): Boolean {
        return favoriteDao.getFavoriteByWord(word) != null
    }

    suspend fun insert(favorite: Favorite) {
        favoriteDao.insertFavorite(favorite)
    }

    suspend fun deleteByWord(word: String) {
        favoriteDao.deleteFavoriteByWord(word)
    }
    
    suspend fun clearAll() {
        favoriteDao.clearAllFavorites()
    }
}
