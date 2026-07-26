import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Add ThemeMode to MainActivity.kt
theme_mode = """
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("dictionary_prefs", android.content.Context.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }
"""

content = content.replace("class DictionaryViewModel(application: Application) : AndroidViewModel(application) {", theme_mode)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

