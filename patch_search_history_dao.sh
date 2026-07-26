sed -i '/clearHistory()/a \
\
    @Query("DELETE FROM search_history WHERE word = :word")\
    suspend fun deleteSearchByWord(word: String)' app/src/main/java/com/example/data/SearchHistoryDao.kt
