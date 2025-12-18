package app.iremote.ui

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.iremote.AppGraph
import app.iremote.data.repository.AppSettings
import app.iremote.viewmodel.RemoteViewModel
import app.iremote.ui.components.AppTopBar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoteScreen(
    vm: RemoteViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val bundle by vm.remote.collectAsState()
    val haptics = LocalHapticFeedback.current
    val settings by AppGraph.settings.flow.collectAsState(initial = AppSettings())

    // Keep screen on while in a remote if user enabled it
    KeepScreenOn(enabled = settings.keepScreenOn)

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(bundle?.remote?.name ?: "Remote") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                }
            )
        }
    ) { pad ->
        val keys = bundle?.keys.orEmpty()
        if (keys.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
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
        if (enabled) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}