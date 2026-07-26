package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val partOfSpeech: String,
    val definition: String,
    val synonyms: String,
    val timestamp: Long = System.currentTimeMillis()
)
