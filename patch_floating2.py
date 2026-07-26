import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

replacement = """                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val scale = (maxWidth / 400.dp).coerceIn(0.5f, 1f)
                                    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
                                    val newDensity = androidx.compose.ui.unit.Density(currentDensity.density * scale, currentDensity.fontScale * scale)
                                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides newDensity) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                                    .imePadding()
                                                    .androidx.compose.foundation.verticalScroll(androidx.compose.foundation.rememberScrollState())
                                            ) {"""

# Replace from Box(modifier = Modifier.fillMaxWidth()) { down to imePadding() ) {
pattern = r'Box\(modifier = Modifier\.fillMaxWidth\(\)\) \{\s*Column\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.padding\(horizontal = 24\.dp, vertical = 16\.dp\)\s*\.windowInsetsPadding\(WindowInsets\.navigationBars\)\s*\.imePadding\(\)\s*\) \{'

content = re.sub(pattern, replacement.strip("\n"), content, count=1)


# Also close the braces
pattern_end = r'\}\s*\}\s*// Resize handle'
replacement_end = """}
                                        }
                                    }
                                }
                                
                                // Resize handle"""

content = re.sub(pattern_end, replacement_end.strip("\n"), content, count=1)

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
