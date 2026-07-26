import re

with open('app/src/main/java/com/example/data/DictionaryRepository.kt', 'r') as f:
    content = f.read()

replacement = """        } catch (e: retrofit2.HttpException) {
            if (e.code() == 429) {
                Result.failure(Exception("Muitas requisições (Erro 429). Aguarde um momento e tente novamente."))
            } else {
                Result.failure(Exception("Erro na API: ${e.code()} - ${e.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }"""

content = content.replace("        } catch (e: Exception) {\n            Result.failure(e)\n        }", replacement)

with open('app/src/main/java/com/example/data/DictionaryRepository.kt', 'w') as f:
    f.write(content)
