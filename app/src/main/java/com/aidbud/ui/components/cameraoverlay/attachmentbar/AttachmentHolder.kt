package com.aidbud.ui.components.cameraoverlay.attachmentbar

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Image // Import for Image composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator // For loading indicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text // For placeholder text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap // For ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap // For converting Bitmap to ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers // Import for Dispatchers
import kotlinx.coroutines.withContext // Import for withContext

import com.aidbud.R

@Composable
private fun VideoThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var thumbnailBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        isLoading = true
        thumbnailBitmap = null // Clear previous thumbnail when URI changes
        try {
            // Perform thumbnail extraction on a background thread
            val bitmap = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    // Get a frame at 1 microsecond (near the start of the video)
                    retriever.getFrameAtTime(1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    println("Error setting data source or getting frame: ${e.message}")
                    null
                } finally {
                    retriever.release() // Release the retriever resources
                }
            }
            thumbnailBitmap = bitmap?.asImageBitmap() // Convert to ImageBitmap
        } catch (e: Exception) {
            println("Error extracting video thumbnail: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            // Show a gray background with a loading indicator
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray.copy(alpha = 1f)), // Gray background
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp)) // Show loading indicator
            }
        } else if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!,
                contentDescription = "Video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Add play icon in the middle of the video thumbnail
            Icon(
                painter = painterResource(id = R.drawable.play_icon_black),
                contentDescription = "Play video",
                tint = Color.White, // Keep original icon tint (black)
                modifier = Modifier
                    .size(28.dp) // Adjust size as needed
                    .align(Alignment.BottomStart) // Align to bottom-start
                    .padding(4.dp) // Add padding from edges
            )
        } else {
            // Placeholder for failed thumbnail loading: show a gray background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray.copy(alpha = 1f)), // Gray background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.play_icon_black),
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp) // Adjust size as needed
                        .align(Alignment.BottomStart) // Align to bottom-start
                        .padding(4.dp) // Add padding from edges
                )
            }
        }
    }
}

@Composable
fun AttachmentThumbnail(
    modifier: Modifier = Modifier,
    uri: Uri,
    onDelete: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val mimeType = context.contentResolver.getType(uri)
    val isVideo = mimeType?.startsWith("video/") == true

    Box(
        modifier = modifier
            .aspectRatio(3f / 4f) // Makes the box square, its width will match its height
            .fillMaxHeight() // Makes the box fill the height available in the parent row
            .clip(RoundedCornerShape(8.dp)) // Rounded corners for the thumbnail
            .background(MaterialTheme.colorScheme.surfaceVariant) // Background color for placeholder/loading
    ) {
        // AsyncImage from Coil to load the image from the Uri
        if (isVideo) {
            VideoThumbnail(uri = uri, modifier = Modifier.fillMaxSize())
        } else {
            // AsyncImage from Coil to load the image from the Uri
            AsyncImage(
                model = uri,
                contentDescription = "Attachment thumbnail",
                contentScale = ContentScale.Crop, // Crop to fit the bounds
                modifier = Modifier.fillMaxSize()
            )
        }

        // Trash bin icon at the top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp) // This padding positions the entire delete button container
        ) {
            Box( // This inner Box defines the clickable circular area
                modifier = Modifier
                    .size(20.dp)                   // Circle size
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 1f)) // Semi-transparent black background
                    .clickable { onDelete(uri) }, // Click handler applied to the clipped circle
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon_white),
                    contentDescription = "Delete attachment",
                    tint = Color.White, // Explicitly set tint to white
                    modifier = Modifier.size(16.dp) // Icon size inside circle
                )
            }
        }
    }
}