package com.example.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.DictionaryApp
import com.example.DictionaryViewModel
import com.example.MainActivity
import com.example.MenuScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(activity: MainActivity, viewModel: DictionaryViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Dicionário", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge)
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Tela Inicial") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("home")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Build, contentDescription = null) },
                    label = { Text("Teste") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("test")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.OpenWith, contentDescription = null) },
                    label = { Text("Ativar Janela") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("menu")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Histórico") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("history")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text("Favoritos") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("favorites")
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Configurações") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("settings")
                    }
                )
            }
        }
    ) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("menu") {
                MenuScreen(
                    onNavigateToTest = { navController.navigate("test") },
                    onEnableFloatingWindow = {
                        if (android.provider.Settings.canDrawOverlays(activity)) {
                            val serviceIntent = Intent(activity, com.example.service.FloatingDictionaryService::class.java)
                            activity.startService(serviceIntent)
                        } else {
                            android.widget.Toast.makeText(activity, "Permita a sobreposição para usar a janela flutuante", android.widget.Toast.LENGTH_LONG).show()
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${activity.packageName}"))
                            activity.startActivity(intent)
                        }
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("test") {
                DictionaryApp(
                    initialTextToProcess = null,
                    onFinishActivity = { navController.popBackStack() },
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("history") {
                HistoryScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("favorites") {
                FavoritesScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Início") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil do Usuário")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Olá! Bem-vindo(a) ao Dicionário de Leitura.", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: DictionaryViewModel, onOpenDrawer: () -> Unit) {
    val favorites by viewModel.favorites.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Pesquisar favoritos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                singleLine = true
            )

            val filteredFavorites = if (searchQuery.isBlank()) favorites else favorites.filter { 
                it.word.contains(searchQuery, ignoreCase = true) || it.definition.contains(searchQuery, ignoreCase = true)
            }

            if (filteredFavorites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Nenhum favorito encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFavorites) { fav ->
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
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: DictionaryViewModel, onOpenDrawer: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Text("Aparência", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(com.example.ThemeMode.SYSTEM) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = themeMode == com.example.ThemeMode.SYSTEM, onClick = { viewModel.setThemeMode(com.example.ThemeMode.SYSTEM) })
                Spacer(Modifier.width(16.dp))
                Text("Padrão do Sistema", style = MaterialTheme.typography.bodyLarge)
            }
            
            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(com.example.ThemeMode.LIGHT) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = themeMode == com.example.ThemeMode.LIGHT, onClick = { viewModel.setThemeMode(com.example.ThemeMode.LIGHT) })
                Spacer(Modifier.width(16.dp))
                Text("Claro", style = MaterialTheme.typography.bodyLarge)
            }
            
            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(com.example.ThemeMode.DARK) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = themeMode == com.example.ThemeMode.DARK, onClick = { viewModel.setThemeMode(com.example.ThemeMode.DARK) })
                Spacer(Modifier.width(16.dp))
                Text("Escuro", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
