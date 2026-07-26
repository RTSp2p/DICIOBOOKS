import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

content = content.replace('.androidx.compose.ui.draw.scale(animatedScale)\n                .androidx.compose.ui.draw.alpha(animatedAlpha)', '.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale; alpha = animatedAlpha }')
content = content.replace('.scale(animatedScale)\n                .alpha(animatedAlpha)', '.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale; alpha = animatedAlpha }')

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
