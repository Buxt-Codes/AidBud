package com.aidbud.ai

import org.json.JSONObject
import java.lang.StringBuilder

/**
 *
 * @param onResponse A lambda function to call when a complete ModelResponse (Text or PCardJson) is parsed.
 */
class ModelResponseParser(private val onResponse: (ModelResponse) -> Unit) {

    private enum class ParserState {
        IDLE,                   // Waiting for a potential start token
        MATCHING_START_TOKEN,   // Matched a '[' and are checking for 'PCARD]'
        INSIDE_PCARD            // Matched '[PCARD]' and are accumulating content
    }

    private var currentState = ParserState.IDLE
    private val startToken = "[PCARD]"
    private val endToken = "[/PCARD]"

    private var currentModelResponseState: ModelResponse = ModelResponse()
    // Buffer for accumulating the content inside a [PCARD] block
    private val pcardContentBuffer = StringBuilder()
    // Buffer for matching the start token sequence
    private val potentialMatchBuffer = StringBuilder()
    private val currentTextBatchBuffer = StringBuilder()

    /**
     * Processes an incoming chunk of text from the model.
     */
    fun processToken(token: String) {
        currentTextBatchBuffer.clear()
        token.forEach { char ->
            when (currentState) {
                ParserState.IDLE -> processIdle(char)
                ParserState.MATCHING_START_TOKEN -> processMatchingStart(char)
                ParserState.INSIDE_PCARD -> processInsidePCard(char)
            }
        }

        if (currentTextBatchBuffer.isNotEmpty()) {
            currentModelResponseState = currentModelResponseState.copy(
                generatedText = currentModelResponseState.generatedText + currentTextBatchBuffer.toString()
            )
            currentTextBatchBuffer.clear()
            }
        emitCurrentState()
    }

    private fun appendToCurrentTextBatch(text: String) {
        currentTextBatchBuffer.append(text)
    }

    private fun emitCurrentState() {
        onResponse(currentModelResponseState.copy()) // Emit a copy to ensure immutability of the emitted object
    }

    private fun processIdle(char: Char) {
        if (char == startToken.first()) {
            // Before changing state to match a token, ensure any pending text is added to generatedText
            if (currentTextBatchBuffer.isNotEmpty()) {
                currentModelResponseState = currentModelResponseState.copy(
                    generatedText = currentModelResponseState.generatedText + currentTextBatchBuffer.toString()
                )
                currentTextBatchBuffer.clear()
            }
            currentState = ParserState.MATCHING_START_TOKEN
            potentialMatchBuffer.append(char)
        } else {
            appendToCurrentTextBatch(char.toString()) // Accumulate regular text
        }
    }

    private fun processMatchingStart(char: Char) {
        potentialMatchBuffer.append(char)
        val currentMatch = potentialMatchBuffer.toString()

        if (startToken.startsWith(currentMatch)) {
            if (currentMatch == startToken) {
                currentState = ParserState.INSIDE_PCARD
                currentModelResponseState = currentModelResponseState.copy(isPCardActive = true)
                potentialMatchBuffer.clear()
            }
            // Otherwise, just keep accumulating in tokenMatchBuffer.
        } else {
            // It's a false alarm (e.g., "[X..." is not "[PCARD]").
            // Treat the false match as regular text.
            appendToCurrentTextBatch(currentMatch)
            potentialMatchBuffer.clear()
            currentState = ParserState.IDLE
            currentModelResponseState = currentModelResponseState.copy(isPCardActive = false) // Signal PCARD not active
        }
    }

    private fun processInsidePCard(char: Char) {
        pcardContentBuffer.append(char)
        val currentContent = pcardContentBuffer.toString()

        // Check if the buffer now ends with the end token
        if (currentContent.endsWith(endToken)) {
            val payload = currentContent.substring(0, currentContent.length - endToken.length)
            handleCompletedPCardBlock(payload)

            // Reset for the next sequence
            pcardContentBuffer.clear()
            currentState = ParserState.IDLE
            currentModelResponseState = currentModelResponseState.copy(isPCardActive = false)
        }
    }

    private fun handleCompletedPCardBlock(payload: String) {
        val jsonStart = payload.indexOf('{')
        val jsonEnd = payload.lastIndexOf('}')

        // If no valid "{...}" structure is found, do nothing and return early.
        if (jsonStart == -1 || jsonEnd <= jsonStart) {
            return // Early exit: no valid JSON structure found
        }

        // Extract and try to parse the JSON object.
        val jsonString = payload.substring(jsonStart, jsonEnd + 1)
        try {
            val jsonObj = JSONObject(jsonString)
            currentModelResponseState = currentModelResponseState.copy(pCardData = jsonObj)
        } catch (e: Exception) {
            currentModelResponseState = currentModelResponseState.copy(errorMessage = "Error while editing PCard")
            return // Early exit: malformed JSON
        }
    }

    fun completed() {
        currentModelResponseState = currentModelResponseState.copy(isCompleted = true)
        emitCurrentState()
        flush()
    }

    fun flush() {
        potentialMatchBuffer.clear()
        pcardContentBuffer.clear()
        currentTextBatchBuffer.clear()
        currentState = ParserState.IDLE
        currentModelResponseState = ModelResponse()
    }
}