sed -i '/clearHistory() {/a \
        searchHistoryDao.clearHistory()\
    }\
\
    suspend fun deleteByWord(word: String) {\
        searchHistoryDao.deleteSearchByWord(word)' app/src/main/java/com/example/data/SearchHistoryRepository.kt
sed -i '/clearHistory() {/{N;d;}' app/src/main/java/com/example/data/SearchHistoryRepository.kt
