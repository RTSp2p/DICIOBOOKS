import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

content = content.replace("shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),", "shape = RoundedCornerShape(28.dp),")

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
