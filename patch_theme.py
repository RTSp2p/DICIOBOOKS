import re

with open('app/src/main/java/com/example/ui/theme/Theme.kt', 'r') as f:
    content = f.read()

replacement = """@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ElegantDarkColorScheme
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}"""

content = re.sub(r'@Composable\nfun MyApplicationTheme.*?\}', replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/theme/Theme.kt', 'w') as f:
    f.write(content)
