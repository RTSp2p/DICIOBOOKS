package com.example.data

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

data class DictionaryResult(
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val synonyms: List<String>
)

class DictionaryRepository {
    suspend fun getDefinitionAndSynonyms(
        word: String,
        targetLanguage: TranslationLanguage = TranslationLanguage.PORTUGUESE,
        searchMode: SearchMode = SearchMode.MEANING
    ): Result<DictionaryResult> = withContext(Dispatchers.IO) {
        // 1. Tentar obter resultado via Google Gemini AI
        val geminiResult = queryGemini(word, targetLanguage, searchMode)
        if (geminiResult.isSuccess) {
            return@withContext geminiResult
        }

        // 2. Fallback transparente para Groq (Llama-3.3-70b) caso o Gemini falhe ou esteja sem chave
        val groqResult = queryGroq(word, targetLanguage, searchMode)
        if (groqResult.isSuccess) {
            return@withContext groqResult
        }

        // 3. Se ambos falharem, retornar erro consolidado com diagnóstico de ambos
        val geminiErr = geminiResult.exceptionOrNull()?.message ?: "Serviço indisponível"
        val groqErr = groqResult.exceptionOrNull()?.message ?: "Serviço indisponível"

        Result.failure(
            Exception("Não foi possível processar a consulta nos provedores de IA.\n• Gemini: $geminiErr\n• Groq: $groqErr")
        )
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

        return try {
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
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = promptText)))),
                systemInstruction = systemInstruction
            )

            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            val defaultPos = if (searchMode == SearchMode.TRANSLATION) "Tradução ($targetName)" else "Desconhecida"
            val parsed = parseApiResponse(responseText, word, defaultPos)

            if (parsed != null && parsed.definition.isNotBlank()) {
                Result.success(parsed)
            } else {
                Result.failure(Exception("Resposta sem conteúdo do Gemini."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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

        return try {
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

            val request = GroqChatRequest(
                model = "llama-3.3-70b-versatile",
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
                Result.success(parsed)
            } else {
                Result.failure(Exception("Resposta sem conteúdo do Groq."))
            }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val parsedMessage = try {
                if (!errorBody.isNullOrBlank()) {
                    val jsonObj = org.json.JSONObject(errorBody)
                    jsonObj.optJSONObject("error")?.optString("message") ?: errorBody
                } else e.message()
            } catch (_: Exception) {
                errorBody ?: e.message()
            }
            if (e.code() == 429) {
                Result.failure(Exception("Muitas requisições (Erro 429)."))
            } else {
                Result.failure(Exception("Erro HTTP ${e.code()}: $parsedMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseApiResponse(
        responseText: String,
        word: String,
        defaultPartOfSpeech: String
    ): DictionaryResult? {
        if (responseText.isBlank()) return null

        var partOfSpeech = defaultPartOfSpeech
        var definition = ""
        var synonyms = listOf<String>()

        responseText.lines().forEach { line ->
            when {
                line.startsWith("Classe Gramatical:", ignoreCase = true) -> {
                    val pos = line.substringAfter(":").trim()
                    if (pos.isNotBlank()) partOfSpeech = pos
                }
                line.startsWith("Definição:", ignoreCase = true) -> {
                    val def = line.substringAfter(":").trim()
                    if (def.isNotBlank()) definition = def
                }
                line.startsWith("Sinônimos:", ignoreCase = true) -> {
                    synonyms = line.substringAfter(":")
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                }
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

    private fun getGeminiApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (_: Exception) { "" }
    }

    private fun getGroqApiKey(): String {
        return try {
            val groqKey = BuildConfig.GROQ_API_KEY
            if (groqKey.isNotBlank() && groqKey != "MY_GROQ_API_KEY") groqKey else ""
        } catch (_: Exception) { "" }
    }
}
