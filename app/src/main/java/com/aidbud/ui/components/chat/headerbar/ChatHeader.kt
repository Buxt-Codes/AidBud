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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Divider

@Composable
fun HeaderBar(
    conversationId: Long,
    // NavController is typically passed from a higher-level navigation graph.
    // For preview purposes, we'll use a dummy NavController.
    // navController: NavController, // Uncomment this line in your actual code
    title: String,
    modifier: Modifier = Modifier
) {
    Column( // Root container is now a Column to stack content and the Divider
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White) // Background for the entire header area
    ) {
        Box( // This Box holds the Row with buttons and title
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp), // Fixed height for the content area
            contentAlignment = Alignment.Center // Centers the Row horizontally and vertically within this Box
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp), // *** KEY CHANGE: Fixed 10.dp horizontal padding ***
                horizontalArrangement = Arrangement.SpaceBetween, // Distribute items with space between
                verticalAlignment = Alignment.CenterVertically // Vertically center all items in the row
            ) {
                // Left: Menu Button
                MenuButton(
                    onClick = { /* TODO: Implement menu action */ },
                    iconSize = 25.dp,
                    // No need for .offset or .align here, padding on Row handles spacing
                )

                Box (
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PCardTitle(
                        title = title,
                        onClick = { /* TODO: Implement title click action */ }
                    )
                }

                // Right: Chat Button
                CameraButton(
                    // onClick = { navController.navigate("chat/$conversationId") }, // Uncomment in actual code
                    onClick = { /* Dummy navigation for preview */ println("Navigate to chat/$conversationId") },
                    iconSize = 20.dp,
                    // No need for .offset or .align here
                )
            }
        }

        // *** KEY CHANGE: Divider at the bottom of the HeaderBar ***
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color.LightGray.copy(alpha = 0.6f) // Light gray color for the divider
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeaderBarPreview() {
    MaterialTheme {
        // For preview, we don't have a real NavController, so we'll pass a dummy value or remove it from preview
        HeaderBar(
            conversationId = 123L,
            // navController = rememberNavController(), // Uncomment if you have navigation-compose in your preview setup
            title = "AidBud"
        )
    }
}