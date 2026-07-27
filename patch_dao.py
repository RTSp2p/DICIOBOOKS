import re

with open('app/src/main/java/com/example/data/SearchHistoryDao.kt', 'r') as f:
    content = f.read()

replacement = """    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentSearches(): Flow<List<SearchHistory>>

    @Query("SELECT * FROM search_history WHERE word = :word COLLATE NOCASE LIMIT 1")
    suspend fun getSearchByWord(word: String): SearchHistory?"""

content = content.replace('    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")\n    fun getRecentSearches(): Flow<List<SearchHistory>>', replacement)

with open('app/src/main/java/com/example/data/SearchHistoryDao.kt', 'w') as f:
    f.write(content)
