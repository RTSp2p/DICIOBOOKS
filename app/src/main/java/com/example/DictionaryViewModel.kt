package com.example

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DictionaryRepository
import com.example.data.Favorite
import com.example.data.FavoriteRepository
import com.example.data.SearchHistory
import com.example.data.SearchHistoryRepository
import com.example.data.SearchMode
import com.example.data.TranslationLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "theme_mode" -> {
                _themeMode.value = ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
            }
            "has_seen_tutorial" -> {
                _hasSeenTutorial.value = prefs.getBoolean("has_seen_tutorial", false)
            }
            "floating_font_size" -> {
                _floatingFontSize.value = prefs.getFloat("floating_font_size", 16f)
            }
            "floating_icon_opacity" -> {
                _floatingIconOpacity.value = prefs.getFloat("floating_icon_opacity", 1.0f)
            }
            "floating_window_opacity" -> {
                _floatingWindowOpacity.value = prefs.getFloat("floating_window_opacity", 0.95f)
            }
            "target_language" -> {
                _targetLanguage.value = TranslationLanguage.fromCode(prefs.getString("target_language", TranslationLanguage.PORTUGUESE.code))
            }
            "search_mode" -> {
                _searchMode.value = SearchMode.fromName(prefs.getString("search_mode", SearchMode.MEANING.name))
            }
            "is_clipboard_listener_enabled" -> {
                _isClipboardListenerEnabled.value = prefs.getBoolean("is_clipboard_listener_enabled", true)
            }
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    private val _hasSeenTutorial = MutableStateFlow(prefs.getBoolean("has_seen_tutorial", false))
    val hasSeenTutorial: StateFlow<Boolean> = _hasSeenTutorial.asStateFlow()

    fun completeTutorial() {
        prefs.edit().putBoolean("has_seen_tutorial", true).apply()
        _hasSeenTutorial.value = true
    }

    fun showTutorialAgain() {
        _hasSeenTutorial.value = false
    }

    private val _floatingFontSize = MutableStateFlow(prefs.getFloat("floating_font_size", 16f))
    val floatingFontSize: StateFlow<Float> = _floatingFontSize.asStateFlow()

    fun setFloatingFontSize(sizeSp: Float) {
        prefs.edit().putFloat("floating_font_size", sizeSp).apply()
        _floatingFontSize.value = sizeSp
    }

    private val _floatingIconOpacity = MutableStateFlow(prefs.getFloat("floating_icon_opacity", 1.0f))
    val floatingIconOpacity: StateFlow<Float> = _floatingIconOpacity.asStateFlow()

    fun setFloatingIconOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.2f, 1.0f)
        prefs.edit().putFloat("floating_icon_opacity", clamped).apply()
        _floatingIconOpacity.value = clamped
    }

    private val _floatingWindowOpacity = MutableStateFlow(prefs.getFloat("floating_window_opacity", 0.95f))
    val floatingWindowOpacity: StateFlow<Float> = _floatingWindowOpacity.asStateFlow()

    fun setFloatingWindowOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.2f, 1.0f)
        prefs.edit().putFloat("floating_window_opacity", clamped).apply()
        _floatingWindowOpacity.value = clamped
    }

    private val _targetLanguage = MutableStateFlow(
        TranslationLanguage.fromCode(prefs.getString("target_language", TranslationLanguage.PORTUGUESE.code))
    )
    val targetLanguage: StateFlow<TranslationLanguage> = _targetLanguage.asStateFlow()

    fun setTargetLanguage(language: TranslationLanguage) {
        prefs.edit().putString("target_language", language.code).apply()
        _targetLanguage.value = language
    }

    private val _searchMode = MutableStateFlow(
        SearchMode.fromName(prefs.getString("search_mode", SearchMode.MEANING.name))
    )
    val searchMode: StateFlow<SearchMode> = _searchMode.asStateFlow()

    fun setSearchMode(mode: SearchMode) {
        prefs.edit().putString("search_mode", mode.name).apply()
        _searchMode.value = mode
    }

    private val _isClipboardListenerEnabled = MutableStateFlow(
        prefs.getBoolean("is_clipboard_listener_enabled", true)
    )
    val isClipboardListenerEnabled: StateFlow<Boolean> = _isClipboardListenerEnabled.asStateFlow()

    fun setClipboardListenerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_clipboard_listener_enabled", enabled).apply()
        _isClipboardListenerEnabled.value = enabled
    }

    private val _geminiCustomApiKey = MutableStateFlow(
        prefs.getString("gemini_custom_api_key", "") ?: ""
    )
    val geminiCustomApiKey: StateFlow<String> = _geminiCustomApiKey.asStateFlow()

    fun setGeminiCustomApiKey(key: String) {
        prefs.edit().putString("gemini_custom_api_key", key.trim()).apply()
        _geminiCustomApiKey.value = key.trim()
    }

    private val _groqCustomApiKey = MutableStateFlow(
        prefs.getString("groq_custom_api_key", "") ?: ""
    )
    val groqCustomApiKey: StateFlow<String> = _groqCustomApiKey.asStateFlow()

    fun setGroqCustomApiKey(key: String) {
        prefs.edit().putString("groq_custom_api_key", key.trim()).apply()
        _groqCustomApiKey.value = key.trim()
    }

    private val repository = DictionaryRepository(application)

    fun isGeminiConfigured(): Boolean = repository.getGeminiApiKey().isNotBlank()
    fun isGroqConfigured(): Boolean = repository.getGroqApiKey().isNotBlank()

    private val db = AppDatabase.getDatabase(application)
    private val searchHistoryRepository = SearchHistoryRepository(db.searchHistoryDao())
    private val favoriteRepository = FavoriteRepository(db.favoriteDao())

    val searchHistory: StateFlow<List<SearchHistory>> = searchHistoryRepository.recentSearches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favorites: StateFlow<List<Favorite>> = favoriteRepository.allFavorites
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cacheCount: StateFlow<Int> = db.cachedDefinitionDao().getCacheCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
        }
    }

    private val _uiState = MutableStateFlow<DictionaryUiState>(DictionaryUiState.Idle)
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    private val _lastSearchedWord = MutableStateFlow("")
    val lastSearchedWord: StateFlow<String> = _lastSearchedWord.asStateFlow()

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

    private val _isCurrentWordFavorite = MutableStateFlow(false)
    val isCurrentWordFavorite: StateFlow<Boolean> = _isCurrentWordFavorite.asStateFlow()

    fun showBottomSheet(word: String) {
        _isBottomSheetVisible.value = true
        fetchDefinition(word)
    }

    fun retryLastSearch() {
        val word = _lastSearchedWord.value
        if (word.isNotBlank()) {
            fetchDefinition(word)
        }
    }

    fun hideBottomSheet() {
        _isBottomSheetVisible.value = false
        _uiState.value = DictionaryUiState.Idle
    }

    fun resetToSearch() {
        _uiState.value = DictionaryUiState.Idle
    }

    fun toggleFavorite(word: String, definition: String) {
        viewModelScope.launch {
            if (_isCurrentWordFavorite.value) {
                favoriteRepository.deleteByWord(word)
                _isCurrentWordFavorite.value = false
            } else {
                favoriteRepository.insert(Favorite(word = word, definition = definition))
                _isCurrentWordFavorite.value = true
            }
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            favoriteRepository.clearAll()
        }
    }

    fun deleteFavorite(word: String) {
        viewModelScope.launch {
            favoriteRepository.deleteByWord(word)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.clearHistory()
        }
    }

    fun deleteSearchHistoryItem(word: String) {
        viewModelScope.launch {
            searchHistoryRepository.deleteByWord(word)
        }
    }

    private var searchJob: Job? = null

    private fun fetchDefinition(word: String) {
        val trimmed = word.trim()
        if (trimmed.isBlank()) return
        _lastSearchedWord.value = trimmed
        searchJob?.cancel()
        _uiState.value = DictionaryUiState.Loading
        searchJob = viewModelScope.launch {
            val currentLang = _targetLanguage.value
            val currentMode = _searchMode.value
            val result = repository.getDefinitionAndSynonyms(trimmed, currentLang, currentMode)
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
                if (error !is CancellationException) {
                    _uiState.value = DictionaryUiState.Error(error.message ?: "Erro desconhecido")
                }
            }
        }
    }
}
