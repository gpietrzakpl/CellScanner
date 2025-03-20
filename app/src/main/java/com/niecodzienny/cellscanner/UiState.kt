package com.niecodzienny.cellscanner

sealed class UiState {
    object NoPermission : UiState()
    object Scanning : UiState()
    data class CodeScanned(val code: String, val decodedInfo: Map<String, String>?) : UiState()
    object Idle : UiState()
    object Error : UiState()
}