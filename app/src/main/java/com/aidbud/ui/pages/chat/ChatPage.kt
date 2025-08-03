package com.aidbud.ui.pages.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aidbud.data.cache.draftmessage.DraftMessage
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.conversation.Conversation
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.data.viewmodel.MainViewModel
import com.aidbud.ui.components.chat.MessageBox
import com.aidbud.ui.components.chat.MessageRole
import com.aidbud.ui.components.chat.headerbar.HeaderBar
import com.aidbud.ui.components.chat.inputbar.InputBar
import com.aidbud.util.attachments.getVideoDuration

@Composable
fun ChatPage(
    conversationId: Long,
    navController: NavController,
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    cacheViewModel: GlobalCacheViewModel
) {
    val context = LocalContext.current
    val currentDraft by remember(conversationId) { // Re-evaluate only if conversationId changes
        derivedStateOf {
            cacheViewModel.drafts[conversationId] ?: DraftMessage()
        }
    }

    val currentConversation by remember(conversationId) {
        viewModel.getConversationById(conversationId)
    }.collectAsStateWithLifecycle(initialValue = Conversation(conversationId, "AidBud", System.currentTimeMillis()))

    val messages by viewModel.getMessagesForConversation(conversationId)
        .collectAsState(initial = emptyList())

    val llmState by viewModel.llmState.collectAsState()
    val listState = rememberLazyListState()

    val onSendClick = {
        val messageText = currentDraft.text.trim()
        val attachments = currentDraft.attachments.toList()

        if (messageText.isNotBlank() || attachments.isNotEmpty()) {
            viewModel.insertMessage(
                conversationId = conversationId,
                role = "USER",
                text = messageText,
                attachments = attachments
            )

            viewModel.runLLM(
                query = messageText,
                attachments = attachments,
                conversationId = conversationId
            )

            currentDraft.text = ""
            currentDraft.attachments.clear()
        }
    }

    val pickMultipleMediaLauncher = rememberLauncherForActivityResult(
        // Only create PickMultipleVisualMedia if we can pick more than 1
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = (4 - currentDraft.attachments.size).coerceAtLeast(2) // Ensure maxItems is at least 2 for this contract
        ),
        onResult = { uris: List<Uri> ->
            if (uris.isEmpty()) {
                return@rememberLauncherForActivityResult
            }

            var addedCount = 0
            val strictRemainingSlots = 4 - currentDraft.attachments.size // Re-calculate for strict filtering

            for (uri in uris) {
                if (addedCount >= strictRemainingSlots) {
                    Toast.makeText(context, "Maximum 4 attachments reached.", Toast.LENGTH_SHORT).show()
                    break
                }

                val mimeType = context.contentResolver.getType(uri)
                val isVideo = mimeType?.startsWith("video/") == true

                if (isVideo) {
                    val videoDuration = getVideoDuration(context, uri)
                    val durationLeft = cacheViewModel.getDurationLeft(conversationId)

                    if (videoDuration > durationLeft) {
                        Toast.makeText(
                            context,
                            "Video is too long or exceeds remaining duration (${durationLeft / 1000}s left).",
                            Toast.LENGTH_LONG
                        ).show()
                        continue
                    }

                    cacheViewModel.addAttachment(conversationId, uri)
                    addedCount++
                } else {
                    cacheViewModel.addAttachment(conversationId, uri)
                    addedCount++
                }
            }
        }
    )

    val pickSingleMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(), // No maxItems for single pick
        onResult = { uri: Uri? ->
            uri?.let {
                if (currentDraft.attachments.size >= 4) { // Should not happen due to plus button logic, but for safety
                    Toast.makeText(context, "Maximum 4 attachments reached.", Toast.LENGTH_SHORT).show()
                    return@rememberLauncherForActivityResult
                }

                val mimeType = context.contentResolver.getType(it)
                val isVideo = mimeType?.startsWith("video/") == true

                if (isVideo) {
                    val videoDuration = getVideoDuration(context, it)
                    val durationLeft = cacheViewModel.getDurationLeft(conversationId)

                    if (videoDuration > durationLeft) {
                        Toast.makeText(
                            context,
                            "Video is too long or exceeds remaining duration (${durationLeft / 1000}s left).",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        cacheViewModel.addAttachment(conversationId, uri)
                    }
                } else {
                    cacheViewModel.addAttachment(conversationId, uri)
                }
            }
        }
    )


    Scaffold(
        topBar = {
            HeaderBar(
                conversationId = conversationId,
                navController = navController,
                title = currentConversation?.title ?: "AidBud",
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp),
                state = listState
            ) {
                items(messages.sortedBy { it.timestamp }) { message ->
                    message.text?.let {
                        MessageBox(
                            text = it,
                            role = when (message.role) {
                                "USER" -> MessageRole.User
                                else -> MessageRole.LLM
                            },
                            attachments = message.attachments
                        )
                    }
                }
                if (llmState.isLoading) {
                    val loadingText = when {
                        llmState.isFunctionCall -> "Calling Function"
                        llmState.isPCardActive -> "Editing Patient Card"
                        llmState.generatedText.isEmpty() -> "Analysing"
                        else -> ""
                    }
                    item {
                        MessageBox(
                            text = llmState.generatedText,
                            role = MessageRole.LLM,
                            isLoading = when {
                                llmState.isFunctionCall -> true
                                llmState.isPCardActive -> true
                                llmState.generatedText.isEmpty() -> true
                                else -> false
                            },
                            isLoadingText = loadingText
                        )
                    }
                }
            }

            InputBar(
                attachments = currentDraft.attachments,
                onDeleteAttachment = { uri ->
                    currentDraft.attachments.remove(uri)
                },
                text = currentDraft.text,
                onTextChange = { newText ->
                    currentDraft.text = newText
                },
                onSendClick = onSendClick,
                onPlusClick = {
                    if (currentDraft.attachments.size < 4) {
                        val currentAttachmentCount = currentDraft.attachments.size
                        println("Current num attachments: $currentAttachmentCount")
                        if (currentDraft.attachments.size == 3) {
                            println("Launch Single Media")
                            pickSingleMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        } else {
                            println("Launch Multiple Media")
                            pickMultipleMediaLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    }
                }
            )
        }
    }
}
