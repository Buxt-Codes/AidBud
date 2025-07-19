package com.aidbud.data.cache.draftmessage

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DraftMessage(
    text: String = "",
    attachments: List<Uri> = emptyList(),
    totalVideoDurationMillis: Long = 0L
) {
    var text by mutableStateOf(text)
    var totalVideoDurationMillis by mutableStateOf(totalVideoDurationMillis)
    val attachments = mutableStateListOf<Uri>().apply {
        addAll(attachments)
    }
}