package com.example.data

import com.example.BuildConfig
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
    suspend fun getDefinitionAndSynonyms(word: String): Result<DictionaryResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = try {
                val groqKey = BuildConfig.GROQ_API_KEY
                if (groqKey.isNotBlank() && groqKey != "MY_GROQ_API_KEY") groqKey else BuildConfig.GEMINI_API_KEY
            } catch (_: Exception) {
                BuildConfig.GEMINI_API_KEY
            }

            if (apiKey.isBlank() || apiKey == "MY_GROQ_API_KEY" || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("Chave da API do Groq não configurada. Adicione sua chave do Groq nas configurações."))
            }

            val authHeader = if (apiKey.startsWith("Bearer ", ignoreCase = true)) apiKey else "Bearer $apiKey"

            val prompt = """
                Analise a seguinte palavra ou frase em português: "$word".
                Forneça o significado e sinônimos no formato exato abaixo, sem adicionar mais nada.
                
                Classe Gramatical: [Substantivo/Adjetivo/Verbo/etc.]
                Definição: [A definição mais comum e contextualizada]
                Sinônimos: [Sinônimo 1, Sinônimo 2, Sinônimo 3]
            """.trimIndent()

            val request = GroqChatRequest(
                model = "llama-3.3-70b-versatile",
                messages = listOf(
                    GroqMessage(
                        role = "system",
                        content = "Você é um dicionário conciso e preciso. Responda apenas no formato solicitado."
                    ),
                    GroqMessage(
                        role = "user",
                        content = prompt
                    )
                ),
                temperature = 0.2
            )

            val response = RetrofitClient.service.chatCompletions(authHeader, request)
            
            val responseText = response.choices?.firstOrNull()?.message?.content ?: ""
            if (responseText.isEmpty()) {
                return@withContext Result.failure(Exception("Resposta vazia da API do Groq."))
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
                return@withContext Result.failure(Exception("Muitas requisições (Erro 429). Aguarde um momento e tente novamente."))
            } else {
                return@withContext Result.failure(Exception("Erro na API do Groq: ${e.code()} - $parsedMessage"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}
