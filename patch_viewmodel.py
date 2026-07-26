import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

replacement = """    private var searchJob: kotlinx.coroutines.Job? = null

    private fun fetchDefinition(word: String) {
        searchJob?.cancel()
        _uiState.value = DictionaryUiState.Loading
        searchJob = viewModelScope.launch {
            val result = repository.getDefinitionAndSynonyms(word)
            result.onSuccess { data ->
                _uiState.value = DictionaryUiState.Success(data)
                _isCurrentWordFavorite.value = favoriteRepository.isFavorite(data.word)
                searchHistoryRepository.insert(
                    SearchHistory(
                        word = data.word,
                        partOfSpeech = data.partOfSpeech,
                        definition = data.definition,
                        synonyms = data.synonyms.joinToString(", ")
                    )
                )
            }.onFailure { error ->
                if (error !is kotlinx.coroutines.CancellationException) {
                    _uiState.value = DictionaryUiState.Error(error.message ?: "Erro desconhecido")
                }
            }
        }
    }"""

content = re.sub(
    r'    private fun fetchDefinition\(word: String\) \{.*?    \}',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
