import re

with open('app/src/main/java/com/example/data/SearchHistoryRepository.kt', 'r') as f:
    content = f.read()

replacement = """    val recentSearches: Flow<List<SearchHistory>> = searchHistoryDao.getRecentSearches()

    suspend fun getSearchByWord(word: String): SearchHistory? {
        return searchHistoryDao.getSearchByWord(word)
    }"""

content = content.replace('    val recentSearches: Flow<List<SearchHistory>> = searchHistoryDao.getRecentSearches()', replacement)

with open('app/src/main/java/com/example/data/SearchHistoryRepository.kt', 'w') as f:
    f.write(content)
