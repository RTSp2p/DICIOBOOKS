import re

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'r') as f:
    content = f.read()

content = content.replace("if (params.height < 400) params.height = 400", "if (params.height < 200) params.height = 200")
content = content.replace("if (params.width < 300) params.width = 300", "if (params.width < 200) params.width = 200")

with open('app/src/main/java/com/example/service/FloatingDictionaryService.kt', 'w') as f:
    f.write(content)
