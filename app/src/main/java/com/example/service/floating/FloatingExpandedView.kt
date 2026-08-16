package com.example.service.floating

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.DictionaryResultContent
import com.example.DictionaryUiState
import com.example.ThemeMode
import com.example.data.SearchMode
import com.example.data.TranslationLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FloatingExpandedView(
    isVisible: Boolean,
    uiState: DictionaryUiState,
    themeMode: ThemeMode,
    searchMode: SearchMode,
    targetLanguage: TranslationLanguage,
    floatingWindowOpacity: Float,
    floatingFontSize: Float,
    isCurrentWordFavorite: Boolean,
    detectedCopiedText: String?,
    focusRequester: FocusRequester,
    tts: TextToSpeech?,
    screenWidthPx: Float,
    onDismissRequest: () -> Unit,
    onMinimize: () -> Unit,
    onThemeToggle: (ThemeMode) -> Unit,
    onSearchModeChange: (SearchMode) -> Unit,
    onTargetLanguageChange: (TranslationLanguage) -> Unit,
    onSearchWord: (String) -> Unit,
    onResetToSearch: () -> Unit,
    onToggleFavorite: (String, String) -> Unit,
    onDismissCopiedText: () -> Unit,
    onEnsureFocusable: () -> Unit,
    onReleaseFocusable: () -> Unit,
    onCheckClipboard: () -> Unit,
    onWindowMove: (dx: Float, dy: Float) -> Unit,
    onWindowResizeStart: () -> Unit,
    onWindowResize: (dx: Float, dy: Float, density: Float) -> Unit
) {
    val scope = rememberCoroutineScope()
    val dragOffsetX = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 150.dp.toPx() }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .graphicsLayer {
                    translationX = dragOffsetX.value
                    val absX = kotlin.math.abs(dragOffsetX.value)
                    val dragAlpha = (1f - (absX / (dismissThresholdPx * 2f))).coerceIn(0.15f, 1f)
                    alpha = dragAlpha * floatingWindowOpacity
                },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 16.dp
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val scale = (maxWidth / 400.dp).coerceIn(0.5f, 1f)
                val currentDensity = LocalDensity.current
                val newDensity = Density(currentDensity.density * scale, currentDensity.fontScale * scale)

                CompositionLocalProvider(LocalDensity provides newDensity) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .imePadding()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Drag handle with horizontal swipe to dismiss gesture and vertical window move
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                val currentOffsetX = dragOffsetX.value
                                                if (kotlin.math.abs(currentOffsetX) > dismissThresholdPx) {
                                                    scope.launch {
                                                        val targetX = if (currentOffsetX > 0) currentOffsetX + screenWidthPx else currentOffsetX - screenWidthPx
                                                        launch { dragOffsetX.animateTo(targetX, tween(200)) }
                                                        delay(220)
                                                        onDismissRequest()
                                                    }
                                                } else {
                                                    scope.launch {
                                                        dragOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                    }
                                                }
                                            },
                                            onDragCancel = {
                                                scope.launch {
                                                    dragOffsetX.animateTo(0f)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                onWindowMove(dragAmount.x, dragAmount.y)
                                                scope.launch {
                                                    dragOffsetX.snapTo(dragOffsetX.value + dragAmount.x)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(5.dp)
                                            .background(
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                RoundedCornerShape(50)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "◄ Deslize para fechar ►",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            when (val state = uiState) {
                                is DictionaryUiState.Idle -> {
                                    FloatingSearchIdleContent(
                                        searchMode = searchMode,
                                        targetLanguage = targetLanguage,
                                        detectedCopiedText = detectedCopiedText,
                                        focusRequester = focusRequester,
                                        onSearchModeChange = onSearchModeChange,
                                        onTargetLanguageChange = onTargetLanguageChange,
                                        onSearch = onSearchWord,
                                        onClose = {
                                            scope.launch {
                                                delay(100)
                                                onDismissRequest()
                                            }
                                        },
                                        onDismissCopiedText = onDismissCopiedText,
                                        onEnsureFocusable = onEnsureFocusable,
                                        onReleaseFocusable = onReleaseFocusable,
                                        onCheckClipboard = onCheckClipboard
                                    )
                                }
                                is DictionaryUiState.Loading -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ProcessingIndicator()
                                    }
                                }
                                is DictionaryUiState.Error -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Erro: ${state.message}",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                        Button(onClick = onResetToSearch) {
                                            Text("Fechar")
                                        }
                                    }
                                }
                                is DictionaryUiState.Success -> {
                                    DictionaryResultContent(
                                        state = state,
                                        tts = tts,
                                        isFavorite = isCurrentWordFavorite,
                                        onToggleFavorite = { onToggleFavorite(state.result.word, state.result.definition) },
                                        onClose = onDismissRequest,
                                        onBackToSearch = onResetToSearch,
                                        fontSizeSp = floatingFontSize,
                                        currentLanguage = targetLanguage,
                                        onSelectLanguage = { lang ->
                                            onTargetLanguageChange(lang)
                                            onSearchWord(state.result.word)
                                        },
                                        currentSearchMode = searchMode,
                                        onSelectSearchMode = { mode ->
                                            onSearchModeChange(mode)
                                            onSearchWord(state.result.word)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Theme toggle button
                IconButton(
                    onClick = {
                        val nextTheme = when (themeMode) {
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM
                        }
                        onThemeToggle(nextTheme)
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 12.dp, start = 50.dp)
                        .size(36.dp)
                ) {
                    val (icon, description) = when (themeMode) {
                        ThemeMode.SYSTEM -> Pair(Icons.Default.BrightnessAuto, "Tema: Sistema")
                        ThemeMode.LIGHT -> Pair(Icons.Default.LightMode, "Tema: Claro")
                        ThemeMode.DARK -> Pair(Icons.Default.DarkMode, "Tema: Escuro")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Resize handle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(32.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onWindowResizeStart() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onWindowResize(dragAmount.x, dragAmount.y, currentDensity.density)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenWith,
                        contentDescription = "Redimensionar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Minimize handle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .size(32.dp)
                        .clickable { onMinimize() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloseFullscreen,
                        contentDescription = "Minimizar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
