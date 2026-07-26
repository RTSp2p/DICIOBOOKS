import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

replacement = """
                val themeMode by viewModel.themeMode.collectAsState()
                val darkTheme = when (themeMode) {
                    com.example.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                    com.example.ThemeMode.LIGHT -> false
                    com.example.ThemeMode.DARK -> true
                }
                MyApplicationTheme(darkTheme = darkTheme) {
"""

content = content.replace("MyApplicationTheme {", replacement)

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)

