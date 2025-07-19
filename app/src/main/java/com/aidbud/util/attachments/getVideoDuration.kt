package com.aidbud.util.attachments

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

/**
 * Retrieves the duration of a video from its Uri.
 * @param context The application context.
 * @param uri The Uri of the video.
 * @return The duration of the video in milliseconds, or 0L if an error occurs.
 */
fun getVideoDuration(context: Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        return durationStr?.toLongOrNull() ?: 0L
    } catch (e: Exception) {
        Log.e("VideoDurationUtil", "Error getting video duration for URI: $uri", e)
        return 0L
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) {
            Log.e("VideoDurationUtil", "Error releasing MediaMetadataRetriever", e)
        }
    }
}