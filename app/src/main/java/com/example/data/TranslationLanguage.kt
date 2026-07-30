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
    SPANISH("es", "Espanhol", "🇪🇸", Locale("es", "ES"));

    companion object {
        fun fromCode(code: String?): TranslationLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) }
                ?: PORTUGUESE
        }
    }
}
