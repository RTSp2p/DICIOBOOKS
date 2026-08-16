package com.example.service.floating

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ProcessingIndicator(modifier: Modifier = Modifier) {
    var isAnimating by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            isAnimating = !isAnimating
            delay(800)
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 0.8f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "ProcessingScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0.3f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "ProcessingAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(animatedScale)
                .alpha(animatedAlpha),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Processando...",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Pesquisando...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = animatedAlpha),
            fontWeight = FontWeight.Medium
        )
    }
}
