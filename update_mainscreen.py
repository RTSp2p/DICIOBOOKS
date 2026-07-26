import re

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    content = f.read()

# Update SettingsScreen call
content = content.replace(
    'SettingsScreen(\n                    onOpenDrawer = { scope.launch { drawerState.open() } }\n                )',
    'SettingsScreen(\n                    viewModel = viewModel,\n                    onOpenDrawer = { scope.launch { drawerState.open() } }\n                )'
)

# Apply patch_mainscreen.kt
with open('patch_mainscreen.kt', 'r') as f:
    patch = f.read()

start_idx = content.find('@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun FavoritesScreen')
if start_idx != -1:
    new_content = content[:start_idx] + patch
    with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
        f.write(new_content)
    print("Replaced screens")
else:
    print("Could not find start index")
