package com.aidbud.ui.components.cameraoverlay.headerbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.clickable
import com.aidbud.ui.components.chat.headerbar.PCardTitle

@OptIn(ExperimentalAnimationApi::class) // Required for AnimatedContent
@Composable
fun PCardTitle(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center // Center the title within the box
    ) {
        // AnimatedContent handles the transition between different content states (in this case, title changes)
        AnimatedContent(
            targetState = title, // The state that triggers the animation when it changes
            transitionSpec = {
                // Define the transition for content entering and exiting
                // New content slides in from the right, old content slides out to the left
                // The 'with' operator expects EnterTransition.with(ExitTransition)
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth }, // Slide in from the right
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth }, // Slide out to the left
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut(animationSpec = tween(durationMillis = 300))
            },
            label = "Title Transition" // Label for debugging/tooling
        ) { targetTitle ->
            // The content to display for the current targetState (title)
            Text(
                text = targetTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ), // Apply a suitable text style
                color = Color.DarkGray // Use appropriate color from theme
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 100)
@Composable
fun AnimatedPageTitleBoxPreview() {
    // This preview won't show the animation directly as it's static.
    // To see the animation, you'd integrate this into a stateful composable
    // where the 'title' changes over time (e.g., via a button click or timer).
    PCardTitle (
        title = "Preview Title",
        onClick = { /* TODO: Implement send action */ }
    )
}