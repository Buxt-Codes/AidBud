package com.aidbud.ui.pages.loading

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aidbud.data.downloader.DownloadState
import com.aidbud.data.viewmodel.MainViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

@Composable
fun LoadingPage(
    navController: NavController,
    viewModel: MainViewModel
) {

    var statusText by remember { mutableStateOf("Initializing...") }
    val downloadState by viewModel.downloadState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!viewModel.isModelDownloaded()) {
            viewModel.startDownload()
        } else {
            statusText = "Model found. Navigating to the next page..."
            navController.navigate("initial_setup") {
                popUpTo("loading_page") { inclusive = true }
            }
        }
    }

    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Loading -> {
                statusText = "Downloading model... (${String.format("%.1f", state.progress * 100)}%)"
            }
            DownloadState.Success -> {
                statusText = "Download complete! Initializing application..."
                navController.navigate("initial_setup") {
                    popUpTo("loading_page") { inclusive = true }
                }
            }
            is DownloadState.Error -> {
                statusText = "Download failed: ${state.message}"
            }
            DownloadState.Idle -> {
                statusText = "Checking for local model..."
            }
        }
    }

    // UI layout for the loading page
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.Gray
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Conditionally show the progress bar if the state is loading
        if (downloadState is DownloadState.Loading) {
            LinearProgressIndicator(
                progress = (downloadState as DownloadState.Loading).progress,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Conditionally show the retry button if the state is an error
        if (downloadState is DownloadState.Error) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.startDownload()
                    }
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .border(
                        width = 1.dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(4.dp)
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Retry Download",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Black
                    )
                )
            }
        }
    }
}