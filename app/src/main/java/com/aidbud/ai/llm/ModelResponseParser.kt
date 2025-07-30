package com.aidbud.ai.llm

import org.json.JSONObject
import java.lang.StringBuilder

/**
 *
 * @param onResponse A lambda function to call when a complete ModelResponse (Text or PCardJson) is parsed.
 */
class ModelResponseParser(private val onResponse: (ModelResponse) -> Unit) {

    // --- Private Enums for State Management ---

    /** The overall strategy for parsing, determined during the initial buffering phase. */
    private enum class ParsingStrategy {
        /** Buffering the first few tokens to determine intent. */
        BUFFERING,
        /** Streaming text and handling inline [PCARD]s. */
        STREAMING,
        /** A function call was detected; capture the arguments and stop. */
        FUNCTION_CALL
    }

    /** The granular, character-by-character state of the parser. */
    private enum class InternalState {
        IDLE,
        MATCHING_TAG,
        INSIDE_PCARD,
        INSIDE_FCALL
    }

    // --- Constants ---
    companion object {
        private const val FCALL_START = "[FCALL]"
        private const val FCALL_END = "[/FCALL]"
        private const val PCARD_START = "[PCARD]"
        private const val PCARD_END = "[/PCARD]"
        private const val BUFFER_SIZE_LIMIT = 60 // Max tokens to buffer before deciding to stream.
    }

    // --- State Variables ---
    private var strategy = ParsingStrategy.BUFFERING
    private var internalState = InternalState.IDLE
    private var currentModelResponse = ModelResponse(isLoading = true)

    // --- Buffers ---
    private val shortBuffer = StringBuilder()           // For the initial ~60 tokens.
    private val contentBuffer = StringBuilder()         // For content inside [PCARD] or [FCALL].
    private val potentialTagBuffer = StringBuilder()    // For matching start/end tags.
    private val textBatchBuffer = StringBuilder()       // Accumulates plain text before emitting.

    /**
     * Processes an incoming token from the model stream.
     * This is the main entry point for the parser.
     */
    fun processToken(token: String) {
        if (currentModelResponse.isCompleted || (currentModelResponse.isFunctionCall && currentModelResponse.functionData != null)) {
            emitCurrentState()
        }

        when (strategy) {
            ParsingStrategy.BUFFERING -> processBuffering(token)
            ParsingStrategy.STREAMING -> processStreaming(token)
            ParsingStrategy.FUNCTION_CALL -> processStreaming(token) // Same logic as streaming, but looks for FCALL_END
        }

        // Emit any accumulated text after processing the token.
        flushTextBatch()
        emitCurrentState()
    }

    /**
     * Handles tokens during the initial buffering phase to determine the model's intent.
     */
    private fun processBuffering(token: String) {
        shortBuffer.append(token)
        val bufferedString = shortBuffer.toString()

        when {
            // Case A: [FCALL] is found. Switch to function call mode and stop streaming.
            bufferedString.contains(FCALL_START) -> {
                strategy = ParsingStrategy.FUNCTION_CALL
                // Re-process the entire buffer in the new mode.
                processStreaming(bufferedString)
                shortBuffer.clear()
            }
            // Case B: [PCARD] is found. Start streaming the response.
            bufferedString.contains(PCARD_START) -> {
                strategy = ParsingStrategy.STREAMING
                // Re-process the buffer.
                processStreaming(bufferedString)
                shortBuffer.clear()
            }
            // Case C: Buffer is full, no special tags found. Assume normal response.
            shortBuffer.length > BUFFER_SIZE_LIMIT -> {
                strategy = ParsingStrategy.STREAMING
                val contentToProcess = bufferedString
                shortBuffer.clear()
                processStreaming(contentToProcess)
            }
        }
    }

    /**
     * Processes tokens character-by-character based on the current internal state.
     * This handles both STREAMING and FUNCTION_CALL strategies.
     */
    private fun processStreaming(text: String) {
        text.forEach { char ->
            when (internalState) {
                InternalState.IDLE -> processIdleChar(char)
                InternalState.MATCHING_TAG -> processMatchingTagChar(char)
                InternalState.INSIDE_PCARD -> processInsidePCardChar(char)
                InternalState.INSIDE_FCALL -> processInsideFCallChar(char)
            }
        }
    }

    private fun processIdleChar(char: Char) {
        if (char == '[') {
            flushTextBatch() // Emit any text we've seen before the tag starts.
            internalState = InternalState.MATCHING_TAG
            potentialTagBuffer.append(char)
        } else {
            // Only accumulate text if a function call has not been detected.
            if (!currentModelResponse.isFunctionCall) {
                textBatchBuffer.append(char)
            }
        }
    }

