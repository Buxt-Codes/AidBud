package com.aidbud.ui.pages.camera

import android.net.Uri
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

import com.aidbud.ui.components.cameraoverlay.CameraOverlay
import com.aidbud.ui.components.cameraoverlay.attachmentbar.AttachmentBar

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
    val coroutineScope = rememberCoroutineScope()

    // Correct way to get the current draft: directly call the getter from the ViewModel.
    // Compose will automatically recompose when `cacheViewModel.drafts` changes for this conversationId.
    val currentDraft = cacheViewModel.getDraft(conversationId.toInt())

    // Local UI state for recording status (not managed by cacheViewModel as it's ephemeral)
    var isRecording by remember { mutableStateOf(false) }

    // Observe settings if needed (e.g., to conditionally enable/disable features)
    val saveInAlbumSetting by settingsViewModel.saveInAlbum.collectAsState(initial = false)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Camera") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
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
                    cacheViewModel.addAttachment(conversationId.toInt(), uri)
                    println("Photo captured and added to draft for conversation $conversationId: $uri")
                },
                onVideoTaken = { uri ->
                    // Add video URI to the draft in cacheViewModel
                    cacheViewModel.addAttachment(conversationId.toInt(), uri)
                    isRecording = false // Assuming video taken means recording stops
                    println("Video captured and added to draft for conversation $conversationId: $uri")
                },
                onStartRecording = { isRecording = true },
                onStopRecording = { isRecording = false }
            )

            // AttachmentBar above CameraOverlay, 150dp from bottom center
            AttachmentBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp),
                settingsViewModel = settingsViewModel,
                cacheViewModel = cacheViewModel,
                isRecording = isRecording,
                attachments = currentDraft.attachments, // Display attachments from cacheViewModel
                onSendClick = {
                    // Handle send action: insert message with attachments into database
                    coroutineScope.launch {
                        if (conversationId != -1L) {
                            viewModel.insertMessage(
                                conversationId = conversationId,
                                role = "user",
                                text = currentDraft.text.ifBlank { null }, // Use cached text, or null if empty
                                photosAndVideos = currentDraft.attachments.ifEmpty { null }
                            )
                            println("Message with attachments sent for conversation $conversationId: ${currentDraft.attachments.size} attachments")

                            // Clear the draft after sending
                            cacheViewModel.clearDraft(conversationId.toInt())
                            println("Draft cleared for conversation $conversationId")

                            // Example of using settingsViewModel
                            if (saveInAlbumSetting) {
                                println("Setting 'Save In Album' is ON. (Simulated save to album)")
                            } else {
                                println("Setting 'Save In Album' is OFF. (Simulated no save to album)")
                            }
                        } else {
                            println("Error: Invalid conversation ID to send message to.")
                        }
                    }
                },
                onPlusClick = {
                    // Handle plus button click (e.g., open gallery)
                    println("Plus button clicked for conversation $conversationId")
                },
                onAttachmentDeleteClick = { uriToDelete ->
                    // Handle attachment deletion from the draft in cacheViewModel
                    cacheViewModel.removeAttachment(conversationId.toInt(), uriToDelete)
                    println("Attachment deleted from draft for conversation $conversationId: $uriToDelete")
                }
            )
        }
    }
}