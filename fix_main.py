import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

start = content.find("private var searchJob: kotlinx.coroutines.Job? = null")
end = content.find("sealed class DictionaryUiState {")

good_block = """private var searchJob: kotlinx.coroutines.Job? = null

    private fun fetchDefinition(word: String) {
        searchJob?.cancel()
        _uiState.value = DictionaryUiState.Loading
        searchJob = viewModelScope.launch {
            val cached = searchHistoryRepository.getSearchByWord(word.trim())
            if (cached != null) {
                val data = DictionaryResult(
                    word = cached.word,
                    partOfSpeech = cached.partOfSpeech,
                    definition = cached.definition,
                    synonyms = if (cached.synonyms.isNotEmpty()) cached.synonyms.split(", ") else emptyList()
                )
                _uiState.value = DictionaryUiState.Success(data)
                _isCurrentWordFavorite.value = favoriteRepository.isFavorite(data.word)
                // Atualiza o timestamp (re-insere) para voltar ao topo do histórico
                searchHistoryRepository.insert(cached.copy(timestamp = System.currentTimeMillis()))
                return@launch
            }

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
    }
}

"""

new_content = content[:start] + good_block + content[end:]
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(new_content)
