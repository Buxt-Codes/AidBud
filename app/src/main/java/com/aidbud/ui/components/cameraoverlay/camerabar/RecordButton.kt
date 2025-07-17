package com.aidbud.ui.components.cameraoverlay.camerabar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RecordButton(
    modifier: Modifier = Modifier,
    // Renamed parameters for clarity, matching the CameraBar's usage
    isRecording: Boolean,
    onClick: () -> Unit,
    onHold: () -> Unit
) {
    val outerCircleSize = 60.dp // Size of the larger, transparent circle
    val innerCircleSize = 50.dp // Size of the smaller, solid white circle
    val borderWidth = 2.dp // Border width for the outer circle

    // State to manage the button's visual feedback
    var isPressed by remember { mutableStateOf(false) } // True when finger is down

    // Haptic feedback controller
    val haptic = LocalHapticFeedback.current

    // Define normal and contracted sizes for the inner circle
    val innerCircleSizeNormal: Dp = innerCircleSize
    val innerCircleSizeContracted: Dp = (innerCircleSize.value * 0.9f).dp // 10% smaller

    // Animate the inner circle size based on state
    val animatedInnerCircleSize by animateDpAsState(
        targetValue = when {
            isRecording -> innerCircleSizeNormal // Normal size when recording
            isPressed -> innerCircleSizeContracted // Contracted when pressed (not recording)
            else -> innerCircleSizeNormal // Normal size when not pressed and not recording
        },
        animationSpec = tween(durationMillis = 100), label = "innerCircleSizeAnimation"
    )

    // Animate the inner circle color based on recording state
    val animatedInnerCircleColor by animateColorAsState(
        targetValue = if (isRecording) Color.Red else Color.White,
        animationSpec = tween(durationMillis = 150), label = "innerCircleColorAnimation"
    )

    Box(
        modifier = modifier
            .size(outerCircleSize) // Set the overall size of the button
            .clip(CircleShape) // Clip the entire button area to a circle
            .border(borderWidth, Color.White, CircleShape) // White border for the outer circle
            .background(Color.Transparent) // Transparent white background for the outer circle
            .pointerInput(isRecording) { // Use pointerInput for stable gesture detection
                detectTapGestures(
                    onPress = {
                        // When finger touches down
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Vibrate lightly
                        println("Record Button: Press detected")
                    },
                    onTap = {
                        // When a short tap/click occurs
                        if (isRecording) {
                            // If currently recording, a tap stops the video
                            onClick() // Call function to stop video
                            println("Record Button: Short press (Stop Video)")
                        } else {
                            // If not recording, a tap takes a photo
                            onClick() // Call function to take photo
                            println("Record Button: Short press (Take Photo)")
                        }
                        isPressed = false // Reset press state here as tap is complete
                    },
                    onLongPress = {
                        // When a long press/hold occurs
                        onHold() // Call function to start video
                        isPressed = false // Stop contraction immediately when hold starts
                        println("Record Button: Long press (Start Video)")
                    }
                    // Removed onRelease as it's not a parameter of detectTapGestures
                )
            },
        contentAlignment = Alignment.Center // Center the inner circle
    ) {
        // Inner white/red circle with animated size and color
        Box(
            modifier = Modifier
                .size(animatedInnerCircleSize) // Use animated size
                .clip(CircleShape) // Clip the inner box to a circle
                .background(animatedInnerCircleColor) // Use animated color
        )
    }
}

/**
 * Preview for the updated RecordButton Composable.
 * Displays the button on a dark background for better visibility.
 */
@Preview(showBackground = true, backgroundColor = 0xFF333333) // Dark background for contrast
@Composable
fun RecordButtonUpdatedPreview() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        RecordButton(
            isRecording = false,
            onClick = { println("Preview: Photo/Stop Video action!") },
            onHold = { println("Preview: Start Video action!") }
        )
    }
}