with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

imports = """import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
"""

content = content.replace('import androidx.compose.runtime.Composable', imports + 'import androidx.compose.runtime.Composable')

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
