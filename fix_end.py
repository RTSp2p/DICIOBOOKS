import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

start_str = '                                    Box(\n                                        modifier = Modifier\n                                            .align(Alignment.TopStart)'
start_idx = content.find(start_str)

end_part = """                                    Box(
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
                            }
                        }
                    }
                }
            }
        }
        
        frameLayout.addView(view)
        
        composeView = frameLayout
        windowManager?.addView(frameLayout, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        
        store.clear()
        tts?.stop()
        tts?.shutdown()
        
        composeView?.let {
            windowManager?.removeView(it)
        }
        composeView = null
    }
}
"""

new_content = content[:start_idx] + end_part
with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(new_content)
