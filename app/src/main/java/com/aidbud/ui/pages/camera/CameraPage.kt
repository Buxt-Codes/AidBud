package com.aidbud.ui.pages.camera

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.settings.SettingsDataStore
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.ui.theme.AidBudTheme
import com.aidbud.data.viewmodel.MainViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.runtime.derivedStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.fadeIn // Import fadeIn
import androidx.compose.animation.fadeOut // Import fadeOut

import com.aidbud.ui.components.cameraoverlay.CameraOverlay
import com.aidbud.ui.components.cameraoverlay.attachmentbar.AttachmentBar
import com.aidbud.util.attachments.getVideoDuration
import com.aidbud.data.cache.draftmessage.DraftMessage
import com.aidbud.ui.components.cameraoverlay.headerbar.HeaderBar
import androidx.compose.animation.AnimatedVisibility

/**
 * Composable screen for the Camera functionality.
 * This is a placeholder for actual camera implementation.
 * It demonstrates how to receive ViewModels and interact with them.
 *
 * @param conversationId The ID of the conversation to associate actions with.
 * @param navController NavController for navigation actions.
 * @param viewModel The AidBudViewModel, passed from AppNavHost, for data operations.
 * @param settingsViewModel The SettingsViewModel, passed from AppNavHost, for settings.
 * @param cacheViewModel The GlobalCacheViewModel, passed from AppNavHost, for caching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPage(
    conversationId: Long,
    navController: NavController,
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    cacheViewModel: GlobalCacheViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Correct way to get the current draft: directly call the getter from the ViewModel.
    // Compose will automatically recompose when `cacheViewModel.drafts` changes for this conversationId.
    val currentDraft by remember(conversationId) { // Re-evaluate only if conversationId changes
        derivedStateOf {
            // This lambda will be re-executed if cacheViewModel.drafts changes
            // or if any observable property (like attachments.size or totalVideoDurationMillis)
            // within the DraftMessage at this conversationId key changes.
            cacheViewModel.drafts[conversationId] ?: DraftMessage()
        }
    }

    val currentConversation by remember(conversationId) {
        viewModel.getConversationById(conversationId)
    }.collectAsStateWithLifecycle(initialValue = null)

    // Local UI state for recording status (not managed by cacheViewModel as it's ephemeral)
    var isRecording by remember { mutableStateOf(false) }


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

    // Launcher for picking a single item (when only 1 slot is left)
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
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // CameraOverlay at the bottom layer
            CameraOverlay(
                modifier = Modifier.fillMaxSize(),
                conversationId = conversationId,
                settingsViewModel = settingsViewModel,
                cacheViewModel = cacheViewModel,
                onPhotoTaken = { uri ->
                    // Add photo URI to the draft in cacheViewModel
                    cacheViewModel.addAttachment(conversationId, uri)
                    println("Photo captured and added to draft for conversation $conversationId: $uri")
                },
                onVideoTaken = { uri ->
                    // Add video URI to the draft in cacheViewModel
                    cacheViewModel.addAttachment(conversationId, uri)
                    isRecording = false // Assuming video taken means recording stops
                    println("Video captured and added to draft for conversation $conversationId: $uri")
                },
                onStartRecording = { isRecording = true },
                onStopRecording = { isRecording = false }
            )

            // AttachmentBar above CameraOverlay, 150dp from bottom center
            AnimatedVisibility( // Wrap AttachmentBar with AnimatedVisibility
                visible = !isRecording, // Visible when not recording
                enter = fadeIn(), // Fade in when becoming visible
                exit = fadeOut(), // Fade out when becoming invisible
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            ) {
                AttachmentBar(
                    settingsViewModel = settingsViewModel,
                    cacheViewModel = cacheViewModel,
                    attachments = currentDraft.attachments,
                    onSendClick = {
                        coroutineScope.launch {
                            if (conversationId != -1L) {
                                viewModel.insertMessage(
                                    conversationId = conversationId,
                                    role = "user",
                                    text = currentDraft.text.ifBlank { null },
                                    photosAndVideos = currentDraft.attachments.ifEmpty { null }
                                )
                                println("Message with attachments sent for conversation $conversationId: ${currentDraft.attachments.size} attachments")
                                cacheViewModel.clearDraft(conversationId)
                                println("Draft cleared for conversation $conversationId")
                            } else {
                                println("Error: Invalid conversation ID to send message to.")
                            }
                        }
                    },
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
                    },
                    onAttachmentDeleteClick = { uriToDelete ->
                        cacheViewModel.removeAttachment(conversationId, uriToDelete)
                        println("Attachment deleted from draft for conversation $conversationId: $uriToDelete")
                    }
                )
            }

            // HeaderBar above CameraOverlay, 16dp from top center
            AnimatedVisibility( // Wrap HeaderBar with AnimatedVisibility
                visible = !isRecording, // Visible when not recording
                enter = fadeIn(), // Fade in when becoming visible
                exit = fadeOut(), // Fade out when becoming invisible
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                HeaderBar(
                    conversationId = conversationId,
                    navController = navController,
                    title = currentConversation?.title ?: "Loading Conversation...",
                )
            }

        }
    }
}