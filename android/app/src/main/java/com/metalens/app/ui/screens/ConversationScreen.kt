package com.metalens.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metalens.app.R
import com.metalens.app.conversation.ChatMessage
import com.metalens.app.conversation.ChatRole
import com.metalens.app.conversation.ConversationStatus
import com.metalens.app.conversation.ConversationViewModel

@Composable
fun ConversationScreen(
    modifier: Modifier = Modifier,
    onStop: () -> Unit,
) {
    val activity = LocalContext.current as ComponentActivity
    val viewModel: ConversationViewModel = viewModel(activity)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val hasMicPermission =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    val permissionLauncher =
        rememberLauncherForActivityResult(RequestPermission()) { granted ->
            if (granted) {
                viewModel.start()
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stop()
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text?.length) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text =
                when (uiState.status) {
                    ConversationStatus.Idle -> stringResource(R.string.conversation_status_idle)
                    ConversationStatus.Connecting -> stringResource(R.string.conversation_status_connecting)
                    ConversationStatus.Listening -> stringResource(R.string.conversation_status_listening)
                    ConversationStatus.Speaking -> stringResource(R.string.conversation_status_speaking)
                    ConversationStatus.Error -> stringResource(R.string.conversation_status_error)
                },
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = "userSpeaking=${uiState.isUserSpeaking}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        uiState.recentError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Chat transcript area
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Transparent)
                    .padding(12.dp),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 14.dp, top = 4.dp, bottom = 4.dp),
            ) {
                items(items = uiState.messages.filter { it.text.isNotBlank() }, key = { it.id }) { msg ->
                    ChatBubble(message = msg, modifier = Modifier.fillMaxWidth())
                }
            }

            // Visible vertical scrollbar overlay (Android Compose doesn't draw one by default).
            ChatVerticalScrollbar(
                listState = listState,
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    if (hasMicPermission) {
                        viewModel.start()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                enabled = uiState.status == ConversationStatus.Idle || uiState.status == ConversationStatus.Error,
            ) {
                Text(stringResource(R.string.conversation_start))
            }

            Button(
                onClick = { viewModel.stop() },
                enabled = uiState.status != ConversationStatus.Idle,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            ) {
                Text(stringResource(R.string.conversation_stop))
            }
        }

        if (!hasMicPermission) {
            Text(
                text = stringResource(R.string.conversation_permission_needed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = {
                viewModel.stop()
                onStop()
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == ChatRole.User
    val containerColor =
        if (isUser) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    val contentColor =
        if (isUser) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Column(
        modifier = modifier,
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = if (isUser) "You" else "AI",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = containerColor,
            contentColor = contentColor,
            shape = MaterialTheme.shapes.large,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 40.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun ChatVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val layoutInfo = listState.layoutInfo
    val total = layoutInfo.totalItemsCount
    val visible = layoutInfo.visibleItemsInfo.size

    // Only show when there is history to scroll.
    if (total <= 0 || visible <= 0 || total <= visible) return

    Canvas(modifier = modifier) {
        val trackWidthPx = 3.dp.toPx()
        val paddingPx = 6.dp.toPx()
        val trackHeight = size.height - paddingPx * 2
        if (trackHeight <= 0f) return@Canvas

        val thumbMinHeight = 24.dp.toPx()
        val thumbHeight =
            maxOf(thumbMinHeight, trackHeight * (visible.toFloat() / total.toFloat()))

        val firstIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
        val maxScrollIndex = (total - visible).coerceAtLeast(1)
        val progress = (firstIndex.toFloat() / maxScrollIndex.toFloat()).coerceIn(0f, 1f)
        val thumbTop = paddingPx + (trackHeight - thumbHeight) * progress

        val x = size.width - paddingPx - trackWidthPx
        val trackColor = Color.Black.copy(alpha = 0.06f)
        val thumbColor = Color.Black.copy(alpha = 0.22f)

        // Track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(x, paddingPx),
            size = Size(trackWidthPx, trackHeight),
            cornerRadius = CornerRadius(trackWidthPx, trackWidthPx),
        )
        // Thumb
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(x, thumbTop),
            size = Size(trackWidthPx, thumbHeight),
            cornerRadius = CornerRadius(trackWidthPx, trackWidthPx),
        )
    }
}

