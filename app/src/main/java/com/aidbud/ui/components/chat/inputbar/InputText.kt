package com.aidbud.ui.components.chat.inputbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ChatInputBox(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val maxHeight = 150.dp // Maximum height for the text field
    val minHeight = 40.dp // Minimum height for the text field
    val shape = RoundedCornerShape(40.dp)

    Row(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .background(Color.White, shape)
            .border(1.dp, Color.Black, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom // Keeps the whole row aligned to the bottom (important for multi-line)
    ) {
        // Box that holds the BasicTextField and the placeholder
        Box(
            modifier = Modifier
                .weight(1f) // Takes up available horizontal space
                .heightIn(min = minHeight, max = maxHeight) // Constrains the height
                .verticalScroll(scrollState) // Allows vertical scrolling when content exceeds max height
                .padding(end = 8.dp),
            contentAlignment = Alignment.CenterStart // *** KEY CHANGE: Align content to CenterStart ***
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    if (it.length <= 3000) {
                        onTextChange(it)
                    }
                },
                textStyle = TextStyle(color = Color.Black),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier.fillMaxWidth() // Fills the width of its parent Box
            )

            // Placeholder text, only visible when text is empty
            if (text.isEmpty()) {
                Text(
                    text = "Write a message...",
                    color = Color.Gray,
                    modifier = Modifier.alpha(0.6f)
                )
            }
        }

        SendButton(
            onClick = onSendClick,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(minHeight)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatInputBoxPreview() {
    var text by remember { mutableStateOf("") }
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray),
            verticalArrangement = Arrangement.Bottom
        ) {
            ChatInputBox(
                text = text,
                onTextChange = { newText -> text = newText },
                onSendClick = { /* Handle send */ }
            )
        }
    }
}
