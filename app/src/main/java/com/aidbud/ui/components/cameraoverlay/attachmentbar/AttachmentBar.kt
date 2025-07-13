package com.aidbud.ui.components.cameraoverlay.attachmentbar

import androidx.compose.ui.Modifier
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.ui.components.cameraoverlay.camerabar.FlashMode

fun AttachmentBar(
    modifier: Modifier = Modifier, // Add modifier parameter for external control
    settingsViewModel: SettingsViewModel,
    cacheViewModel: GlobalCacheViewModel,
    isRecording: Boolean,
    onSendClick: () -> Unit,
    onPlusClick: () -> Unit
) {