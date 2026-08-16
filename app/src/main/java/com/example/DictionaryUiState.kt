package com.example

import com.example.data.DictionaryResult

sealed class DictionaryUiState {
    object Idle : DictionaryUiState()
    object Loading : DictionaryUiState()
    data class Success(val result: DictionaryResult) : DictionaryUiState()
    data class Error(val message: String) : DictionaryUiState()
}
