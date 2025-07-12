package com.aidbud.ui.components.camerafooter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ExpandedOptionsOverlay(
    modifier: Modifier = Modifier,
    onPhotoAlbumClick: () -> Unit,
    onTextClick: () -> Unit,
    onDismiss: () -> Unit // Callback to dismiss the overlay
) {
    val barHeight = 60.dp // Height of the expanded bar
    val barWidth = 200.dp // Width of the expanded bar
    val cornerRadius = 30.dp // Rounded corners for the expanded bar

    Box(
        modifier = modifier
            .fillMaxSize() // This Box covers the entire screen
            .background(Color.LightGray.copy(alpha = 0.2f))
            .pointerInput(Unit) { // Detect taps anywhere on this background
                detectTapGestures(onTap = {
                    onDismiss() // Dismiss if tapped outside the inner bar
                })
            }
    ) {
        // The actual expanded bar, placed above the CameraBar (using padding)
        AnimatedVisibility(
            visible = true, // Always visible when this Composable is rendered
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomStart) // Align to bottom-left of the screen
                .padding(start = 16.dp, bottom = 80.dp) // Position above CameraBar and aligned with PlusButton
                .size(width = barWidth, height = barHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.LightGray.copy(alpha = 0.7f))
                .graphicsLayer {
                    renderEffect = BlurEffect(radiusX = 50f, radiusY = 50f)
                }
                .pointerInput(Unit) { // Consume clicks on this bar so they don't dismiss it
                    detectTapGestures(onTap = {})
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