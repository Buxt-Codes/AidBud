package com.aidbud.ai

import org.json.JSONObject

data class ModelResponse(
    val generatedText: String = "",
    val pCardData: JSONObject? = null,
    val isPCardActive: Boolean = false,
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)
