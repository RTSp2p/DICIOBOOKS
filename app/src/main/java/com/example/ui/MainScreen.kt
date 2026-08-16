package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.DictionaryApp
import com.example.DictionaryViewModel
import com.example.MainActivity
import com.example.MenuScreen
import com.example.service.FloatingDictionaryService
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TutorialOverlay
import kotlinx.coroutines.launch

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
                    Text(
                        text = "Dicionário",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
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
                            if (Settings.canDrawOverlays(activity)) {
                                val serviceIntent = Intent(activity, FloatingDictionaryService::class.java)
                                activity.startService(serviceIntent)
                            } else {
                                Toast.makeText(
                                    activity,
                                    "Permita a sobreposição para usar a janela flutuante",
                                    Toast.LENGTH_LONG
                                ).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${activity.packageName}")
                                )
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
