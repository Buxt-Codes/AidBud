package com.aidbud.ui.components.cameraoverlay.headerbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import androidx.navigation.NavController

@Composable
fun HeaderBar(
    conversationId: Long, // New parameter
    navController: NavController, // New parameter
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 50.dp, // Default corner radius for the bar
    barBackgroundColor: Color = Color.LightGray.copy(alpha = 0.5f) // Example background for the bar
) {
    // Calculate the offset needed for the button's center to be 20dp from the border.
    // Button size is 100.dp, so radius is 50.dp.
    // If center is at 20dp, left edge is at 20dp - 50dp = -30dp.
    val buttonOffset = -30.dp

    Box(
        modifier = modifier
            .widthIn(min = 310.dp, max = 400.dp) // Minimum width of 310dp, maximum of 400dp
            .fillMaxWidth(0.9f) // Fills 90% of the parent's width
            .height(60.dp) // Fixed height of 80dp
            .clip(RoundedCornerShape(cornerRadius)) // Apply rounded corners to the entire bar
            .background(barBackgroundColor) // Background for the bar itself
            .border(0.5.dp, Color.White, RoundedCornerShape(cornerRadius)), // Add the white border here
        contentAlignment = Alignment.Center // This centers the content (Row) horizontally and vertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            // Removed padding from Row to allow buttons to offset
            horizontalArrangement = Arrangement.SpaceBetween, // Distribute items with space between
            verticalAlignment = Alignment.CenterVertically // Vertically center all items in the row
        ) {
            // Left: Menu Button
            MenuButton(
                onClick = { /* TODO: Implement send action */ },
                iconSize = 25.dp, // Use default or customize
                modifier = Modifier.offset(x = buttonOffset) // Apply offset to move button left
            )

            // Middle: Animated Page Title Box
            AnimatedPageTitleBox(
                title = title,
                onClick = { /* TODO: Implement send action */ },
                modifier = Modifier.weight(1f), // Take up remaining space
            )

            // Right: Chat Button
            ChatButton(
                onClick = { navController.navigate("chat/$conversationId") },
                iconSize = 20.dp, // Use default or customize
                modifier = Modifier.offset(x = -buttonOffset) // Apply offset to move button right (negative of left offset)
            )
        }
    }
}