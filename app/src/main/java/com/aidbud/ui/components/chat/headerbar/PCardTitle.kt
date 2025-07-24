package com.aidbud.ui.components.chat.headerbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip

// IMPORTANT: Replace with the actual R import for your project's drawables
// Ensure you have a drawable resource named 'ic_default_icon' (or your actual icon name)
import com.aidbud.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PCardTitle(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // State to track if the box is pressed
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(10.dp)

    // Animation for the scale
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1.0f, // Shrink to 80% when pressed, back to normal otherwise
        animationSpec = tween(durationMillis = 100), // Quick animation for feedback
        label = "boxScaleAnimation"
    )

    Box(
        modifier = modifier
            .scale(scale) // Apply the scaling animation here
            .background(Color.LightGray.copy(alpha = 0.2f), shape)
            .border(1.dp, Color.LightGray.copy(alpha = 0.6f), shape)
            .pointerInput(Unit) { // Use pointerInput for detailed tap gestures
                detectTapGestures(
                    onPress = { /*offset*/ // Called when pointer goes down
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Subtle feedback
                    },
                    onTap = { // Called when a tap gesture is recognized (press & release within bounds)
                        onClick() // Trigger the actual click action
                        isPressed = false // Reset state after tap
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 8.dp), // Inner padding for content (icon + text)
        contentAlignment = Alignment.Center // Center the content (Row) within this box
    ) {
        // Row to place the icon and text side-by-side
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Static Icon (not animated by AnimatedContent)
            Icon(
                painter = painterResource(id = R.drawable.pcard_icon_white), // *** REPLACE WITH YOUR ACTUAL ICON ID ***
                contentDescription = "Conversation Icon", // Accessibility description
                tint = Color.Black, // Adjust tint as needed
                modifier = Modifier.size(20.dp) // Set desired icon size
            )

            Spacer(modifier = Modifier.width(6.dp)) // Space between icon and text

            // AnimatedContent wraps ONLY the Text for title animation
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(durationMillis = 300)
                            ) + fadeOut(animationSpec = tween(durationMillis = 300))
                },
                label = "Title Transition"
            ) { targetTitle ->
                val textScrollState = rememberScrollState()
                // The Text composable for the title
                Box(
                    modifier = Modifier
                        // It's important for the Box to fill the available width
                        // so that text overflowing it can be scrolled.
                        .horizontalScroll(textScrollState) // Apply the horizontal scrolling here
                ) {
                    // The Text composable itself
                    Text(
                        text = targetTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black,
                        maxLines = 1, // Crucial: Ensures text stays on a single line
                        softWrap = false, // Prevents word wrapping
                        overflow = TextOverflow.Clip // Cuts off text if it overflows without scrolling
                        // Change to TextOverflow.Ellipsis if you want "..." before scrolling
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 100)
@Composable
fun AnimatedPageTitleBoxPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PCardTitle(
                title = "My Awesome Chat",
                onClick = { /* Handle click */ }
            )
        }
    }
}