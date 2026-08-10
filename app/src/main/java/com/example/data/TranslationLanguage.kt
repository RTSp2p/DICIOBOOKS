package com.example.data

import java.util.Locale

enum class TranslationLanguage(
    val code: String,
    val displayName: String,
    val flag: String,
    val locale: Locale
) {
    PORTUGUESE("pt", "Português", "🇧🇷", Locale("pt", "BR")),
    ENGLISH("en", "Inglês", "🇺🇸", Locale.US),
    SPANISH("es", "Espanhol", "🇪🇸", Locale("es", "ES")),
    FRENCH("fr", "Francês", "🇫🇷", Locale.FRANCE),
    GERMAN("de", "Alemão", "🇩🇪", Locale.GERMANY),
    ITALIAN("it", "Italiano", "🇮🇹", Locale.ITALY),
    JAPANESE("ja", "Japonês", "🇯🇵", Locale.JAPAN),
    KOREAN("ko", "Coreano", "🇰🇷", Locale.KOREA),
    CHINESE("zh", "Chinês (Simplificado)", "🇨🇳", Locale.SIMPLIFIED_CHINESE),
    RUSSIAN("ru", "Russo", "🇷🇺", Locale("ru", "RU")),
    DUTCH("nl", "Holandês", "🇳🇱", Locale("nl", "NL"));

    companion object {
        fun fromCode(code: String?): TranslationLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: PORTUGUESE
        }
    }
}

enum class SearchMode(
    val displayName: String,
    val description: String
) {
    MEANING("Exibir Significado", "Exibe a classe gramatical, a definição detalhada e os sinônimos da palavra no dicionário."),
    TRANSLATION("Traduzir Texto", "Traduz a palavra ou frase diretamente para o seu idioma nativo usando o Gemini AI.");

    companion object {
        fun fromName(name: String?): SearchMode {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: MEANING
        }
    }
}
