import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

# Replace 'val scale by' with 'val animatedScale by'
content = content.replace('val scale by infiniteTransition.animateFloat(', 'val animatedScale by infiniteTransition.animateFloat(')
content = content.replace('.scale(scale)', '.scale(animatedScale)')
content = content.replace('val alpha by infiniteTransition.animateFloat(', 'val animatedAlpha by infiniteTransition.animateFloat(')
content = content.replace('.alpha(alpha)', '.alpha(animatedAlpha)')
content = content.replace('onSurfaceVariant.copy(alpha = alpha)', 'onSurfaceVariant.copy(alpha = animatedAlpha)')

# Let's fix animateFloat Unresolved Reference. 
# In compose, it's usually `import androidx.compose.animation.core.animateFloat` 
# Oh wait, animateFloat is a member function or extension function.
# Let's just use `import androidx.compose.animation.core.*` to be safe.
content = content.replace('import androidx.compose.animation.core.animateFloat', 'import androidx.compose.animation.core.*')

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
