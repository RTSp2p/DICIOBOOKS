import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

# First, add the imports at the top
imports = """import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
"""
content = content.replace("import androidx.compose.ui.unit.IntSize", imports + "import androidx.compose.ui.unit.IntSize")

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
