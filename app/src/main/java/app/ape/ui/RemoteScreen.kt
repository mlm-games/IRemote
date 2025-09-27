package app.ape.ir.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.ape.data.repository.AppSettings
import app.ape.ir.viewmodel.RemoteViewModel
import app.ape.ui.components.AppTopBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoteScreen(
    vm: RemoteViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val bundle by vm.remote.collectAsState()
    val haptics = LocalHapticFeedback.current
    val settings by app.ape.AppGraph.settings.settingsFlow.collectAsState(initial = AppSettings())

    // Keep screen on while in a remote if user enabled it
    KeepScreenOn(enabled = settings.keepScreenOn)

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(bundle?.remote?.name ?: "Remote") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                }
            )
        }
    ) { pad ->
        val keys = bundle?.keys.orEmpty()
        if (keys.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No keys yet. Tap Edit to add.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                contentPadding = pad
            ) {
                items(keys, key = { it.id }) { key ->
                    ElevatedButton(
                        onClick = {
                            vm.sendOnce(key)
                            if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .height(64.dp)
                            .combinedClickable(
                                onClick = {
                                    vm.sendOnce(key)
                                    if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onLongClick = {
                                    vm.startRepeat(key)
                                    if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onLongClickLabel = "Hold to repeat",
                                onDoubleClick = null
                            )
                    ) {
                        Text(key.name)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { vm.stopRepeat() }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val activity = (LocalActivity.current)
    DisposableEffect(enabled) {
        if (enabled) activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}