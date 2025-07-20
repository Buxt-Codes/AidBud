package com.aidbud.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.nlp.llminference.LlmInference
import com.google.mediapipe.tasks.nlp.llminference.LlmInference.LlmInferenceOptions
import java.io.IOException

/**
 * A class to handle loading and running inference with a Gemma Nano TFLite model
 * using the MediaPipe Tasks Library for LLM Inference.
 *
 * This simplifies the process by handling tokenization, output parsing, and
 * internal interpreter management.
 */
class GemmaNanoModel(private val context: Context, private val modelPath: String) {

    private var llmInference: LlmInference? = null
    private val TAG = "GemmaNanoModel"

    /**
     * Initializes the MediaPipe LlmInference with the Gemma Nano model.
     * This handles model loading and setup internally.
     */
    fun initialize() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelPath) // Path to your .tflite model in assets
                .build()

            val options = LlmInference.LlmInferenceOptions.builder()
                .setBaseOptions(baseOptions)
                // You can add more options here if available for your specific model
                // For example, some models might allow setting max tokens, temperature, etc.
                .build()

            llmInference = LlmInference.createFromFile(context, options)
            Log.i(TAG, "Gemma Nano model loaded successfully using MediaPipe LlmInference.")

        } catch (e: IOException) {
            Log.e(TAG, "Failed to load TFLite model via MediaPipe LlmInference: $modelPath", e)
            llmInference = null
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaPipe LlmInference: ${e.message}", e)
            llmInference = null
        }
    }

    /**
     * Generates a response from the LLM based on the given prompt.
     * This method uses MediaPipe's streaming capabilities.
     *
     * @param prompt The user's input prompt.
     * @param onResultListener A callback to receive streaming text chunks.
     * @param onFinishListener A callback invoked when generation is complete.
     * @param onErrorListener A callback invoked if an error occurs during generation.
     */
    fun generateResponse(
        prompt: String,
        onResultListener: (String) -> Unit,
        onFinishListener: () -> Unit,
        onErrorListener: (String) -> Unit
    ) {
        if (llmInference == null) {
            val errorMessage = "LlmInference not initialized. Cannot generate response."
            Log.e(TAG, errorMessage)
            onErrorListener(errorMessage)
            return
        }

        try {
            llmInference?.generateResponse(
                prompt,
                // Result listener for streaming chunks
                { partialResult ->
                    onResultListener(partialResult)
                },
                // Finish listener
                {
                    onFinishListener()
                }
            )
            Log.i(TAG, "LLM response generation started for prompt: '$prompt'")
        } catch (e: Exception) {
            val errorMessage = "Error generating LLM response: ${e.message}"
            Log.e(TAG, errorMessage, e)
            onErrorListener(errorMessage)
        }
    }

    /**
     * Releases the MediaPipe LlmInference resources.
     * Call this when the ViewModel or Activity is destroyed to prevent memory leaks.
     */
    fun close() {
        llmInference?.close()
        llmInference = null
        Log.i(TAG, "MediaPipe LlmInference closed.")
    }
}