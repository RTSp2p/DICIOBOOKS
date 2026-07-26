import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

# Let's count how many braces need to be removed.
# The error says `override fun onDestroy()` is a top level function, so we have 1 extra closing brace.
# Wait, if we have 1 extra closing brace, let's just remove one `}` before `frameLayout.addView(view)`.

content = content.replace("}\n            }\n        }\n        frameLayout.addView(view)", "            }\n        }\n        frameLayout.addView(view)")
content = content.replace("}\n        }\n        frameLayout.addView(view)", "    }\n        frameLayout.addView(view)")

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
