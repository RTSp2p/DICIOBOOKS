import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

# Replace from `Surface(` to the end of `Minimize handle`
start = content.find('Surface(\n                                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp)')
end_str = 'Icon(\n                                        imageVector = Icons.Default.CloseFullscreen,\n                                        contentDescription = "Minimizar",\n                                        tint = MaterialTheme.colorScheme.onSurfaceVariant\n                                    )\n                                }\n                            }'
end = content.find(end_str) + len(end_str)

replacement = """Surface(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shadowElevation = 16.dp
                            ) {
                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val scale = (maxWidth / 400.dp).coerceIn(0.5f, 1f)
                                    val currentDensity = LocalDensity.current
                                    val newDensity = Density(currentDensity.density * scale, currentDensity.fontScale * scale)
                                    
                                    CompositionLocalProvider(LocalDensity provides newDensity) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                                    .windowInsetsPadding(WindowInsets.navigationBars)
                                                    .imePadding()
                                                    .verticalScroll(rememberScrollState())
                                            ) {
                                                // Drag handle mockup
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .pointerInput(Unit) {
                                                            detectDragGestures { change, dragAmount ->
                                                                change.consume()
                                                                params.y = Math.max(0, params.y - dragAmount.y.toInt())
                                                                params.x += dragAmount.x.toInt()
                                                                windowManager?.updateViewLayout(frameLayout, params)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(32.dp)
                                                            .height(4.dp)
                                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(16.dp))
                                                
                                                when (val state = uiState) {
                                                    is DictionaryUiState.Idle -> {
                                                        var textToSearch by remember { mutableStateOf("") }
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            OutlinedTextField(
                                                                value = textToSearch,
                                                                onValueChange = { textToSearch = it },
                                                                label = { Text("Pesquisar palavra") },
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                            Spacer(modifier = Modifier.height(16.dp))
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                            ) {
                                                                OutlinedButton(
                                                                    onClick = {
                                                                        scope.launch {
                                                                            isVisible = false
                                                                            kotlinx.coroutines.delay(300)
                                                                            stopSelf()
                                                                        }
                                                                    },
                                                                    modifier = Modifier.weight(1f)
                                                                ) {
                                                                    Text("Fechar")
                                                                }
                                                                Button(
                                                                    onClick = { 
                                                                        if (textToSearch.isNotBlank()) {
                                                                            viewModel.showBottomSheet(textToSearch)
                                                                        }
                                                                    },
                                                                    modifier = Modifier.weight(1f)
                                                                ) {
                                                                    Text("Buscar")
                                                                }
                                                            }
                                                        }
                                                    }
                                                    is DictionaryUiState.Loading -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(200.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator()
                                                        }
                                                    }
                                                    is DictionaryUiState.Error -> {
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
                                                    }
                                                    is DictionaryUiState.Success -> {
                                                        DictionaryResultContent(
                                                            state = state,
                                                            tts = tts,
                                                            isFavorite = isCurrentWordFavorite,
                                                            onToggleFavorite = { viewModel.toggleFavorite(state.result.word, state.result.definition) },
                                                            onClose = {
                                                                scope.launch {
                                                                    isVisible = false
                                                                    kotlinx.coroutines.delay(300)
                                                                    stopSelf()
                                                                }
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Resize handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp)
                                            .size(32.dp)
                                            .pointerInput(Unit) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        if (params.width == WindowManager.LayoutParams.MATCH_PARENT) {
                                                            params.width = frameLayout.width
                                                        }
                                                        if (params.height == WindowManager.LayoutParams.WRAP_CONTENT) {
                                                            params.height = frameLayout.height
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        params.height -= dragAmount.y.toInt()
                                                        params.width += (dragAmount.x.toInt() * 2)
                                                        
                                                        // Min sizes
                                                        if (params.height < 200) params.height = 200
                                                        if (params.width < 200) params.width = 200
                                                        
                                                        windowManager?.updateViewLayout(frameLayout, params)
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenWith,
                                            contentDescription = "Redimensionar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // Minimize handle
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(16.dp)
                                            .size(32.dp)
                                            .clickable { isMinimized = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloseFullscreen,
                                            contentDescription = "Minimizar",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }"""

new_content = content[:start] + replacement + content[end:]
# fix duplicate closing braces before frameLayout.addView
new_content = new_content.replace("""                        }
                    }
                    }
                }
            }
        }
        frameLayout.addView(view)""", """                        }
                    }
                }
            }
        }
        frameLayout.addView(view)""")

new_content = new_content.replace("""                        }
                    }
                }
            }
        }
        frameLayout.addView(view)""", """                        }
                    }
                }
            }
        }
        frameLayout.addView(view)""")

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(new_content)
