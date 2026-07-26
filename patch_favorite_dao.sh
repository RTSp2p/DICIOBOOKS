sed -i '/deleteFavoriteByWord/a \
\
    @Query("DELETE FROM favorites")\
    suspend fun clearAllFavorites()' app/src/main/java/com/example/data/FavoriteDao.kt
