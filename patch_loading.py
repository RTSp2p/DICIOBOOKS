import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

processing_indicator = """@Composable
fun ProcessingIndicator() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "processing")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .alpha(alpha),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}
"""

replacement = """                                                    is DictionaryUiState.Loading -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(200.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            ProcessingIndicator()
                                                        }
                                                    }"""

content = re.sub(
    r'is DictionaryUiState\.Loading -> \{\s*Box\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.height\(200\.dp\),\s*contentAlignment = Alignment\.Center\s*\) \{\s*CircularProgressIndicator\(\)\s*\}\s*\}',
    replacement.strip(),
    content
)

# Add ProcessingIndicator at the top level, right before class FloatingDictionaryService
content = content.replace('class FloatingDictionaryService', processing_indicator + '\nclass FloatingDictionaryService')

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
