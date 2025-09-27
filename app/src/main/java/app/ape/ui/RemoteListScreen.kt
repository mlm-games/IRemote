package app.ape.ir.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ape.ui.components.AppTopBar
import app.ape.ir.viewmodel.RemoteListViewModel

@Composable
fun RemoteListScreen(
    vm: RemoteListViewModel,
    onOpenSettings: () -> Unit,
    onOpenRemote: (Long) -> Unit,
    onCreateRemote: () -> Unit
) {
    val remotes by vm.remotes.collectAsState()
    val hasIr by vm.hasEmitter.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("ape Remote") },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRemote) {
                Icon(Icons.Default.Add, contentDescription = "New remote")
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (!hasIr) {
                AssistChip(
                    onClick = {},
                    label = { Text("No IR emitter detected. You can still manage remotes.") }
                )
            }
            OutlinedTextField(
                value = "",
                onValueChange = { vm.setQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                label = { Text("Search") }
            )

            LazyColumn(Modifier.fillMaxSize()) {
                items(remotes, key = { it.id }) { r ->
                    ListItem(
                        headlineContent = { Text(r.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(listOfNotNull(r.brand, r.model).joinToString(" • "))
                        },
                        trailingContent = {
                            val label = if (r.favorite) "★" else "☆"
                            Text(label, style = MaterialTheme.typography.titleLarge)
                        },
                        modifier = Modifier
                            .clickable { onOpenRemote(r.id) }
                            .padding(horizontal = 8.dp)
                    )
                    Divider()
                }
            }
        }
    }
}