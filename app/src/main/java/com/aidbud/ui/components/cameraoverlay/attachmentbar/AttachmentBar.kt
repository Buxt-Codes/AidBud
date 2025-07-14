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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage // For loading images from Uri

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
    onPlusClick: () -> Unit,
    onAttachmentDeleteClick: (Uri) -> Unit,
    cornerRadius: Dp = 16.dp, // Default corner radius
    blurRadius: Float = 10f // Default blur radius
) {
    Box(
        modifier = modifier
            .widthIn(min = 310.dp, max = 350.dp) // Minimum and maximum width constraints
            .fillMaxWidth(0.3f) // Fills 30% of the parent's width, clamped by widthIn
            .height(80.dp) // Fixed height
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
            contentAlignment = Alignment.Center // This centers the Row
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth() // Fill the width of the inner content box
                    .padding(horizontal = 8.dp, vertical = 4.dp), // Padding inside the content area
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Distribute items evenly
            ) {
                // Plus Button (Far Left)
                IconButton(
                    onClick = onPlusClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Attachment",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(Modifier.width(8.dp)) // Space between plus button and attachments

                // Attachment Thumbnails (Middle)
                Row(
                    modifier = Modifier.weight(1f), // Occupy available space
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Space between thumbnails
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Limit to a maximum of 4 attachments
                    attachments.take(4).forEach { uri ->
                        AttachmentThumbnail(
                            uri = uri,
                            onDelete = onAttachmentDeleteClick
                        )
                    }
                }

                Spacer(Modifier.width(8.dp)) // Space between attachments and send button

                // Send Button (Far Right)
                IconButton(
                    onClick = onSendClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}