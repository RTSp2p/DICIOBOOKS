import re

with open('app/src/main/java/com/example/data/DictionaryRepository.kt', 'r') as f:
    content = f.read()

replacement = """
    suspend fun getDefinitionAndSynonyms(word: String, retryCount: Int = 3): Result<DictionaryResult> = withContext(Dispatchers.IO) {
        var currentRetry = 0
        var lastException: Exception? = null
        
        while (currentRetry < retryCount) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    return@withContext Result.failure(Exception("API Key not configured."))
                }

                val prompt = \"\"\"
                    Analise a seguinte palavra ou frase em português: "$word".
                    Forneça o significado e sinônimos no formato exato abaixo, sem adicionar mais nada.
                    
                    Classe Gramatical: [Substantivo/Adjetivo/Verbo/etc.]
                    Definição: [A definição mais comum e contextualizada]
                    Sinônimos: [Sinônimo 1, Sinônimo 2, Sinônimo 3]
                \"\"\".trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = prompt)))
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = "Você é um dicionário conciso e preciso. Responda apenas no formato solicitado.")))
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                if (responseText.isEmpty()) {
                    return@withContext Result.failure(Exception("Resposta vazia da API."))
                }

                var partOfSpeech = "Desconhecida"
                var definition = "Definição não encontrada."
                var synonyms = listOf<String>()

                responseText.lines().forEach { line ->
                    when {
                        line.startsWith("Classe Gramatical:", ignoreCase = true) -> partOfSpeech = line.substringAfter(":").trim()
                        line.startsWith("Definição:", ignoreCase = true) -> definition = line.substringAfter(":").trim()
                        line.startsWith("Sinônimos:", ignoreCase = true) -> {
                            synonyms = line.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        }
                    }
                }

                return@withContext Result.success(DictionaryResult(
                    word = word.trim(),
                    partOfSpeech = partOfSpeech,
                    definition = definition,
                    synonyms = synonyms
                ))
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 429) {
                    lastException = Exception("Muitas requisições (Erro 429). Aguarde um momento e tente novamente.")
                    currentRetry++
                    kotlinx.coroutines.delay(1000L * currentRetry) // Backoff
                } else {
                    return@withContext Result.failure(Exception("Erro na API: ${e.code()} - ${e.message()}"))
                }
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }
        
        return@withContext Result.failure(lastException ?: Exception("Muitas requisições (Erro 429). Aguarde um momento e tente novamente."))
    }
"""

content = re.sub(
    r'suspend fun getDefinitionAndSynonyms\(word: String\): Result<DictionaryResult> = withContext\(Dispatchers\.IO\) \{.*?\n    \}',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/data/DictionaryRepository.kt', 'w') as f:
    f.write(content)
