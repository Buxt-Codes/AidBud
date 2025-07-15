package com.aidbud.data.viewmodel

/**
 * Represents the different states of an LLM inference stream.
 */
sealed class LLMResponseState {
    /** Indicates that the LLM inference has started. */
    object Loading : LLMResponseState()

    /**
     * Represents a new chunk of text received from the LLM.
     * @param text The partial text received.
     */
    data class Chunk(val text: String) : LLMResponseState()

    /** Indicates that the LLM inference has completed successfully. */
    object Complete : LLMResponseState()

    /**
     * Indicates that an error occurred during LLM inference.
     * @param message A descriptive error message.
     */
    data class Error(val message: String) : LLMResponseState()
}