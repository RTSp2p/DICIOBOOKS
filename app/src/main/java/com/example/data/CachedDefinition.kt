package com.example.data

import androidx.room.Entity

@Entity(
    tableName = "dictionary_cache",
    primaryKeys = ["word", "targetLanguage", "searchMode"]
)
data class CachedDefinition(
    val word: String,
    val targetLanguage: String,
    val searchMode: String,
    val partOfSpeech: String,
    val definition: String,
    val synonyms: String,
    val timestamp: Long = System.currentTimeMillis()
)
