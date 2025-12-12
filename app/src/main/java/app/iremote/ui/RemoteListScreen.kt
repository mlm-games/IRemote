package app.iremote.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import app.iremote.AppGraph
import app.iremote.ui.components.AppTopBar
import app.iremote.viewmodel.RemoteListViewModel
import kotlinx.coroutines.launch

@Composable
fun RemoteListScreen(
    vm: RemoteListViewModel,
    onOpenSettings: () -> Unit,
    onOpenRemote: (Long) -> Unit,
    onCreateRemote: () -> Unit
) {
    val remotes by vm.remotes.collectAsState()
    val hasIr by vm.hasEmitter.collectAsState()

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = AppGraph.irRepo
    var showMenu by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!json.isNullOrBlank()) repo.importBundle(json)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    fun shareTextFile(name: String, content: String) {
        val f = java.io.File(ctx.cacheDir, name)
        f.writeText(content)
        val u = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, u)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "Share JSON"))
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("IRemote") },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Export all (JSON)") },
                            onClick = {
                                showMenu = false
                                scope.launch {
                                    val json = repo.exportAll()
                                    shareTextFile("remotes.json", json)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import (JSON)") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = { showMenu = false; onOpenSettings() }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateRemote) { Icon(Icons.Default.Add, contentDescription = "New remote") }
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

                    var showRowMenu by remember { mutableStateOf(false) }

                    ListItem(
                        headlineContent = { Text(r.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(listOfNotNull(r.brand, r.model).joinToString(" • "))
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { vm.toggleFavorite(r) }) {
                                    Icon(
                                        imageVector = if (r.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = { showRowMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Remote menu")
                                }
                                DropdownMenu(expanded = showRowMenu, onDismissRequest = { showRowMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            showRowMenu = false
                                            vm.deleteRemote(r)
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .clickable { onOpenRemote(r.id) }
                            .padding(horizontal = 8.dp)
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}