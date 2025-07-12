package com.aidbud.ui.components.cameraoverlay.mediumbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MediumBar(
    modifier: Modifier = Modifier,
    onPhotoAlbumClick: () -> Unit,
    onTextClick: () -> Unit,
    onDismiss: () -> Unit // Callback to dismiss the overlay
) {
    val barHeight = 65.dp // Height of the expanded bar
    val barWidth = 150.dp // Width of the expanded bar
    val cornerRadius = 30.dp // Rounded corners for the expanded bar

    // State to hold the global bounds of the MediumBar.
    var barRect by remember { mutableStateOf(Rect.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize() // This Box covers the entire screen, acting as a scrim
            .background(Color.Transparent) // Semi-transparent scrim visual
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent() // Wait for any pointer event
                        val change = event.changes.first() // Get the first pointer change
                        val currentPosition = change.position // Get the current position of the pointer

                        when (event.type) {
                            PointerEventType.Press -> {
                                if (!barRect.contains(currentPosition)) {
                                    // If the press occurred OUTSIDE the MediumBar:
                                    // Call onDismiss immediately.
                                    onDismiss()
                                    // IMPORTANT: DO NOT consume the event.
                                    // This allows the underlying UI element to receive the Press.
                                } else {
                                    // If the press occurred INSIDE the MediumBar:
                                    // Consume the event. This prevents underlying elements from reacting
                                    // to presses that are intended for the MediumBar itself or its buttons.
                                    change.consume()
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }
            }
    ) {
        // 1. Blurred Background Layer:
        // This Box is placed *behind* the actual MediumBar and has the blur effect.
        // It's given the same size, shape, and position as the main bar.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter) // Adjust alignment as needed (e.g., to match CameraBar)
                .padding(bottom = 16.dp) // Example padding
                .size(width = barWidth, height = barHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .graphicsLayer {
                    // Apply blur effect to this background Box.
                    // This blurs whatever is behind this semi-transparent layer,
                    // creating the "frosted glass" effect without blurring the bar's content.
                    renderEffect = BlurEffect(radiusX = 50f, radiusY = 50f)
                }
                .background(Color.Gray.copy(alpha = 0.5f)) // Semi-transparent background for the blur
        )

        // 2. The Actual MediumBar Content:
        // This is the visible bar with buttons. It's placed on top of the blurred background.
        AnimatedVisibility(
            visible = true, // Keep as true as per original code; for a true overlay, control this with state in parent
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter) // Align to match the blurred background
                .padding(bottom = 16.dp) // Example padding
                .size(width = barWidth, height = barHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .border(0.5.dp, Color.White, RoundedCornerShape(cornerRadius))
                .background(Color.Gray.copy(alpha = 0.5f)) // Sharp background for the bar itself
                .onGloballyPositioned { coordinates ->
                    // Get the global position and size of the MediumBar.
                    // This is used by the outer pointerInput to determine if a click was inside or outside.
                    val windowPosition = coordinates.positionInWindow()
                    barRect = Rect(
                        offset = windowPosition,
                        size = coordinates.size.toSize()
                    )
                }
                .pointerInput(Unit) {
                    // This pointerInput is specifically for the MediumBar itself.
                    // It consumes any taps that occur directly on the bar's background
                    // (i.e., not on the buttons), preventing them from being processed
                    // by the outer scrim's pointerInput, which would otherwise dismiss the bar.
                    // Clicks on the actual buttons are handled by the buttons' own onClick.
                    detectTapGestures(onTap = {}) // Consume taps within the bar's area
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhotoAlbumButton(
                    onClick = {
                        onPhotoAlbumClick()
                        onDismiss() // Dismiss after action
                    }
                )
                TextButton(
                    onClick = {
                        onTextClick()
                        onDismiss() // Dismiss after action
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333)
@Composable
fun MediumBarPreview() {
    // A Box to provide the white background for the preview
    Box(
        modifier = Modifier
            .fillMaxSize() // Fills the entire preview area
    ) {
        // Calls your CameraBar Composable to be displayed on the white background
        MediumBar(
            onPhotoAlbumClick = { println("Preview: Photo taken!") },
            onTextClick = { println("Preview: Video started!") },
            onDismiss = { println("Preview: Video stopped!") }
        )
    }
}