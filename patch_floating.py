import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

replacement = """
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val scale = (maxWidth / 400.dp).coerceIn(0.5f, 1f)
                                    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
                                    val newDensity = androidx.compose.ui.unit.Density(currentDensity.density * scale, currentDensity.fontScale * scale)
                                    CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides newDensity) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                                    .imePadding()
                                                    .androidx.compose.foundation.verticalScroll(androidx.compose.foundation.rememberScrollState())
                                            ) {
"""

content = content.replace("""
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 16.dp)
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                            .imePadding()
                                    ) {""".strip("\n"), replacement.strip("\n"))

# And close the extra brace
replacement_end = """
                                        }
                                    }
                                }
"""

content = content.replace("""
                                    }
                                }
                                
                                // Resize handle
""".strip("\n"), replacement_end.strip("\n") + "\n                                \n                                // Resize handle")


with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
