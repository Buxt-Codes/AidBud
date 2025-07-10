package com.aidbud.ui.components.camerafooter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.unit.dp

@Composable
fun CameraBar() {
    val cornerRadius = 48.dp
    val blurRadius = 50f
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(100.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.LightGray.copy(alpha = 0.5f))
            .graphicsLayer {
                renderEffect = BlurEffect(radiusX = blurRadius, radiusY = blurRadius)
                // No shadow or border as per your request
            }
    ) {
        // No content inside for now, as requested.
        // This space can be used later for buttons or other UI elements.
    }
}