package com.aidbud.ai.llm

import org.json.JSONObject

data class ModelResponse(
    val generatedText: String = "",
    val pCardData: JSONObject? = null,
    val isPCardActive: Boolean = false,
    val functionData: JSONObject? = null,
    val isFunctionCall: Boolean = false,
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)
