package com.aidbud.ui.components.cameraoverlay.camerabar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

enum class FlashMode {
    Auto, On, Off
}

@Composable
fun CameraBar(
    modifier: Modifier = Modifier, // Add modifier parameter for external control
    isRecording: Boolean,
    onTakePhoto: () -> Unit, // Separate function for taking a photo
    onStartVideo: () -> Unit, // Function for starting video recording
    onStopVideo: () -> Unit, // Separate function for stopping video recording
    onFlashModeChange: (FlashMode) -> Unit,
    onCameraFlipClick: () -> Unit
) {
    val cornerRadius = 48.dp
    val blurRadius = 50f

    var currentFlashMode by remember { mutableStateOf(FlashMode.Auto) }

    Box(
        modifier = Modifier
            .widthIn(min = 310.dp, max = 350.dp) // Minimum width of 200dp
            .fillMaxWidth(0.3f) // Fills 70% of the parent's width
            .height(80.dp) // Fixed height of 65dp
            .clip(RoundedCornerShape(cornerRadius)) // Apply rounded corners to the entire bar
            .border(0.5.dp, Color.White, RoundedCornerShape(cornerRadius)), // Add the white border here
        contentAlignment = Alignment.Center // This centers the content (RecordButton) horizontally and vertically
    ) {
        // Inner Box for the background and blur effect
        // This ensures only the background is blurred, not the content placed on top of it.
        Box(
            modifier = Modifier
                .fillMaxSize() // Fill the size of the parent Box (CameraBar)
                .clip(RoundedCornerShape(cornerRadius)) // Apply rounded corners to the background
                .background(Color.Gray.copy(alpha = 0.5f)) // Semi-transparent background
                .graphicsLayer {
                    renderEffect = BlurEffect(radiusX = blurRadius, radiusY = blurRadius) // Apply blur here
                }
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center // This centers the RecordButton
        ) {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {

                CameraFlashButton(
                    currentFlashMode = currentFlashMode,
                    onClick = { newMode ->
                        currentFlashMode = newMode
                        onFlashModeChange(newMode)
                    }
                )

                // Place the RecordButton in the center of the CameraBar
                RecordButton(
                    isRecording = isRecording,
                    // Pass a lambda that checks the recording state and calls the appropriate function
                    onClick = {
                        if (isRecording) {
                            onStopVideo() // If recording, stop video
                            println("CameraBar: Stopping video via RecordButton short press")
                        } else {
                            onTakePhoto() // If not recording, take photo
                            println("CameraBar: Taking photo via RecordButton short press")
                        }
                    },
                    // When long pressed, start video and update state
                    onHold = {
                        onStartVideo()
                        println("CameraBar: Starting video via RecordButton long press")
                    }
                )

                CameraFlipButton(
                    onClick = onCameraFlipClick
                )

            }
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
            isRecording = false,
            onTakePhoto = { println("Preview: Photo taken!") },
            onStartVideo = { println("Preview: Video started!") },
            onStopVideo = { println("Preview: Video stopped!") },
            onFlashModeChange = { mode -> println("Preview: Flash mode changed to $mode") },
            onCameraFlipClick = { println("Preview: Camera flipped!") }
        )
    }
}