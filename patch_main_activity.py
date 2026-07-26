import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Add themeMode collection in setContent
set_content_replacement = """
    private val viewModel: DictionaryViewModel by androidx.activity.viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingTextToProcess = extractTextFromIntent(intent)
        handlePendingText()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            MyApplicationTheme(darkTheme = darkTheme) {
                com.example.ui.MainScreen(activity = this@MainActivity, viewModel = viewModel)
            }
        }
    }
"""

content = re.sub(
    r'override fun onCreate.*?setContent \{.*?MyApplicationTheme \{.*?com\.example\.ui\.MainScreen\(activity = this@MainActivity\).*?\}.*?\}.*?\}',
    set_content_replacement.strip(),
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)

