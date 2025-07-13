package com.aidbud.ui.components.cameraoverlay.attachmentbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
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

import com.aidbud.R

// --- PlusButton Composable (New) ---
/**
 * A button with a plus icon that expands into additional options.
 * It provides visual and haptic feedback on interaction, similar to other buttons.
 *
 * @param modifier The modifier to be applied to the button.
 * @param onClick Lambda to be invoked when the button is clicked.
 */
@Composable
fun PlusButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val buttonSize = 50.dp
    val iconSizeNormal = 35.dp
    val iconSizeContracted = (iconSizeNormal.value * 0.8f).dp
    val plusIcon = ImageVector.vectorResource(id = R.drawable.plus_icon_white)

    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val animatedIconSize by animateDpAsState(
        targetValue = if (isPressed) iconSizeContracted else iconSizeNormal,
        animationSpec = tween(durationMillis = 100), label = "plusIconSizeAnimation"
    )

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(Color.LightGray.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        println("Plus Button: Press detected")
                    },
                    onTap = {
                        onClick()
                        isPressed = false
                        println("Plus Button: Clicked!")
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = plusIcon,
            contentDescription = "Expand Options",
            tint = Color.White,
            modifier = Modifier.size(animatedIconSize)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF333333) // Dark background for contrast
@Composable
fun PlusButtonPreview() {
    PlusButton(
        onClick = { /* TODO: Implement send action */ }
    )
}