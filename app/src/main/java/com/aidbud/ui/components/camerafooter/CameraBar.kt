package com.aidbud.ui.components.camerafooter

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
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.AnimatedVisibility

enum class FlashMode {
    Auto, On, Off
}

@Composable
fun CameraBar(
    modifier: Modifier = Modifier, // Add modifier parameter for external control
    onTakePhoto: () -> Unit, // Separate function for taking a photo
    onStartVideo: () -> Unit, // Function for starting video recording
    onStopVideo: () -> Unit, // Separate function for stopping video recording
    onSendClick: () -> Unit // New parameter for SendButton click
    onFlashModeChange: (FlashMode) -> Unit,
    onCameraFlipClick: () -> Unit,
    onPhotoAlbumClick: () -> Unit,
    onTextClick: () -> Unit
) {
    val cornerRadius = 48.dp
    val blurRadius = 50f

    var isRecordingVideo by remember { mutableStateOf(false) }
    var currentFlashMode by remember { mutableStateOf(FlashMode.Auto) }
    var showExtraOptions by remember { mutableStateOf(false) }

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
            if (!showExtraOptions) {
                Row(
                    modifier = modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    PlusButton(
                        onClick = { showExtraOptions = true } // Toggle expanded options
                    )

                    CameraFlashButton(
                        currentFlashMode = currentFlashMode,
                        onClick = { newMode ->
                            currentFlashMode = newMode
                            onFlashModeChange(newMode)
                        }
                    )

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

                    CameraFlipButton(
                        onClick = onCameraFlipClick
                    )

                    // Place the SendButton to the right of the RecordButton
                    SendButton(
                        onClick = onSendClick // Pass the send click function
                    )
                }
            }

            AnimatedVisibility(
                visible = showExtraOptions,
                enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
                modifier = Modifier
                    .fillMaxSize() // Occupy the same space as CameraBar
                    .clip(RoundedCornerShape(cornerRadius)) // Match CameraBar's rounded corners
                    .background(Color.LightGray.copy(alpha = 0.7f)) // Slightly more opaque background for overlay
                    .graphicsLayer {
                        renderEffect = BlurEffect(radiusX = blurRadius, radiusY = blurRadius) // Apply blur to overlay
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp), // Padding inside the expanded bar
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button (to close expanded options)
                    PlusButton( // Reusing PlusButton for simplicity, could be a custom back icon
                        onClick = { showExtraOptions = false }
                    )

                    // Photo Album Button
                    PhotoAlbumButton(
                        onClick = onPhotoAlbumClick
                    )

                    // Text Button
                    TextButton(
                        onClick = onTextClick
                    )
                }
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
            onTakePhoto = { println("Preview: Photo taken!") },
            onStartVideo = { println("Preview: Video started!") },
            onStopVideo = { println("Preview: Video stopped!") },
            onSend = { println("Preview: Send Button Click!") }
        )
    }
}