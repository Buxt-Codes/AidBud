package com.aidbud.ui.components.pcard.headerbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.aidbud.R

/**
 * A standalone composable function for a Menu Button.
 * This button navigates to a specified destination when clicked.
 *
 * @param navController The NavController used for navigation.
 * @param destinationRoute The route string to navigate to when the button is clicked.
 * @param buttonText The text to display on the button.
 */
@Composable
fun CameraButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    iconSize: Dp = 25.dp, // Default size for the icon
) {
    val cameraIcon = ImageVector.vectorResource(id = R.drawable.camera_icon_white)

    Box(
        modifier = Modifier
            .size(50.dp) // Set the size of the circular box
            .background(Color.Transparent), // Translucent gray bubble background
        contentAlignment = Alignment.Center // Center the icon inside the box
    ) {
        Icon(
            imageVector = cameraIcon,
            contentDescription = "Camera Button",
            tint = Color.Black, // Assuming the icon should be white as per 'home_icon_white'
            modifier = Modifier
                .size(iconSize) // Set the size of the icon
                .clickable {
                    // Navigate to the specified destination route when clicked
                    onClick()
                },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CameraButtonPreview() {
    CameraButton(
        onClick = { /* TODO: Implement send action */ }
    )
}