package com.example.service.floating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Componente visual do ícone flutuante (bolha).
 * Trata o badge de texto copiado e os gestos de:
 * 1. Toque simples rápido -> expande a janela do dicionário.
 * 2. Pressão contínua (> 2 segundos) em bordas acolhidas -> desacolhe e fica 100% visível para arrasto.
 * 3. Arrasto manual contínuo -> reposicionamento livre ou acolhimento sob pressão nas bordas.
 */
@Composable
fun FloatingBubbleView(
    detectedCopiedText: String?,
    floatingIconOpacity: Float,
    onCopiedTextClick: (String) -> Unit,
    onDismissCopiedBadge: () -> Unit,
    onExpandWindow: () -> Unit,
    onCheckClipboard: () -> Unit,
    onLongPressUndock: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onInteraction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.wrapContentSize()
    ) {
        // Badge de texto recentemente copiado
        AnimatedVisibility(
            visible = !detectedCopiedText.isNullOrBlank(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }) + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 }) + scaleOut(targetScale = 0.85f)
        ) {
            val clipText = detectedCopiedText
            if (!clipText.isNullOrBlank()) {
                val previewText = if (clipText.length > 22) clipText.take(22) + "..." else clipText
                Surface(
                    onClick = { onCopiedTextClick(clipText) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .widthIn(max = 280.dp)
                        .testTag("floating_copied_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Texto copiado",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Pesquisar agora?",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "\"$previewText\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Ir",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDismissCopiedBadge,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar badge",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Ícone Flutuante com detecção precisa de Toque Simples vs Pressão Longa (>2s) vs Arrasto
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 8.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(4.dp)
                .size(56.dp)
                .graphicsLayer { alpha = floatingIconOpacity }
                .testTag("floating_bubble_icon")
                .pointerInput(Unit) {
                    coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Registra interação no início do toque para renovar o temporizador de 60s
                            onInteraction()
                            onCheckClipboard()

                            var totalDx = 0f
                            var totalDy = 0f
                            var isDrag = false
                            var hasUndocked = false
                            val touchSlop = viewConfiguration.touchSlop

                            // Temporizador de 2 segundos para desacolher o ícone se pressionado continuamente
                            val longPressJob = launch {
                                delay(2000L)
                                if (!isDrag) {
                                    hasUndocked = true
                                    onLongPressUndock()
                                    onInteraction()
                                }
                            }

                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val pointerChange = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (pointerChange.pressed) {
                                        val posChange = pointerChange.positionChange()
                                        totalDx += posChange.x
                                        totalDy += posChange.y

                                        // Identifica se ultrapassou a folga de toque (touch slop)
                                        if (!isDrag && kotlin.math.hypot(totalDx, totalDy) > touchSlop) {
                                            isDrag = true
                                            longPressJob.cancel()
                                        }

                                        if (isDrag) {
                                            pointerChange.consume()
                                            onDragDelta(posChange.x, posChange.y)
                                            onInteraction()
                                        }
                                    } else {
                                        // Dedo levantado (UP)
                                        longPressJob.cancel()
                                        if (isDrag) {
                                            // Soltou após arrasto: acolhe na borda se pressionado contra ela, ou fica livre
                                            onDragEnd()
                                            onInteraction()
                                        } else if (!hasUndocked) {
                                            // Toque simples rápido sem arrasto: expande a janela imediatamente
                                            onExpandWindow()
                                            onCheckClipboard()
                                            onInteraction()
                                        } else {
                                            // Ficou pressionado por > 2s e soltou sem arrastar: mantém livre no local
                                            onInteraction()
                                        }
                                        break
                                    }
                                } while (event.changes.any { it.pressed })
                            } finally {
                                longPressJob.cancel()
                            }
                        }
                    }
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Maximizar dicionário",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
