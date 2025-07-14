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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage // For loading images from Uri

import com.aidbud.R

@Composable
fun AttachmentThumbnail(
    modifier: Modifier = Modifier,
    uri: Uri,
    onDelete: (Uri) -> Unit,
) {
    Box(
        modifier = modifier
            .width(48.dp)  // Rectangular width
            .height(64.dp) // Rectangular height
            .clip(RoundedCornerShape(8.dp)) // Rounded corners for the thumbnail
            .background(MaterialTheme.colorScheme.surfaceVariant) // Background color for placeholder/loading
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)) // Border
    ) {
        // AsyncImage from Coil to load the image from the Uri
        AsyncImage(
            model = uri,
            contentDescription = "Attachment thumbnail",
            contentScale = ContentScale.Crop, // Crop to fit the bounds
            modifier = Modifier.fillMaxSize()
        )

        // Trash bin icon at the top-right
        IconButton(
            onClick = { onDelete(uri) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.White) // solid white circle
        ) {
            Icon(
                painter = painterResource(id = R.drawable.remove_icon_black),
                contentDescription = "Delete attachment",
                tint = Color.Black, // apply black tint
                modifier = Modifier.size(8.dp)
            )
        }
    }
}