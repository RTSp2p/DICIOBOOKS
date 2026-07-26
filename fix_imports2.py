import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

imports = """import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
"""

content = content.replace('import androidx.compose.animation.core.*', imports + 'import androidx.compose.animation.core.*')

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
