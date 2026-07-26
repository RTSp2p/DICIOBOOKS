package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val definition: String,
    val timestamp: Long = System.currentTimeMillis()
)
