sed -i '/deleteByWord(word: String) {/a \
        favoriteDao.deleteFavoriteByWord(word)\
    }\
\
    suspend fun clearAll() {\
        favoriteDao.clearAllFavorites()' app/src/main/java/com/example/data/FavoriteRepository.kt
sed -i '/deleteByWord(word: String) {/{N;d;}' app/src/main/java/com/example/data/FavoriteRepository.kt
