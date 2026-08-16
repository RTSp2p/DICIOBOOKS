package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
