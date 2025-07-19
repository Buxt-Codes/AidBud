package com.aidbud.ui.components.cameraoverlay.headerbar

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
fun MenuButton(
    navController: NavController,
    destinationRoute: String,
    boxSize: Dp = 64.dp, // Default size for the circular box
    iconSize: Dp = 36.dp, // Default size for the icon
) {
    val menuIcon = ImageVector.vectorResource(id = R.drawable.home_icon_white)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth() // Ensure the column takes full width to center content
    ) {
        Box(
            modifier = Modifier
                .size(boxSize) // Set the size of the circular box
                .clip(CircleShape) // Clip the box to a circular shape
                .background(Color.Transparent) // Make the background transparent
                .clickable {
                    // Navigate to the specified destination route when clicked
                    navController.navigate(destinationRoute)
                },
            contentAlignment = Alignment.Center // Center the icon inside the box
        ) {
            Icon(
                imageVector = menuIcon, // Use the Home icon
                contentDescription = "Home Button",
                modifier = Modifier.size(iconSize) // Set the size of the icon
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuButtonPreview() {
    // Create a dummy NavController for the preview
    val dummyNavController = rememberNavController()
    MenuButton(
        navController = dummyNavController,
        destinationRoute = "some_destination"
    )
}
