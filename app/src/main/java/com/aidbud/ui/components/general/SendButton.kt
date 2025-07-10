package com.aidbud.ui.components.general

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon // Using Material3 Icon for modern Compose apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

/**
 * A customizable send button with a transparent circular background and a white send icon.
 *
 * @param modifier The modifier to be applied to the button.
 * @param onClick Lambda to be invoked when the button is clicked.
 */
@Composable
fun SendButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val buttonSize = 50.dp // Size of the circular button
    val iconSizeNormal = 45.dp // Normal size of the send icon
    val iconSizeContracted = (iconSizeNormal.value * 0.8f).dp // 20% smaller when pressed

    // State to manage the button's visual feedback
    var isPressed by remember { mutableStateOf(false) }

    // Haptic feedback controller
    val haptic = LocalHapticFeedback.current

    // Animate the icon size based on pressed state
    val animatedIconSize by animateDpAsState(
        targetValue = if (isPressed) iconSizeContracted else iconSizeNormal,
        animationSpec = tween(durationMillis = 100), label = "iconSizeAnimation"
    )

    Box(
        modifier = modifier
            .size(buttonSize) // Set the size of the circular button
            .clip(CircleShape) // Clip the Box to a circular shape
            .background(Color.Transparent) // Changed to fully transparent background
            .pointerInput(Unit) { // Use pointerInput for stable gesture detection
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Vibrate lightly
                        println("Send Button: Press detected")
                    },
                    onTap = {
                        onClick() // Invoke the provided onClick lambda
                        isPressed = false // Reset press state here as tap is complete
                        println("Send Button: Clicked!")
                    }
                    // No onLongPress for SendButton, as it's a simple click
                )
            },
        contentAlignment = Alignment.Center // Center the icon within the button
    ) {
        Icon(
            imageVector = Icons.Filled.Send, // The send icon
            contentDescription = "Send", // Content description for accessibility
            tint = Color.White, // White color for the icon
            modifier = Modifier.size(animatedIconSize) // Use animated size for the icon
        )
    }
}

/**
 * Preview for the SendButton Composable.
 * Displays the button on a dark background for better visibility of the transparent white elements.
 */
@Preview(showBackground = true, backgroundColor = 0xFF333333) // Dark background for contrast
@Composable
fun SendButtonPreview() {
    SendButton(
        onClick = { /* TODO: Implement send action */ }
    )
}