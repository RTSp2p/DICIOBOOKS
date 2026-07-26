@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: DictionaryViewModel, onOpenDrawer: () -> Unit) {
    val favorites by viewModel.favorites.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Limpar tudo?") },
            text = { Text("Tem certeza que deseja apagar todos os favoritos?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllFavorites()
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
                title = { Text("Favoritos") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    if (favorites.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirmDialog = true }) {
                            Text("Limpar tudo")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhum favorito salvo ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favorites) { fav ->
                    var showItemDeleteDialog by remember { mutableStateOf(false) }
                    
                    if (showItemDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showItemDeleteDialog = false },
                            title = { Text("Excluir favorito?") },
                            text = { Text("Remover '${fav.word}' dos favoritos?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteFavorite(fav.word)
                                    showItemDeleteDialog = false
                                }) { Text("Excluir") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showItemDeleteDialog = false }) { Text("Cancelar") }
                            }
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.showBottomSheet(fav.word) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fav.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(fav.definition, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: DictionaryViewModel, onOpenDrawer: () -> Unit) {
    val history by viewModel.searchHistory.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

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
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Nenhum histórico salvo ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { hist ->
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
