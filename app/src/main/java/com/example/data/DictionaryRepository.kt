package com.example.data

import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
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
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("API Key not configured."))
            }

            val prompt = """
                Analise a seguinte palavra ou frase em português: "$word".
                Forneça o significado e sinônimos no formato exato abaixo, sem adicionar mais nada.
                
                Classe Gramatical: [Substantivo/Adjetivo/Verbo/etc.]
                Definição: [A definição mais comum e contextualizada]
                Sinônimos: [Sinônimo 1, Sinônimo 2, Sinônimo 3]
            """.trimIndent()

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

            Result.success(DictionaryResult(
                word = word.trim(),
                partOfSpeech = partOfSpeech,
                definition = definition,
                synonyms = synonyms
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
