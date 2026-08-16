package com.example.service.floating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SearchMode
import com.example.data.TranslationLanguage

@Composable
fun FloatingSearchIdleContent(
    searchMode: SearchMode,
    targetLanguage: TranslationLanguage,
    detectedCopiedText: String?,
    focusRequester: FocusRequester,
    onSearchModeChange: (SearchMode) -> Unit,
    onTargetLanguageChange: (TranslationLanguage) -> Unit,
    onSearch: (String) -> Unit,
    onClose: () -> Unit,
    onDismissCopiedText: () -> Unit,
    onEnsureFocusable: () -> Unit,
    onReleaseFocusable: () -> Unit,
    onCheckClipboard: () -> Unit
) {
    var textToSearch by remember { mutableStateOf("") }
    var languageMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Toggle Selector
        Text(
            text = "Modo de Consulta",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = searchMode == SearchMode.MEANING,
                onClick = { onSearchModeChange(SearchMode.MEANING) },
                label = { Text("Significado") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = searchMode == SearchMode.TRANSLATION,
                onClick = { onSearchModeChange(SearchMode.TRANSLATION) },
                label = { Text("Tradução") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Target Language Dropdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Idioma de Destino:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                OutlinedButton(
                    onClick = { languageMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${targetLanguage.flag} ${targetLanguage.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar idioma")
                }

                DropdownMenu(
                    expanded = languageMenuExpanded,
                    onDismissRequest = { languageMenuExpanded = false }
                ) {
                    TranslationLanguage.entries.forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(lang.flag, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (targetLanguage == lang) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                onTargetLanguageChange(lang)
                                languageMenuExpanded = false
                            },
                            leadingIcon = if (targetLanguage == lang) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // OutlinedTextField com recuperação instantânea de foco ao toque
        OutlinedTextField(
            value = textToSearch,
            onValueChange = { textToSearch = it },
            label = {
                Text(
                    if (searchMode == SearchMode.TRANSLATION) "Digite o texto para traduzir"
                    else "Digite a palavra para pesquisar"
                )
            },
            placeholder = {
                Text(
                    if (searchMode == SearchMode.TRANSLATION) "Ex: Hello world"
                    else "Ex: Efêmero"
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (searchMode == SearchMode.TRANSLATION) Icons.Default.Translate else Icons.Default.Search,
                    contentDescription = null
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        onCheckClipboard()
                        onEnsureFocusable()
                        focusRequester.requestFocus()
                    }
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onCheckClipboard()
                        onEnsureFocusable()
                    } else {
                        onReleaseFocusable()
                    }
                }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = !detectedCopiedText.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val clipText = detectedCopiedText
            if (!clipText.isNullOrBlank()) {
                val preview = if (clipText.length > 28) clipText.take(28) + "..." else clipText
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Texto Copiado Detectado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "\"$preview\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    onDismissCopiedText()
                                    onSearch(clipText)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Consultar", fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = onDismissCopiedText,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Fechar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("Fechar")
            }
            Button(
                onClick = {
                    if (textToSearch.isNotBlank()) {
                        onSearch(textToSearch)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (searchMode == SearchMode.TRANSLATION) "Traduzir" else "Buscar")
            }
        }
    }
}
