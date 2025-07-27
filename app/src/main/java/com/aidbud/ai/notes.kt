package com.aidbud.ai

//class ChatProcessManager(
//    private val repository: YourRepositoryType,
//    private val embedder: YourEmbedderType,
//    private val llmInference: Any, // Your LLM inference engine instance
//    private val processUris: (List<Uri>) -> List<Bitmap> // Utility to process URIs
//) {
//
//    // A CoroutineScope to manage the lifecycle of all chat-related operations
//    // Using SupervisorJob allows child coroutine failures to not cancel the parent scope.
//    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
//
//    // A Job reference to the currently running chat operation (RAG + LLM)
//    private var currentChatOperationJob: Job? = null
//
//    /**
//     * Starts a new RAG and LLM response generation process.
//     * If an existing process is running, it will be cancelled first.
//     */
//    fun startChatProcess(
//        conversationId: Long,
//        query: String,
//        attachments: List<Uri> = emptyList()
//    ) {
//        // Cancel any previous operation to ensure only one is active at a time
//        cancelCurrentChatProcess()
//
//        currentChatOperationJob = managerScope.launch {
//            try {
//                // Step 1: Run RAG
//                println("ChatProcess: Starting RAG operation...")
//                val ragResults = runRAG(repository, embedder, conversationId, query)
//                println("ChatProcess: RAG results: $ragResults")
//
//                // Construct the prompt for LLM, potentially using RAG results
//                val llmPrompt = if (ragResults != null && ragResults.isNotEmpty()) {
//                    "Context: ${ragResults.joinToString("\n")}\n\nQuery: $query"
//                } else {
//                    query
//                }
//
//                // Ensure the coroutine is still active before starting LLM
//                // This check is implicitly done by 'collect' but good for clarity
//                ensureActive()
//
//                // Step 2: Generate LLM Response (collect from the flow)
//                println("ChatProcess: Starting LLM response generation...")
//                generateResponsePCard(llmInference, processUris, llmPrompt, attachments)
//                    .collect { modelResponse ->
//                        // This block receives streaming parts of the LLM response
//                        println("ChatProcess: LLM Partial: ${modelResponse.text}")
//                        // Update your UI with the partial response here
//                    }
//                println("ChatProcess: LLM response generation completed.")
//
//            } catch (e: CancellationException) {
//                // This is caught when `cancelCurrentChatProcess()` is called
//                println("ChatProcess: Operation was cancelled.")
//                // You might update UI to show "Response cancelled"
//            } catch (e: Exception) {
//                // Catch any other errors that occur during the process
//                println("ChatProcess: Operation failed: ${e.message}")
//                e.printStackTrace()
//                // Update UI to show an error message
//            } finally {
//                // Always clear the job reference when the operation finishes or is cancelled
//                currentChatOperationJob = null
//                println("ChatProcess: Job reference cleared.")
//            }
//        }
//    }
//
//    /**
//     * Cancels the currently running RAG and LLM response generation process.
//     * This is the function you'll call from your "Pause" or "Stop" button.
//     */
//    fun cancelCurrentChatProcess() {
//        currentChatOperationJob?.cancel() // Calls cancel() on the Job
//        println("ChatProcess: Sent cancellation signal to current operation.")
//    }
//
//    /**
//     * Should be called when the component owning this manager is destroyed (e.g., ViewModel onCleared).
//     * This cancels all coroutines launched within this manager's scope.
//     */
//    fun shutdown() {
//        managerScope.cancel() // Cancels the entire scope and all its children
//        println("ChatProcess: Manager scope shut down.")
//    }
//}