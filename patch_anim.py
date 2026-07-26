import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

new_indicator = """@Composable
fun ProcessingIndicator() {
    var isAnimating by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            isAnimating = !isAnimating
            kotlinx.coroutines.delay(800)
        }
    }
    
    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 0.8f,
        animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )
    
    val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0.3f,
        animationSpec = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    )

    Column(
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
            text = "Processando tradução...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = animatedAlpha),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}
"""

content = re.sub(r'@Composable\nfun ProcessingIndicator\(\) \{.*?\n\}\n', new_indicator, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
