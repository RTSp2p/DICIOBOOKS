package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.DictionaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: DictionaryViewModel, onOpenDrawer: () -> Unit) {
    val history by viewModel.searchHistory.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Limpar histórico?") },
            text = { Text("Tem certeza que deseja apagar todo o histórico de pesquisas?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSearchHistory()
                    showClearConfirmDialog = false
                }) { Text("Sim") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Não") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirmDialog = true }) {
                            Text("Limpar tudo")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Pesquisar histórico...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                singleLine = true
            )

            val filteredHistory = if (searchQuery.isBlank()) history else history.filter {
                it.word.contains(searchQuery, ignoreCase = true) || it.definition.contains(searchQuery, ignoreCase = true)
            }

            if (filteredHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Nenhum histórico encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredHistory) { hist ->
                        var showItemDeleteDialog by remember { mutableStateOf(false) }

                        if (showItemDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showItemDeleteDialog = false },
                                title = { Text("Excluir do histórico?") },
                                text = { Text("Remover '${hist.word}' do histórico?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteSearchHistoryItem(hist.word)
                                        showItemDeleteDialog = false
                                    }) { Text("Excluir") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showItemDeleteDialog = false }) { Text("Cancelar") }
                                }
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.showBottomSheet(hist.word) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hist.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(hist.definition, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                }
                                IconButton(onClick = { showItemDeleteDialog = true }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remover")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
