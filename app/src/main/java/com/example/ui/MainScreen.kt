package com.example.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.DictionaryApp
import com.example.DictionaryViewModel
import com.example.MainActivity
import com.example.MenuScreen
import com.example.data.SearchMode
import com.example.data.TranslationLanguage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(activity: MainActivity, viewModel: DictionaryViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val hasSeenTutorial by viewModel.hasSeenTutorial.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
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
                        icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                        label = { Text("Tutorial de Uso") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.showTutorialAgain()
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
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onShowTutorial = { viewModel.showTutorialAgain() },
                        onOpenFloatingWindowMenu = { navController.navigate("menu") }
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

        if (!hasSeenTutorial) {
            TutorialOverlay(onDismiss = { viewModel.completeTutorial() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onShowTutorial: () -> Unit,
    onOpenFloatingWindowMenu: () -> Unit
) {
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
                    IconButton(onClick = onShowTutorial) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Tutorial")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Dicionário de Leitura",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Consulte definições e traduções de qualquer palavra instantaneamente ao selecionar textos em seus aplicativos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onShowTutorial,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver Tutorial de Seleção")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OpenWith, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Janela Flutuante",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ative a bolha flutuante para manter o dicionário sempre à mão durante a leitura.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onOpenFloatingWindowMenu,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ativar Janela Flutuante")
                    }
                }
            }
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
    val floatingFontSize by viewModel.floatingFontSize.collectAsState()
    val floatingIconOpacity by viewModel.floatingIconOpacity.collectAsState()
    val floatingWindowOpacity by viewModel.floatingWindowOpacity.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val searchMode by viewModel.searchMode.collectAsState()
    val isClipboardListenerEnabled by viewModel.isClipboardListenerEnabled.collectAsState()
    val context = LocalContext.current

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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Modo de Operação", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Escolha como o aplicativo e a janela flutuante devem processar o texto selecionado ou pesquisado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            SearchMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setSearchMode(mode) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = searchMode == mode,
                        onClick = { viewModel.setSearchMode(mode) }
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (searchMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Idioma de Tradução Preferido (Contexto Gemini)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Selecione o idioma de destino no menu suspenso abaixo. Essa escolha é enviada como contexto direto para a API do Gemini AI ajustar as definições e traduções.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            var languageMenuExpanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { languageMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = targetLanguage.flag,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Idioma de Destino",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = targetLanguage.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Menu suspenso de idiomas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                DropdownMenu(
                    expanded = languageMenuExpanded,
                    onDismissRequest = { languageMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    TranslationLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(lang.flag, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (targetLanguage == lang) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                viewModel.setTargetLanguage(lang)
                                languageMenuExpanded = false
                            },
                            leadingIcon = if (targetLanguage == lang) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

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

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Tamanho da Fonte da Janela Flutuante", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Ajuste a escala do texto para melhorar a acessibilidade e a leitura no modo janela flutuante.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "Pequena" to 14f,
                    "Normal" to 16f,
                    "Grande" to 20f,
                    "Extra" to 24f
                )
                presets.forEach { (label, size) ->
                    val isSelected = kotlin.math.abs(floatingFontSize - size) < 0.5f
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFloatingFontSize(size) },
                        label = { Text(label, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Tamanho personalizado: ${floatingFontSize.toInt()} sp",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.setFloatingFontSize(16f) }) {
                    Text("Redefinir (16 sp)")
                }
            }

            Slider(
                value = floatingFontSize,
                onValueChange = { viewModel.setFloatingFontSize(it) },
                valueRange = 12f..28f,
                steps = 7,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = floatingWindowOpacity },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Pré-visualização do Texto na Janela Flutuante",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Resiliência",
                        fontSize = (floatingFontSize * 1.4f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "SUBSTANTIVO FEMININO",
                        fontSize = (floatingFontSize * 0.75f).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Capacidade de se recuperar facilmente ou se adaptar com sucesso às mudanças e dificuldades.",
                        fontSize = floatingFontSize.sp,
                        lineHeight = (floatingFontSize * 1.4f).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Opacidade da Janela Flutuante", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Ajuste a transparência do painel da janela flutuante quando ela estiver aberta sobre outros aplicativos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Opacidade da Janela: ${(floatingWindowOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.setFloatingWindowOpacity(1.0f) }) {
                    Text("Redefinir (100%)")
                }
            }

            Slider(
                value = floatingWindowOpacity,
                onValueChange = { viewModel.setFloatingWindowOpacity(it) },
                valueRange = 0.2f..1.0f,
                steps = 7,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Opacidade do Ícone Flutuante", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Ajuste a transparência do ícone flutuante minimizado (fechado) para reduzir distrações visuais enquanto lê ou navega.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Opacidade do Ícone: ${(floatingIconOpacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.setFloatingIconOpacity(1.0f) }) {
                    Text("Redefinir (100%)")
                }
            }

            Slider(
                value = floatingIconOpacity,
                onValueChange = { viewModel.setFloatingIconOpacity(it) },
                valueRange = 0.2f..1.0f,
                steps = 7,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Pré-visualização do Ícone Flutuante",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = {},
                        shape = CircleShape,
                        modifier = Modifier.graphicsLayer { alpha = floatingIconOpacity }
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Pré-visualização do Ícone")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Leitura Automática da Área de Transferência", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Quando a janela flutuante estiver ativa, ao copiar qualquer texto, o app exibe um mini badge discreto na tela oferecendo significado/tradução instantânea com 1 toque.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setClipboardListenerEnabled(!isClipboardListenerEnabled) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Monitorar Área de Transferência",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isClipboardListenerEnabled) "Ativado (Badge de 1 toque ativo)" else "Desativado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isClipboardListenerEnabled,
                        onCheckedChange = { viewModel.setClipboardListenerEnabled(it) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Captura de Seleção de Texto (Acessibilidade)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "Ao selecionar qualquer palavra em outro aplicativo, a bolha flutuante do dicionário abre automaticamente para exibir a definição.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Accessibility, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ativar Serviço de Acessibilidade")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    if (!android.provider.Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        android.widget.Toast.makeText(context, "Permissão de sobreposição já concedida!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Layers, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Permissão de Sobreposição na Tela")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("Ajuda & Tutorial", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.showTutorialAgain() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Revisar Tutorial de Seleção de Texto")
            }
        }
    }
}

data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun TutorialOverlay(onDismiss: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    val steps = listOf(
        TutorialStep(
            title = "1. Seleção de Texto",
            description = "Ao ler qualquer texto em PDF, navegador ou livro digital, selecione a palavra ou expressão que deseja consultar.",
            icon = Icons.Default.TouchApp
        ),
        TutorialStep(
            title = "2. Menu e Janela Flutuante",
            description = "No menu de contexto do sistema ou ativando a Bolha Flutuante, você pode visualizar instantaneamente a definição e pronúncia da palavra.",
            icon = Icons.Default.Search
        ),
        TutorialStep(
            title = "3. Bolha Inteligente",
            description = "Mova o ícone para onde quiser. Ao soltá-la próximo às bordas ou após 60s sem interação, ela se recolhe discretamente na tela.",
            icon = Icons.Default.OpenWith
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Passo ${currentStep + 1} de ${steps.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Pular", color = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = steps[currentStep].icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = steps[currentStep].title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = steps[currentStep].description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentStep) 10.dp else 8.dp)
                                .background(
                                    color = if (index == currentStep)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Anterior")
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < steps.size - 1) {
                                currentStep++
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (currentStep == steps.size - 1) "Entendido" else "Próximo")
                    }
                }
            }
        }
    }
}
