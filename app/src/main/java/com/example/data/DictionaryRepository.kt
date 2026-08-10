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
        if (searchMode == SearchMode.TRANSLATION) {
            translateWithGemini(word, targetLanguage)
        } else {
            fetchMeaningWithGroq(word, targetLanguage, isTranslationMode = false)
        }
    }

    private suspend fun translateWithGemini(
        word: String,
        targetLanguage: TranslationLanguage
    ): Result<DictionaryResult> {
        val geminiKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (_: Exception) { "" }

        if (geminiKey.isNotBlank()) {
            try {
                val targetName = targetLanguage.displayName
                val promptText = """
                    Traduza com alta precisão e contexto natural a seguinte palavra ou frase para $targetName (${targetLanguage.code}): "$word".
                    Forneça a resposta exatamente no formato abaixo, sem texto adicional:

                    Classe Gramatical: Tradução ($targetName)
                    Definição: [A tradução direta e contextualizada para $targetName]
                    Sinônimos: [Traduções ou palavras alternativas em $targetName, separadas por vírgula]
                """.trimIndent()

                val systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = "Você é um tradutor profissional alimentado pelo Google Gemini AI. O idioma de tradução preferido do usuário é $targetName (${targetLanguage.code}). Adapte a tradução para soar natural neste idioma de destino. Responda apenas no formato exato solicitado."))
                )

                val request = GeminiGenerateContentRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = promptText)))),
                    systemInstruction = systemInstruction
                )

                val response = GeminiRetrofitClient.service.generateContent(geminiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

                if (responseText.isNotBlank()) {
                    var partOfSpeech = "Tradução (${targetLanguage.displayName})"
                    var definition = ""
                    var synonyms = listOf<String>()

                    responseText.lines().forEach { line ->
                        when {
                            line.startsWith("Classe Gramatical:", ignoreCase = true) -> {
                                val pos = line.substringAfter(":").trim()
                                if (pos.isNotBlank()) partOfSpeech = pos
                            }
                            line.startsWith("Definição:", ignoreCase = true) -> {
                                definition = line.substringAfter(":").trim()
                            }
                            line.startsWith("Sinônimos:", ignoreCase = true) -> {
                                synonyms = line.substringAfter(":").split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            }
                        }
                    }

                    if (definition.isNotBlank()) {
                        return Result.success(
                            DictionaryResult(
                                word = word.trim(),
                                partOfSpeech = partOfSpeech,
                                definition = definition,
                                synonyms = synonyms
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // If Gemini call fails, continue to fallback below
            }
        }

        return fetchMeaningWithGroq(word, targetLanguage, isTranslationMode = true)
    }

    private suspend fun fetchMeaningWithGroq(
        word: String,
        targetLanguage: TranslationLanguage,
        isTranslationMode: Boolean
    ): Result<DictionaryResult> {
        try {
            val apiKey = try {
                val groqKey = BuildConfig.GROQ_API_KEY
                if (groqKey.isNotBlank() && groqKey != "MY_GROQ_API_KEY") groqKey else BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) {
                BuildConfig.GEMINI_API_KEY
            }

            if (apiKey.isBlank() || apiKey == "MY_GROQ_API_KEY" || apiKey == "MY_GEMINI_API_KEY") {
                return Result.failure(Exception("Chave de API não configurada nas variáveis do app."))
            }

            val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"

            val prompt = if (isTranslationMode) {
                """
                    Traduza a palavra ou texto a seguir para ${targetLanguage.displayName}: "$word".
                    Forneça no formato exato abaixo:

                    Classe Gramatical: Tradução (${targetLanguage.displayName})
                    Definição: [Texto traduzido em ${targetLanguage.displayName}]
                    Sinônimos: [Traduções alternativas separadas por vírgula]
                """.trimIndent()
            } else {
                when (targetLanguage) {
                    TranslationLanguage.PORTUGUESE -> """
                        Analise a seguinte palavra ou frase: "$word".
                        Forneça a definição, classe gramatical e sinônimos explicados em Português no formato exato abaixo, sem adicionar nada mais:

                        Classe Gramatical: [Substantivo/Adjetivo/Verbo/etc.]
                        Definição: [A definição e explicação em Português]
                        Sinônimos: [Sinônimo 1, Sinônimo 2, Sinônimo 3]
                    """.trimIndent()

                    TranslationLanguage.ENGLISH -> """
                        Analyze the following word or phrase: "$word".
                        Provide the definition, part of speech, and synonyms explained in English in the exact format below, without adding anything else:

                        Classe Gramatical: [Part of speech in English, e.g., Noun/Adjective/Verb]
                        Definição: [Definition and explanation in English]
                        Sinônimos: [Synonym 1, Synonym 2, Synonym 3]
                    """.trimIndent()

                    TranslationLanguage.SPANISH -> """
                        Analice la siguiente palabra o frase: "$word".
                        Proporcione la definición, clase gramatical y sinónimos explicados en Español en el formato exacto a continuación, sin agregar nada más:

                        Classe Gramatical: [Clase gramatical en Español, p. ej. Sustantivo/Adjetivo/Verbo]
                        Definição: [Definición y explicación en Español]
                        Sinônimos: [Sinónimo 1, Sinónimo 2, Sinónimo 3]
                    """.trimIndent()

                    else -> """
                        Analyze the word or phrase "$word".
                        Provide the definition, part of speech, and synonyms explained in ${targetLanguage.displayName} in the exact format below:

                        Classe Gramatical: [Part of speech in ${targetLanguage.displayName}]
                        Definição: [Definition and explanation in ${targetLanguage.displayName}]
                        Sinônimos: [Synonyms separated by comma]
                    """.trimIndent()
                }
            }

            val systemInstruction = when (targetLanguage) {
                TranslationLanguage.PORTUGUESE -> "Você é um dicionário e tradutor conciso e preciso. Responda apenas no formato solicitado com explicações em Português."
                TranslationLanguage.ENGLISH -> "You are a concise and accurate dictionary and translator. Provide all definitions and explanations in English. Respond only in the requested format."
                TranslationLanguage.SPANISH -> "Eres un diccionario y traductor conciso y preciso. Proporciona todas las definiciones y explicaciones en Español. Responde solo en el formato solicitado."
                else -> "You are a concise dictionary and translator. Provide all definitions and explanations in ${targetLanguage.displayName}. Respond only in the requested format."
            }

            val request = GroqChatRequest(
                model = "llama-3.3-70b-versatile",
                messages = listOf(
                    GroqMessage(role = "system", content = systemInstruction),
                    GroqMessage(role = "user", content = prompt)
                ),
                temperature = 0.2
            )

            val response = RetrofitClient.service.chatCompletions(authHeader, request)
            val responseText = response.choices?.firstOrNull()?.message?.content ?: ""

            if (responseText.isEmpty()) {
                return Result.failure(Exception("Resposta vazia da API."))
            }

            var partOfSpeech = if (isTranslationMode) "Tradução (${targetLanguage.displayName})" else "Desconhecida"
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

            return Result.success(
                DictionaryResult(
                    word = word.trim(),
                    partOfSpeech = partOfSpeech,
                    definition = definition,
                    synonyms = synonyms
                )
            )
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
                return Result.failure(Exception("Muitas requisições (Erro 429). Aguarde um momento e tente novamente."))
            } else {
                return Result.failure(Exception("Erro na API: ${e.code()} - $parsedMessage"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
