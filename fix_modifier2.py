import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.compose.animation.core.*', 'import androidx.compose.animation.core.*\nimport androidx.compose.ui.graphics.graphicsLayer\n')

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
