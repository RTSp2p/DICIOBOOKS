import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

target = """                                                            Button(
                                                                onClick = {
                                                                    scope.launch {
                                                                        isVisible = false
                                                                        kotlinx.coroutines.delay(300)
                                                                        stopSelf()
                                                                    }
                                                                }
                                                            ) {
                                                                Text("Fechar")
                                                            }"""

replacement = """                                                            Button(
                                                                onClick = {
                                                                    scope.launch {
                                                                        isVisible = false
                                                                        viewModel.hideBottomSheet()
                                                                    }
                                                                }
                                                            ) {
                                                                Text("Fechar")
                                                            }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
        f.write(content)
    print("Replaced button action successfully")
else:
    print("Target not found")
