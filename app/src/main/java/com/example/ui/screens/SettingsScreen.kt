package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DictionaryViewModel
import com.example.ThemeMode
import com.example.data.SearchMode
import com.example.data.TranslationLanguage

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

            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(ThemeMode.SYSTEM) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = themeMode == ThemeMode.SYSTEM, onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) })
                Spacer(Modifier.width(16.dp))
                Text("Padrão do Sistema", style = MaterialTheme.typography.bodyLarge)
            }

            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(ThemeMode.LIGHT) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = themeMode == ThemeMode.LIGHT, onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) })
                Spacer(Modifier.width(16.dp))
                Text("Claro", style = MaterialTheme.typography.bodyLarge)
            }

            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(ThemeMode.DARK) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = themeMode == ThemeMode.DARK, onClick = { viewModel.setThemeMode(ThemeMode.DARK) })
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
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
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
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Permissão de sobreposição já concedida!", Toast.LENGTH_SHORT).show()
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
