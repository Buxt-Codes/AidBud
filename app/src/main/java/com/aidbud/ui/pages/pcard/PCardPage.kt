package com.aidbud.ui.pages.pcard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aidbud.data.cache.draftmessage.DraftMessage
import com.aidbud.data.cache.draftmessage.GlobalCacheViewModel
import com.aidbud.data.conversation.Conversation
import com.aidbud.data.settings.SettingsViewModel
import com.aidbud.data.viewmodel.MainViewModel
import com.aidbud.ui.components.chat.MessageBox
import com.aidbud.ui.components.chat.MessageRole
import com.aidbud.ui.components.chat.headerbar.HeaderBar
import com.aidbud.ui.components.chat.inputbar.InputBar
import com.aidbud.ui.components.pcard.body.SegmentList
import com.aidbud.util.attachments.getVideoDuration

@Composable
fun PCardPage(
    conversationId: Long,
    navController: NavController,
    viewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    cacheViewModel: GlobalCacheViewModel
) {
    val currentConversation by remember(conversationId) {
        viewModel.getConversationById(conversationId)
    }.collectAsStateWithLifecycle(initialValue = Conversation(conversationId, "AidBud", System.currentTimeMillis()))

    val pcards by viewModel.getPCardsForConversation(conversationId)
        .collectAsState(initial = emptyList())

    val pCard = pcards.firstOrNull()

    Scaffold(
        topBar = {
            HeaderBar(
                conversationId = conversationId,
                navController = navController,
                title = currentConversation?.title ?: "AidBud",
                modifier = Modifier.fillMaxWidth()
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pCard != null) {
                SegmentList(
                    pCard,
                    viewModel
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No patient card generated.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}