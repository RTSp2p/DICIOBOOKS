import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.compose.ui.graphics.graphicsLayer', 'import androidx.compose.ui.draw.*')
content = content.replace('.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale; alpha = animatedAlpha }', '.scale(animatedScale).alpha(animatedAlpha)')

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
