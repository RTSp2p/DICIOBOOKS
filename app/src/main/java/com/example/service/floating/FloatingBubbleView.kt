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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FloatingBubbleView(
    detectedCopiedText: String?,
    floatingIconOpacity: Float,
    onCopiedTextClick: (String) -> Unit,
    onDismissCopiedBadge: () -> Unit,
    onExpandWindow: () -> Unit,
    onCheckClipboard: () -> Unit,
    onDragDelta: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
    onInteraction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.wrapContentSize()
    ) {
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
                                    imageVector = Icons.Default.ArrowForward,
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

        FloatingActionButton(
            onClick = {},
            shape = CircleShape,
            modifier = Modifier
                .graphicsLayer { alpha = floatingIconOpacity }
                .padding(4.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onCheckClipboard()
                        var totalDx = 0f
                        var totalDy = 0f
                        var isDrag = false
                        val touchSlop = viewConfiguration.touchSlop

                        onInteraction()

                        do {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.find { it.id == down.id } ?: break
                            if (pointerChange.pressed) {
                                val posChange = pointerChange.positionChange()
                                totalDx += posChange.x
                                totalDy += posChange.y

                                if (!isDrag && kotlin.math.hypot(totalDx, totalDy) > touchSlop) {
                                    isDrag = true
                                }

                                if (isDrag) {
                                    pointerChange.consume()
                                    onDragDelta(posChange.x, posChange.y)
                                }
                            } else {
                                if (isDrag) {
                                    onDragEnd()
                                    onInteraction()
                                } else {
                                    onExpandWindow()
                                    onCheckClipboard()
                                    onInteraction()
                                }
                                break
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Maximizar")
        }
    }
}
