package com.aidbud.ui.components.cameraoverlay.attachmentbar

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.* // Using Material 3 components
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // Import for graphicsLayer
import androidx.compose.ui.graphics.BlurEffect // Import for BlurEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview // Import for @Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage // For loading images from Uri
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll

import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.ui.components.cameraoverlay.camerabar.FlashMode

@Composable
fun AttachmentBar(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    cacheViewModel: GlobalCacheViewModel,
    isRecording: Boolean,
    attachments: List<Uri>,
    onSendClick: () -> Unit,
    onPlusClick: (Int) -> Unit,
    onAttachmentDeleteClick: (Uri) -> Unit,
    cornerRadius: Dp = 50.dp, // Default corner radius
    blurRadius: Float = 50f // Default blur radius
) {
    val context = LocalContext.current
    val MAX_ATTACHMENTS = 4
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .widthIn(min = 310.dp, max = 400.dp) // Minimum and maximum width constraints
            .fillMaxWidth(0.9f) // Fills 30% of the parent's width, clamped by widthIn
            .height(100.dp) // Fixed height
            .clip(RoundedCornerShape(cornerRadius)) // Apply rounded corners to the entire bar
            .border(0.5.dp, Color.White, RoundedCornerShape(cornerRadius)), // Add the white border
        contentAlignment = Alignment.Center // Centers the content (Row) horizontally and vertically
    ) {
        // Inner Box for the background and blur effect
        // This ensures only the background is blurred, not the content placed on top of it.
        Box(
            modifier = Modifier
                .fillMaxSize() // Fill the size of the parent Box (AttachmentBar)
                .clip(RoundedCornerShape(cornerRadius)) // Apply rounded corners to the background
                .background(Color.Gray.copy(alpha = 0.5f)) // Semi-transparent background
                .graphicsLayer {
                    renderEffect = BlurEffect(radiusX = blurRadius, radiusY = blurRadius) // Apply blur here
                }
        )

        // Content Box to hold the Row of buttons and thumbnails, centered on top of the blurred background
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                // Plus Button (Far Left)
                PlusButton(
                    onClick = {
                        val currentAttachmentCount = attachments.size
                        if (currentAttachmentCount >= 4) {
                            Toast.makeText(context, "Maximum 4 images allowed", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            onPlusClick(currentAttachmentCount) // Pass current count to parent
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Attachment Thumbnails Row (Takes up remaining space)
                Box( // This Box takes the weighted space in the main Row
                    modifier = Modifier
                        .weight(1f) // Takes all available horizontal space in the parent Row
                        .fillMaxHeight(0.8f) // Fills the height of the parent Row
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight() // Respects the height of its parent Box
                            .horizontalScroll(scrollState), // Makes the Row horizontally scrollable
                        horizontalArrangement = Arrangement.spacedBy(8.dp), // Spacing between slots
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Only iterate over actual attachments
                        // Use .take(MAX_ATTACHMENTS) if you still want to limit how many are displayed visually
                        attachments.forEach { uri ->
                            AttachmentThumbnail(
                                modifier = Modifier
                                    .fillMaxHeight(), // Fills height, which will then determine width via aspectRatio(1f)
                                uri = uri,
                                onDelete = onAttachmentDeleteClick
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Send Button (Far Right)
                SendButton(
                    onClick = onSendClick
                )
            }
        }
    }
}