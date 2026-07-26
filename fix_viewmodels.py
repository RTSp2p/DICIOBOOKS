import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'private val viewModel: DictionaryViewModel by androidx.activity.viewModels()',
    'private val viewModel: DictionaryViewModel by viewModels()'
)

if 'import androidx.activity.viewModels' not in content:
    content = content.replace(
        'import androidx.activity.ComponentActivity',
        'import androidx.activity.ComponentActivity\nimport androidx.activity.viewModels'
    )

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
