package com.aidbud.ui.components.cameraoverlay.attachmentbar

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.* // Using Material 3 components
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage // For loading images from Uri
import androidx.compose.foundation.clickable

import com.aidbud.R

@Composable
fun AttachmentThumbnail(
    modifier: Modifier = Modifier,
    uri: Uri,
    onDelete: (Uri) -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(3f / 4f) // Makes the box square, its width will match its height
            .fillMaxHeight() // Makes the box fill the height available in the parent row
            .clip(RoundedCornerShape(8.dp)) // Rounded corners for the thumbnail
            .background(MaterialTheme.colorScheme.surfaceVariant) // Background color for placeholder/loading
    ) {
        // AsyncImage from Coil to load the image from the Uri
        AsyncImage(
            model = uri,
            contentDescription = "Attachment thumbnail",
            contentScale = ContentScale.Crop, // Crop to fit the bounds
            modifier = Modifier.fillMaxSize()
        )

        // Trash bin icon at the top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)                 // Space from edges
                .size(20.dp)                   // Circle size
                .clip(CircleShape)
                .background(Color.Black)      // Black background
                .clickable { onDelete(uri) }, // Click handler
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.delete_icon_white),
                contentDescription = "Delete attachment",
                tint = Color.Unspecified, // Keep original icon tint (white)
                modifier = Modifier.size(16.dp) // Icon size inside circle
            )
        }
    }
}