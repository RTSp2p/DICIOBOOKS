import re

with open('app/src/main/java/com/example/ui/MainScreen.kt', 'r') as f:
    content = f.read()

with open('patch_screens.kt', 'r') as f:
    patch = f.read()

# Replace FavoritesScreen and HistoryScreen
# Find start of FavoritesScreen and end of HistoryScreen
start_idx = content.find('@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun FavoritesScreen')
end_idx = content.find('@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun SettingsScreen')

if start_idx != -1 and end_idx != -1:
    new_content = content[:start_idx] + patch + "\n" + content[end_idx:]
    with open('app/src/main/java/com/example/ui/MainScreen.kt', 'w') as f:
        f.write(new_content)
    print("Replaced successfully")
else:
    print(f"Could not find indices: start {start_idx}, end {end_idx}")

