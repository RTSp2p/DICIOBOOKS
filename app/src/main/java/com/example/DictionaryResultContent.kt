package com.example

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SearchMode
import com.example.data.TranslationLanguage

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
                onClick = { },
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
