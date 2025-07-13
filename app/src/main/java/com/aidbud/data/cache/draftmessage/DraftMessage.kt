package com.aidbud.data.cache.draftmessage

import android.net.Uri

data class DraftMessage(
    val text: String = "",
    val attachments: List<Uri> = emptyList()
)