    private fun processMatchingTagChar(char: Char) {
        potentialTagBuffer.append(char)
        val currentTag = potentialTagBuffer.toString()

        when {
            currentTag == PCARD_START -> {
                internalState = InternalState.INSIDE_PCARD
                currentModelResponse = currentModelResponse.copy(isPCardActive = true)
                potentialTagBuffer.clear()
            }
            currentTag == FCALL_START -> {
                // A function call can be detected at any point.
                // If detected, we stop all text generation and switch modes.
                strategy = ParsingStrategy.FUNCTION_CALL
                internalState = InternalState.INSIDE_FCALL

                // Retract any previously generated text and clear pending text.
                currentModelResponse = currentModelResponse.copy(isFunctionCall = true, generatedText = "")
                textBatchBuffer.clear()

                potentialTagBuffer.clear()
            }
            // If it's not a prefix of a known tag, it's a false alarm.
            !PCARD_START.startsWith(currentTag) && !FCALL_START.startsWith(currentTag) -> {
                // Treat the buffered tag characters as plain text, but only if we are not in a function call.
                if (!currentModelResponse.isFunctionCall) {
                    textBatchBuffer.append(currentTag)
                }
                potentialTagBuffer.clear()
                internalState = InternalState.IDLE
            }
        }
    }

    private fun processInsidePCardChar(char: Char) {
        contentBuffer.append(char)
        if (contentBuffer.toString().endsWith(PCARD_END)) {
            val payload = contentBuffer.substring(0, contentBuffer.length - PCARD_END.length)
            handleCompletedBlock(payload, isPCard = true)
            contentBuffer.clear()
            internalState = InternalState.IDLE
            currentModelResponse = currentModelResponse.copy(isPCardActive = false)
        }
    }

    private fun processInsideFCallChar(char: Char) {
        contentBuffer.append(char)
        if (contentBuffer.toString().endsWith(FCALL_END)) {
            val payload = contentBuffer.substring(0, contentBuffer.length - FCALL_END.length)
            handleCompletedBlock(payload, isPCard = false)
            contentBuffer.clear()
            internalState = InternalState.IDLE
            // Do not change state further; wait for completion signal.
        }
    }

    /**
     * Parses the JSON from a completed block and updates the model state.
     */
    private fun handleCompletedBlock(payload: String, isPCard: Boolean) {
        try {
            val jsonObj = JSONObject(payload.trim())
            currentModelResponse = if (isPCard) {
                currentModelResponse.copy(pCardData = jsonObj)
            } else {
                currentModelResponse.copy(functionData = jsonObj)
            }
        } catch (e: Exception) {
            val errorMsg = if (isPCard) "Error parsing PCARD JSON" else "Error parsing FCALL JSON"
            currentModelResponse = currentModelResponse.copy(errorMessage = "$errorMsg: ${e.message}")
        }
    }

    /**
     * Sends any accumulated plain text to the response.
     */
    private fun flushTextBatch() {
        // Only flush text if the buffer is not empty AND a function call hasn't been detected.
        if (textBatchBuffer.isNotEmpty() && !currentModelResponse.isFunctionCall) {
            currentModelResponse = currentModelResponse.copy(
                generatedText = currentModelResponse.generatedText + textBatchBuffer.toString()
            )
        }
        // Always clear the batch buffer after flushing or deciding not to flush.
        textBatchBuffer.clear()
    }

    /**
     * Called when the model signals the end of the entire stream.
     */
    fun complete() {
        // If we are still buffering when completion is called, it means no tags were found.
        if (shortBuffer.isNotEmpty()) {
            strategy = ParsingStrategy.STREAMING
            processStreaming(shortBuffer.toString())
            shortBuffer.clear()
        }
        flushTextBatch() // Ensure any final text is flushed.
        currentModelResponse = currentModelResponse.copy(isCompleted = true, isLoading = false)
        emitCurrentState()
    }

    /**
     * Resets the parser to its initial state for a new stream.
     */
    fun reset() {
        strategy = ParsingStrategy.BUFFERING
        internalState = InternalState.IDLE
        shortBuffer.clear()
        contentBuffer.clear()
        potentialTagBuffer.clear()
        textBatchBuffer.clear()
        currentModelResponse = ModelResponse(isLoading = true)
    }

    private fun emitCurrentState() {
        onResponse(currentModelResponse)
    }
}