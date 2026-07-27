import re

with open('app/src/main/java/com/example/network/GeminiApi.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'import com.squareup.moshi.JsonClass',
    'import com.squareup.moshi.JsonClass\nimport com.squareup.moshi.Json'
)

content = content.replace(
    '    val systemInstruction: Content? = null',
    '    @Json(name = "system_instruction")\n    val systemInstruction: Content? = null'
)

with open('app/src/main/java/com/example/network/GeminiApi.kt', 'w') as f:
    f.write(content)
print("Moshi system_instruction fixed")
