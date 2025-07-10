package com.aidbud.ui.components.camerafooter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import com.aidbud.ui.components.general.SendButton

@Composable
fun CameraBar(
    modifier: Modifier = Modifier, // Add modifier parameter for external control
    onTakePhoto: () -> Unit, // Separate function for taking a photo
    onStartVideo: () -> Unit, // Function for starting video recording
    onStopVideo: () -> Unit, // Separate function for stopping video recording
    onSend: () -> Unit // New parameter for SendButton click
) {
    val cornerRadius = 48.dp
    val blurRadius = 50f

    var isRecordingVideo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .widthIn(min = 200.dp, max = 400.dp) // Minimum width of 200dp
            .fillMaxWidth(0.7f) // Fills 70% of the parent's width
            .height(65.dp), // Fixed height of 65dp
        contentAlignment = Alignment.Center // This centers the content (RecordButton) horizontally and vertically
    ) {
        // Inner Box for the background and blur effect
        // This ensures only the background is blurred, not the content placed on top of it.
        Box(
            modifier = Modifier
                .fillMaxSize() // Fill the size of the parent Box (CameraBar)
                .clip(RoundedCornerShape(cornerRadius)) // Apply rounded corners to the background
                .background(Color.LightGray.copy(alpha = 0.5f)) // Semi-transparent background
                .graphicsLayer {
                    renderEffect = BlurEffect(radiusX = blurRadius, radiusY = blurRadius) // Apply blur here
                }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center // This centers the RecordButton
        ) {
            // Place the RecordButton in the center of the CameraBar
            RecordButton(
                // Pass a lambda that checks the recording state and calls the appropriate function
                onClick = {
                    if (isRecordingVideo) {
                        onStopVideo() // If recording, stop video
                        isRecordingVideo = false // Update state
                        println("CameraBar: Stopping video via RecordButton short press")
                    } else {
                        onTakePhoto() // If not recording, take photo
                        println("CameraBar: Taking photo via RecordButton short press")
                    }
                },
                // When long pressed, start video and update state
                onHold = {
                    onStartVideo()
                    isRecordingVideo = true // Update state
                    println("CameraBar: Starting video via RecordButton long press")
                }
            )

            // Place the SendButton to the right of the RecordButton
            SendButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd) // Align to the center-right of the CameraBar
                    .padding(end = 16.dp), // Add some padding from the right edge
                onClick = onSend // Pass the send click function
            )
        }
    }
}


// Preview Composable for CameraBar with a white background
@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
fun CameraBarPreview() {
    // A Box to provide the white background for the preview
    Box(
        modifier = Modifier
            .fillMaxSize() // Fills the entire preview area
    ) {
        // Calls your CameraBar Composable to be displayed on the white background
        CameraBar(
            onTakePhoto = { println("Preview: Photo taken!") },
            onStartVideo = { println("Preview: Video started!") },
            onStopVideo = { println("Preview: Video stopped!") },
            onSend = { println("Preview: Send Button Click!") }
        )
    }
}