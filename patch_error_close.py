import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

target = """                                                    is DictionaryUiState.Error -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(200.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = "Erro: ${state.message}",
                                                                color = MaterialTheme.colorScheme.error,
                                                                style = MaterialTheme.typography.bodyLarge
                                                            )
                                                        }
                                                    }"""

replacement = """                                                    is DictionaryUiState.Error -> {
                                                        androidx.compose.foundation.layout.Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(24.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                                        ) {
                                                            Text(
                                                                text = "Erro: ${state.message}",
                                                                color = MaterialTheme.colorScheme.error,
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                                modifier = Modifier.padding(bottom = 16.dp)
                                                            )
                                                            Button(
                                                                onClick = {
                                                                    scope.launch {
                                                                        isVisible = false
                                                                        kotlinx.coroutines.delay(300)
                                                                        stopSelf()
                                                                    }
                                                                }
                                                            ) {
                                                                Text("Fechar")
                                                            }
                                                        }
                                                    }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
