package com.aidbud.ui.components.chat.inputbar

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.aidbud.ui.components.cameraoverlay.attachmentbar.AttachmentThumbnail

@Composable
fun AttachmentBar(
    attachments: List<Uri>,
    onDeleteAttachment: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // Fixed height for attachment bar
            .padding(top = 1.dp) // Push content down to avoid drawing over the border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (uri in attachments) {
                AttachmentThumbnail(
                    uri = uri,
                    onDelete = { onDeleteAttachment },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
