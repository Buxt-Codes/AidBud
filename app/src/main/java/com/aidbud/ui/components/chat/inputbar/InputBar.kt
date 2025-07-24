package com.aidbud.ui.components.chat.inputbar

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Divider

@Composable
fun InputBar(
    attachments: List<Uri>,
    onDeleteAttachment: (Uri) -> Unit,
    text: String,
    onTextChange: (String) -> Unit,
    onDraftUpdate: (String) -> Unit,
    onSendClick: () -> Unit,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {

        if (attachments.isNotEmpty()) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp, // Thickness of your border line
                color = Color.LightGray.copy(alpha = 0.6f) // Your light gray border color
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp) // Apply padding to the content within the bar
        ) {

            // Attachment bar (only visible if attachments exist)
            if (attachments.isNotEmpty()) {
                AttachmentBar(
                    attachments = attachments,
                    onDeleteAttachment = onDeleteAttachment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            // Row with PlusButton and ChatInputBox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlusButton(
                    onClick = onPlusClick,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                )

                ChatInputBox(
                    text = text,
                    onTextChange = onTextChange,
                    onDraftUpdate = onDraftUpdate,
                    onSendClick = onSendClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewInputBarWithAttachments() {
    // Sample URIs for preview (using fake strings as Uri.parse)
    val sampleUris = listOf(
        Uri.parse("content://sample/image1"),
        Uri.parse("content://sample/image2"),
        Uri.parse("content://sample/image3")
    )

    var text by remember { mutableStateOf("Hello world") }

    InputBar(
        attachments = sampleUris,
        onDeleteAttachment = { /* no-op for preview */ },
        text = text,
        onTextChange = { text = it },
        onDraftUpdate = { /* no-op for preview */ },
        onSendClick = { /* no-op for preview */ },
        onPlusClick = { /* no-op for preview */ }
    )
}
