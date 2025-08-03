package com.aidbud.data.downloader

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

sealed class DownloadState {
    object Idle : DownloadState()
    data class Loading(val progress: Float) : DownloadState()
    object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}

object ModelDownloader {
    // OkHttpClient is a heavy object, so it's best to create a single instance
    private val client = OkHttpClient()

    /**
     * Downloads a file from a given URL and saves it to a specified file.
     * It emits the download progress as a Flow.
     *
     * @param url The URL of the file to download.
     * @param destinationFile The file where the downloaded data will be saved.
     * @return A Flow of DownloadState, which can be collected to update the UI.
     */
    fun downloadFile(url: String, destinationFile: File): Flow<DownloadState> = flow {
        // Emit a loading state with initial progress
        emit(DownloadState.Loading(0.0f))

        val request = Request.Builder().url(url).build()
        var totalBytes = 0L
        var downloadedBytes = 0L

        try {
            // Execute the network request
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                // If the response is not successful, emit an error
                emit(DownloadState.Error("HTTP Error: ${response.code}"))
                return@flow
            }

            // Get the total size of the file from the Content-Length header
            totalBytes = response.body?.contentLength() ?: -1L
            if (totalBytes == -1L) {
                // If content-length is not available, emit an error or handle it
                // differently (e.g., just show an indeterminate progress bar)
                emit(DownloadState.Error("Content-Length header not found."))
                return@flow
            }

            // Write the response body to the destination file
            response.body?.byteStream()?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(4096) // Use a 4KB buffer
                    var bytesRead = inputStream.read(buffer)
                    while (bytesRead != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        // Calculate and emit the current progress
                        val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                        emit(DownloadState.Loading(progress))

                        bytesRead = inputStream.read(buffer)
                    }
                }
            }

            // If the loop completes, the download is successful
            emit(DownloadState.Success)

        } catch (e: IOException) {
            // Catch and handle network or I/O errors
            e.printStackTrace()
            emit(DownloadState.Error("Download failed: ${e.message}"))
        }
    }
}