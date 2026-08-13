package com.example

import android.content.Intent
import android.os.Bundle
import com.example.data.SearchMode
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.util.Locale
import com.example.data.AppDatabase
import com.example.data.DictionaryRepository
import com.example.data.DictionaryResult
import com.example.data.SearchHistory
import com.example.data.SearchHistoryRepository
import com.example.data.TranslationLanguage
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.Favorite
import com.example.data.FavoriteRepository
import kotlinx.coroutines.flow.map
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState


enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("dictionary_prefs", android.content.Context.MODE_PRIVATE)
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

    private val repository = DictionaryRepository()
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

    private val _uiState = MutableStateFlow<DictionaryUiState>(DictionaryUiState.Idle)
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

    private val _isCurrentWordFavorite = MutableStateFlow(false)
    val isCurrentWordFavorite: StateFlow<Boolean> = _isCurrentWordFavorite.asStateFlow()

    fun showBottomSheet(word: String) {
        _isBottomSheetVisible.value = true
        fetchDefinition(word)
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

private var searchJob: kotlinx.coroutines.Job? = null

    private fun fetchDefinition(word: String) {
        searchJob?.cancel()
        _uiState.value = DictionaryUiState.Loading
        searchJob = viewModelScope.launch {
            val currentLang = _targetLanguage.value
            val currentMode = _searchMode.value
            val result = repository.getDefinitionAndSynonyms(word, currentLang, currentMode)
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

sealed class DictionaryUiState {
    object Idle : DictionaryUiState()
    object Loading : DictionaryUiState()
    data class Success(val result: DictionaryResult) : DictionaryUiState()
    data class Error(val message: String) : DictionaryUiState()
}

class MainActivity : ComponentActivity() {
    private var pendingTextToProcess: String? = null

    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingTextToProcess = extractTextFromIntent(intent)
        handlePendingText()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            MyApplicationTheme(darkTheme = darkTheme) {
                com.example.ui.MainScreen(activity = this@MainActivity, viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTextToProcess = extractTextFromIntent(intent)
        handlePendingText()
    }
    
    private fun handlePendingText() {
        if (pendingTextToProcess != null) {
            if (android.provider.Settings.canDrawOverlays(this)) {
                val serviceIntent = Intent(this, com.example.service.FloatingDictionaryService::class.java).apply {
                    putExtra("EXTRA_WORD", pendingTextToProcess)
                }
                startService(serviceIntent)
                finish()
            } else {
                android.widget.Toast.makeText(this, "Permita a sobreposição para usar a janela flutuante", android.widget.Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
                startActivity(intent)
                finish()
            }
        }
    }

    private fun extractTextFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        var text: String? = null
        when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (text == null) {
                    text = intent.getStringExtra(Intent.EXTRA_TEXT)
                }
            }
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    text = intent.getStringExtra(Intent.EXTRA_TEXT)
                }
            }
            "colordict.intent.action.SEARCH" -> {
                text = intent.getStringExtra("EXTRA_QUERY")
            }
            Intent.ACTION_SEARCH -> {
                text = intent.getStringExtra(android.app.SearchManager.QUERY)
            }
        }
        return text
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onNavigateToTest: () -> Unit,
    onEnableFloatingWindow: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ativar Janela") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Dicionário de Leitura",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Escolha uma opção",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onEnableFloatingWindow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.OpenWith, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Ativar Janela Flutuante", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onNavigateToTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Tela de Teste", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun rememberTextToSpeech(targetLanguage: TranslationLanguage = TranslationLanguage.PORTUGUESE): TextToSpeech? {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context, targetLanguage) {
        var textToSpeech: TextToSpeech? = null
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = targetLanguage.locale
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }
    return tts
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DictionaryApp(
    initialTextToProcess: String?,
    viewModel: DictionaryViewModel = viewModel(),
    onFinishActivity: () -> Unit = {},
    onOpenDrawer: () -> Unit = {}
) {
    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isCurrentWordFavorite by viewModel.isCurrentWordFavorite.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    
    val searchHistory by viewModel.searchHistory.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tts = rememberTextToSpeech(targetLanguage)

    LaunchedEffect(initialTextToProcess) {
        if (!initialTextToProcess.isNullOrBlank()) {
            viewModel.showBottomSheet(initialTextToProcess)
        }
    }

    val isLaunchedFromIntent = initialTextToProcess != null

    LaunchedEffect(isBottomSheetVisible) {
        if (!isBottomSheetVisible && isLaunchedFromIntent) {
            onFinishActivity()
        }
    }

    if (!isLaunchedFromIntent) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Teste") },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Dicionário de Leitura",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                var textToSimulate by remember { mutableStateOf("Efêmero") }
                OutlinedTextField(
                    value = textToSimulate,
                    onValueChange = { textToSimulate = it },
                    label = { Text("Palavra para testar") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_input")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.showBottomSheet(textToSimulate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("simulate_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simular Seleção de Texto")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histórico Recente",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchHistory) { historyItem ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.showBottomSheet(historyItem.word) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = historyItem.word,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = historyItem.definition,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize())
    }

    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.hideBottomSheet() },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.surfaceVariant) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                when (val state = uiState) {
                    is DictionaryUiState.Idle, is DictionaryUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is DictionaryUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Erro: ${state.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    is DictionaryUiState.Success -> {
                        DictionaryResultContent(
                            state = state,
                            tts = tts,
                            isFavorite = isCurrentWordFavorite,
                            onToggleFavorite = { viewModel.toggleFavorite(state.result.word, state.result.definition) },
                            onClose = { viewModel.hideBottomSheet() },
                            onBackToSearch = { viewModel.hideBottomSheet() },
                            currentLanguage = targetLanguage,
                            onSelectLanguage = { lang ->
                                viewModel.setTargetLanguage(lang)
                                viewModel.showBottomSheet(state.result.word)
                            },
                            currentSearchMode = searchMode,
                            onSelectSearchMode = { mode ->
                                viewModel.setSearchMode(mode)
                                viewModel.showBottomSheet(state.result.word)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryResultContent(
    state: DictionaryUiState.Success,
    tts: TextToSpeech?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onBackToSearch: (() -> Unit)? = null,
    fontSizeSp: Float = 16f,
    currentLanguage: TranslationLanguage = TranslationLanguage.PORTUGUESE,
    onSelectLanguage: ((TranslationLanguage) -> Unit)? = null,
    currentSearchMode: SearchMode = SearchMode.MEANING,
    onSelectSearchMode: ((SearchMode) -> Unit)? = null
) {
    val titleSize = (fontSizeSp * 1.5f).sp
    val definitionSize = fontSizeSp.sp
    val definitionLineHeight = (fontSizeSp * 1.45f).sp
    val labelSize = (fontSizeSp * 0.8f).sp
    
    // Botão "Voltar para pesquisa" no canto supior esquerdo
    /*if (onBackToSearch != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToSearch,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("top_back_to_search_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar para Pesquisa",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Voltar para Pesquisa",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBackToSearch() }
            )
        }
    }*/

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.result.word,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = titleSize),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = state.result.partOfSpeech.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = labelSize),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        Row {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favoritar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            IconButton(
                onClick = { 
                    tts?.speak(state.result.word, TextToSpeech.QUEUE_FLUSH, null, null)
                },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Ouvir pronúncia",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    if (onSelectLanguage != null || onSelectSearchMode != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onSelectSearchMode != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = currentSearchMode == SearchMode.MEANING,
                        onClick = { if (currentSearchMode != SearchMode.MEANING) onSelectSearchMode(SearchMode.MEANING) },
                        label = { Text("Significado", fontSize = 12.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                    FilterChip(
                        selected = currentSearchMode == SearchMode.TRANSLATION,
                        onClick = { if (currentSearchMode != SearchMode.TRANSLATION) onSelectSearchMode(SearchMode.TRANSLATION) },
                        label = { Text("Tradução", fontSize = 12.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            if (onSelectLanguage != null) {
                var langDropdownExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { langDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("${currentLanguage.flag} ${currentLanguage.displayName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Idioma", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = langDropdownExpanded,
                        onDismissRequest = { langDropdownExpanded = false }
                    ) {
                        TranslationLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text("${lang.flag} ${lang.displayName}", fontSize = 13.sp) },
                                onClick = {
                                    langDropdownExpanded = false
                                    if (lang != currentLanguage) onSelectLanguage(lang)
                                },
                                leadingIcon = if (lang == currentLanguage) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
    Spacer(modifier = Modifier.height(16.dp))
    
    // Definition
    Text(
        text = state.result.definition,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = definitionSize, lineHeight = definitionLineHeight),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    if (state.result.synonyms.isNotEmpty()) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "SINÔNIMOS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.result.synonyms.forEach { synonym ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = synonym,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBackToSearch != null) {
            Button(
                onClick = onBackToSearch,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("back_to_search_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Voltar à Pesquisa", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        } else {
            Button(
                onClick = { /* Could trigger more details */ },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Ver mais detalhes", fontWeight = FontWeight.Bold)
            }
        }
        
        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .height(52.dp)
                .testTag("close_sheet_button"),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Fechar", fontWeight = FontWeight.Medium)
        }
    }
}
