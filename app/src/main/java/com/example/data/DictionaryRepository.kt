package com.example.data

import android.content.Context
import com.example.BuildConfig
import com.example.network.GeminiContent
import com.example.network.GeminiGenerateContentRequest
import com.example.network.GeminiPart
import com.example.network.GeminiRetrofitClient
import com.example.network.GroqChatRequest
import com.example.network.GroqMessage
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class DictionaryResult(
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val synonyms: List<String>
)

class DictionaryRepository(private val context: Context? = null) {

    private val geminiCandidateModels = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-flash",
        "gemini-flash-latest"
    )

    private val groqCandidateModels = listOf(
        "qwen/qwen3.6-27b",
        "openai/gpt-oss-20b",
        "groq/compound",
        "groq/compound-mini"
    )

    suspend fun getDefinitionAndSynonyms(
        word: String,
        targetLanguage: TranslationLanguage = TranslationLanguage.PORTUGUESE,
        searchMode: SearchMode = SearchMode.MEANING
    ): Result<DictionaryResult> = withContext(Dispatchers.IO) {
        val trimmedWord = word.trim()
        if (trimmedWord.isBlank()) {
            return@withContext Result.failure(Exception("Nenhum termo informado para busca."))
        }

        // [MODO TESTE GROQ]: Busca do Gemini desativada temporariamente a pedido do usuário (comentar o "1.")
        
        // 1. Tentar obter resultado via Google Gemini AI (com cascata de modelos)
        val geminiResult = queryGemini(trimmedWord, targetLanguage, searchMode)
        if (geminiResult.isSuccess) {
            val data = geminiResult.getOrNull()
            if (data != null) saveToCache(data, targetLanguage, searchMode)
            return@withContext geminiResult
        }
        
        // 2. Fallback transparente para Groq caso o Gemini falhe ou esteja sem chave
        val groqResult = queryGroq(trimmedWord, targetLanguage, searchMode)
        if (groqResult.isSuccess) {
            val data = groqResult.getOrNull()
            if (data != null) saveToCache(data, targetLanguage, searchMode)
            return@withContext groqResult
        }

        // 3. Fallback Offline: Se ambos os provedores online falharem (sem internet, instabilidade, etc.), buscar no cache local
        val cachedResult = getFromCache(trimmedWord, targetLanguage, searchMode)
        if (cachedResult != null) {
            return@withContext Result.success(cachedResult)
        }

        // 4. Se ambos falharem e não houver cache offline, retornar diagnóstico detalhado
        val geminiErr = geminiResult.exceptionOrNull()?.message ?: "Serviço indisponível"
        val groqErr = groqResult.exceptionOrNull()?.message ?: "Serviço indisponível"
        val hasGeminiKey = getGeminiApiKey().isNotBlank()
        val hasGroqKey = getGroqApiKey().isNotBlank()

        val details = buildString {
            append("Não foi possível processar a consulta online e nenhum resultado foi encontrado no cache offline.\n\n")
            append("• Google Gemini: $geminiErr\n")
            append("• Groq: $groqErr")
            if (!hasGeminiKey && !hasGroqKey) {
                append("\n\n💡 Dica: Conecte-se à internet ou insira sua chave de API (Gemini ou Groq) no painel de Segredos (Secrets) ou em Configurações > Provedores de IA.")
            }
        }

        Result.failure(Exception(details))
    }

    suspend fun saveToCache(
        result: DictionaryResult,
        targetLanguage: TranslationLanguage,
        searchMode: SearchMode
    ) {
        if (context == null) return
        try {
            val db = AppDatabase.getDatabase(context)
            db.cachedDefinitionDao().insertCache(
                CachedDefinition(
                    word = result.word,
                    targetLanguage = targetLanguage.code,
                    searchMode = searchMode.name,
                    partOfSpeech = result.partOfSpeech,
                    definition = result.definition,
                    synonyms = result.synonyms.joinToString(", ")
                )
            )
        } catch (_: Exception) {
            // Ignora falha de escrita em cache
        }
    }

    suspend fun getFromCache(
        word: String,
        targetLanguage: TranslationLanguage,
        searchMode: SearchMode
    ): DictionaryResult? {
        if (context == null) return null
        return try {
            val db = AppDatabase.getDatabase(context)
            val cached = db.cachedDefinitionDao().getCached(word, targetLanguage.code, searchMode.name)
                ?: db.cachedDefinitionDao().getAnyCached(word)

            if (cached != null) {
                val synList = cached.synonyms
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                DictionaryResult(
                    word = cached.word,
                    partOfSpeech = cached.partOfSpeech,
                    definition = cached.definition,
                    synonyms = synList
                )
            } else {
                // Fallback secundário no histórico de buscas locais caso exista registro
                val history = db.searchHistoryDao().getSearchByWord(word)
                if (history != null) {
                    val synList = history.synonyms
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    DictionaryResult(
                        word = history.word,
                        partOfSpeech = history.partOfSpeech,
                        definition = history.definition,
                        synonyms = synList
                    )
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearCache() {
        if (context == null) return
        try {
            AppDatabase.getDatabase(context).cachedDefinitionDao().clearCache()
        } catch (_: Exception) {}
    }

    private suspend fun queryGemini(
        word: String,
        targetLanguage: TranslationLanguage,
        searchMode: SearchMode
    ): Result<DictionaryResult> {
        val apiKey = getGeminiApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Chave do Gemini não configurada."))
        }

        val targetName = targetLanguage.displayName
        val targetCode = targetLanguage.code

        val promptText = if (searchMode == SearchMode.TRANSLATION) {
            """
                Traduza com alta precisão e contexto natural a seguinte palavra ou frase para $targetName ($targetCode): "$word".
                Forneça a resposta exatamente no formato abaixo, sem texto adicional:

                Classe Gramatical: Tradução ($targetName)
                Definição: [A tradução direta e contextualizada para $targetName]
                Sinônimos: [Traduções ou palavras alternativas em $targetName, separadas por vírgula]
            """.trimIndent()
        } else {
            """
                Analise a seguinte palavra ou frase: "$word".
                Forneça a definição, classe gramatical e sinônimos explicados em $targetName ($targetCode) no formato exato abaixo, sem adicionar nada mais:

                Classe Gramatical: [Substantivo/Adjetivo/Verbo/etc. em $targetName]
                Definição: [A definição e explicação clara em $targetName]
                Sinônimos: [Sinônimo 1, Sinônimo 2, Sinônimo 3 em $targetName]
            """.trimIndent()
        }

        val systemInstructionText = if (searchMode == SearchMode.TRANSLATION) {
            "Você é um tradutor profissional alimentado pelo Google Gemini AI. O idioma de destino preferido é $targetName ($targetCode). Adapte a tradução para soar natural neste idioma. Responda apenas no formato exato solicitado."
        } else {
            "Você é um dicionário e analista linguístico profissional alimentado pelo Google Gemini AI. O idioma das explicações e definições é $targetName ($targetCode). Responda apenas no formato exato solicitado."
        }

        val systemInstruction = GeminiContent(
            parts = listOf(GeminiPart(text = systemInstructionText))
        )

        val request = GeminiGenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = promptText)),
                    role = "user"
                )
            ),
            systemInstruction = systemInstruction
        )

        var lastError: Exception = Exception("Nenhum modelo do Gemini respondeu.")

        for (model in geminiCandidateModels) {
            try {
                val response = GeminiRetrofitClient.service.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

                val defaultPos = if (searchMode == SearchMode.TRANSLATION) "Tradução ($targetName)" else "Desconhecida"
                val parsed = parseApiResponse(responseText, word, defaultPos)

                if (parsed != null && parsed.definition.isNotBlank()) {
                    return Result.success(parsed)
                }
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                val parsedMessage = parseHttpErrorMessage(errorBody, e.message())

                if (code == 400 && (parsedMessage.contains("API key not valid", ignoreCase = true) ||
                            parsedMessage.contains("API_KEY_INVALID", ignoreCase = true))) {
                    return Result.failure(Exception("Chave do Gemini inválida ou expirada."))
                }

                if (code == 403) {
                    return Result.failure(Exception("Acesso negado (403): verifique as permissões da sua chave Gemini."))
                }

                lastError = Exception("Erro HTTP $code ($model): $parsedMessage")
                // Se for 404 ou 400 referente ao modelo, continua tentando os outros modelos da lista
            } catch (e: Exception) {
                lastError = e
            }
        }

        return Result.failure(lastError)
    }

    private suspend fun queryGroq(
        word: String,
        targetLanguage: TranslationLanguage,
        searchMode: SearchMode
    ): Result<DictionaryResult> {
        val apiKey = getGroqApiKey()
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Chave do Groq não configurada."))
        }

        val targetName = targetLanguage.displayName
        val targetCode = targetLanguage.code
        val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"

        val promptText = if (searchMode == SearchMode.TRANSLATION) {
            """
                Traduza a palavra ou texto a seguir para $targetName ($targetCode): "$word".
                Forneça a resposta exatamente no formato abaixo, sem texto adicional:

                Classe Gramatical: Tradução ($targetName)
                Definição: [Texto traduzido em $targetName]
                Sinônimos: [Traduções alternativas separadas por vírgula]
            """.trimIndent()
        } else {
            """
                Analise a seguinte palavra ou frase: "$word".
                Forneça a definição, classe gramatical e sinônimos explicados em $targetName ($targetCode) no formato exato abaixo, sem adicionar nada mais:

                Classe Gramatical: [Substantivo/Adjetivo/Verbo/etc.]
                Definição: [A definição e explicação em $targetName]
                Sinônimos: [Sinônimo 1, Sinônimo 2, Sinônimo 3]
            """.trimIndent()
        }

        val systemInstructionText = if (searchMode == SearchMode.TRANSLATION) {
            "Você é um tradutor conciso e preciso. O idioma de destino é $targetName ($targetCode). Responda apenas no formato solicitado."
        } else {
            "Você é um dicionário e tradutor conciso e preciso. Responda apenas no formato solicitado com explicações em $targetName ($targetCode)."
        }

        var lastError: Exception = Exception("Nenhum modelo Groq disponível.")

        for (model in groqCandidateModels) {
            try {
                val request = GroqChatRequest(
                    model = model,
                    messages = listOf(
                        GroqMessage(role = "system", content = systemInstructionText),
                        GroqMessage(role = "user", content = promptText)
                    ),
                    temperature = 0.2
                )

                val response = RetrofitClient.service.chatCompletions(authHeader, request)
                val responseText = response.choices?.firstOrNull()?.message?.content ?: ""

                val defaultPos = if (searchMode == SearchMode.TRANSLATION) "Tradução ($targetName)" else "Desconhecida"
                val parsed = parseApiResponse(responseText, word, defaultPos)

                if (parsed != null && parsed.definition.isNotBlank()) {
                    return Result.success(parsed)
                }
            } catch (e: retrofit2.HttpException) {
                val code = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                val parsedMessage = parseHttpErrorMessage(errorBody, e.message())

                if (code == 401) {
                    return Result.failure(Exception("Chave do Groq inválida ou não autorizada."))
                }
                if (code == 429) {
                    lastError = Exception("Limite de requisições excedido no Groq (429).")
                    continue
                }

                lastError = Exception("Erro HTTP $code: $parsedMessage")
                // Se o modelo não existir (404/400), o loop tenta o próximo modelo (ex: llama-3.1-8b-instant)
            } catch (e: Exception) {
                lastError = e
            }
        }

        return Result.failure(lastError)
    }

    private fun parseHttpErrorMessage(errorBody: String?, fallback: String): String {
        if (errorBody.isNullOrBlank()) return fallback
        return try {
            val jsonObj = JSONObject(errorBody)
            val errorObj = jsonObj.optJSONObject("error")
            if (errorObj != null) {
                val msg = errorObj.optString("message")
                if (msg.isNotBlank()) msg else errorObj.optString("status", errorBody)
            } else {
                jsonObj.optString("message", errorBody)
            }
        } catch (_: Exception) {
            errorBody
        }
    }

    private fun parseApiResponse(
        responseText: String,
        word: String,
        defaultPartOfSpeech: String
    ): DictionaryResult? {
        val trimmedRaw = responseText.trim()
        if (trimmedRaw.isBlank()) return null

        var partOfSpeech = defaultPartOfSpeech
        var definition = ""
        var synonyms = listOf<String>()

        val lines = trimmedRaw.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isBlank()) continue

            // Remove marcações markdown comuns como **, *, #, -, • do início ou da linha
            val cleanLine = line
                .replace("**", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "")
                .replace("•", "")
                .trim()
                .removePrefix("-")
                .removePrefix("*")
                .trim()

            when {
                cleanLine.startsWith("Classe Gramatical:", ignoreCase = true) ||
                cleanLine.startsWith("Classe gramatical:", ignoreCase = true) ||
                cleanLine.startsWith("Part of Speech:", ignoreCase = true) -> {
                    val pos = cleanLine.substringAfter(":").trim().removeSurrounding("[", "]").trim()
                    if (pos.isNotBlank()) partOfSpeech = pos
                }
                cleanLine.startsWith("Definição:", ignoreCase = true) ||
                cleanLine.startsWith("Definicao:", ignoreCase = true) ||
                cleanLine.startsWith("Significado:", ignoreCase = true) ||
                cleanLine.startsWith("Tradução:", ignoreCase = true) ||
                cleanLine.startsWith("Traducao:", ignoreCase = true) ||
                cleanLine.startsWith("Definition:", ignoreCase = true) -> {
                    val def = cleanLine.substringAfter(":").trim().removeSurrounding("[", "]").trim()
                    if (def.isNotBlank()) definition = def
                }
                cleanLine.startsWith("Sinônimos:", ignoreCase = true) ||
                cleanLine.startsWith("Sinonimos:", ignoreCase = true) ||
                cleanLine.startsWith("Synonyms:", ignoreCase = true) ||
                cleanLine.startsWith("Alternativas:", ignoreCase = true) -> {
                    val synRaw = cleanLine.substringAfter(":").trim().removeSurrounding("[", "]").trim()
                    synonyms = synRaw
                        .split(",")
                        .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                        .filter { it.isNotEmpty() && !it.equals("Nenhum", ignoreCase = true) && !it.equals("None", ignoreCase = true) }
                }
            }
        }

        // Fallback inteligente: se a resposta não continha tags estruturadas explícitas,
        // limpa o texto markdown e usa a resposta integral como definição em vez de descartar.
        if (definition.isBlank()) {
            val cleanFullText = trimmedRaw
                .replace("**", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "")
                .lines()
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .trim()

            if (cleanFullText.isNotBlank()) {
                definition = cleanFullText
            }
        }

        if (definition.isBlank()) return null

        return DictionaryResult(
            word = word.trim(),
            partOfSpeech = partOfSpeech,
            definition = definition,
            synonyms = synonyms
        )
    }

    fun getGeminiApiKey(): String {
        // Prioridade 1: Chave configurada pelo usuário nas Configurações
        val customKey = context?.getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)
            ?.getString("gemini_custom_api_key", "")?.trim() ?: ""
        if (customKey.isNotBlank()) return customKey

        // Prioridade 2: Chave injetada via BuildConfig (.env)
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (_: Exception) { "" }
    }

    fun getGroqApiKey(): String {
        // Prioridade 1: Chave configurada pelo usuário nas Configurações
        val customKey = context?.getSharedPreferences("dictionary_prefs", Context.MODE_PRIVATE)
            ?.getString("groq_custom_api_key", "")?.trim() ?: ""
        if (customKey.isNotBlank()) return customKey

        // Prioridade 2: Chave injetada via BuildConfig (.env)
        return try {
            val groqKey = BuildConfig.GROQ_API_KEY
            if (groqKey.isNotBlank() && groqKey != "MY_GROQ_API_KEY") groqKey else ""
        } catch (_: Exception) { "" }
    }
}
