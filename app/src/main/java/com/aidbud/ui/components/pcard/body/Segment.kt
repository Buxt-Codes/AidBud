package com.aidbud.ui.components.pcard.body

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight

@Composable
fun Segment(
    title: String,
    body: String,
    onBodyChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Display the title using a styled Text composable.
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // A Box is used to stack the BasicTextField and the placeholder Text on top of each other.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp) // Set a minimum height for the text field area.
        ) {
            // The actual editable text field.
            BasicTextField(
                value = body,
                onValueChange = {
                    onBodyChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Black
                ),
                cursorBrush = SolidColor(Color.Black),
            )

            if (body.isEmpty()) {
                Text(
                    text = "Write a message...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray
                    ),
                    modifier = Modifier.alpha(0.6f)
                )
            }
        }
    }
}

/**
 * A preview function to display the PlainEditableTextField composable.
 * This function demonstrates how to use the composable and manage its state.
 */
@Preview(showBackground = true)
@Composable
fun PlainEditableTextFieldPreview() {
    var text by remember { mutableStateOf("") }

    Segment(
        title = "My Note",
        body = text,
        onBodyChange = { newText -> text = newText }
    )
}