package app.iremote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.iremote.ir.CodeFormat
import app.iremote.ir.data.IrRepository
import app.iremote.ir.data.RemoteKeyEntity
import app.iremote.ir.data.RemoteProfileEntity
import app.iremote.ui.components.AppTopBar
import app.iremote.ui.dialogs.InputDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun EditRemoteScreen(
    repo: IrRepository,
    remoteId: Long,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val remoteFlow = repo.remote(remoteId)
    val remote by remoteFlow.collectAsState(initial = null)
    var name by remember { mutableStateOf(remote?.name ?: if (remoteId == -1L) "New Remote" else "") }
    var brand by remember { mutableStateOf(remote?.brand ?: "") }
    var model by remember { mutableStateOf(remote?.model ?: "") }

    val keys by remember(remoteId) {
        if (remoteId > 0) repo.keys(remoteId) else MutableStateFlow(emptyList())
    }.collectAsState(initial = emptyList())

    var showAddKey by remember { mutableStateOf(false) }
    var showPronto by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("Edit Remote") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val savedId = repo.saveRemote(
                                RemoteProfileEntity(
                                    id = remote?.id ?: 0,
                                    name = name.trim(),
                                    brand = brand.trim().ifBlank { null },
                                    model = model.trim().ifBlank { null }
                                )
                            )
                            if (remoteId == -1L && savedId > 0) {
                                // nothing else
                            }
                            onClose()
                        }
                    }) { Icon(Icons.Default.Save, contentDescription = "Save") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAddKey = true }) { Text("Add Key") }
                OutlinedButton(onClick = { showPronto = true }) { Text("Import Pronto") }
            }

            Divider()
            Text("Keys", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(keys, key = { it.id }) { k ->
                    ListItem(
                        headlineContent = { Text(k.name) },
                        supportingContent = { Text(k.format.name) },
                        trailingContent = {
                            IconButton(onClick = {
                                scope.launch { repo.deleteKey(k) }
                            }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                        }
                    )
                    Divider()
                }
            }
        }
    }

    if (showAddKey) {
        AddKeyDialog(
            onDismiss = { showAddKey = false },
            onCreate = { label, fmt, payload, freq, pattern ->
                val rId = remote?.id ?: 0
                if (rId == 0L) return@AddKeyDialog
                val entity = when (fmt) {
                    CodeFormat.PRONTO -> RemoteKeyEntity(profileId = rId, name = label, format = fmt, payload = payload)
                    CodeFormat.RAW -> RemoteKeyEntity(profileId = rId, name = label, format = fmt, carrierHz = freq, patternMicros = pattern)
                    CodeFormat.NEC, CodeFormat.RC5, CodeFormat.SAMSUNG, CodeFormat.RC6  -> RemoteKeyEntity(profileId = rId, name = label, format = fmt, payload = payload)
                }
                scope.launch { repo.saveKey(entity); showAddKey = false }
            }
        )
    }

    if (showPronto) {
        InputDialog(
            title = "Import Pronto Hex",
            label = "Pronto (0000 ...)",
            value = "",
            placeholder = "0000 006D 0022 0002 ...",
            onConfirm = { p ->
                val rId = remote?.id ?: 0
                if (rId != 0L) {
                    scope.launch {
                        repo.saveKey(
                            RemoteKeyEntity(profileId = rId, name = "Imported", format = CodeFormat.PRONTO, payload = p.trim())
                        )
                    }
                }
                showPronto = false
            },
            onDismiss = { showPronto = false },
            validator = { it.trim().startsWith("0000") && it.trim().length > 10 }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddKeyDialog(
    onDismiss: () -> Unit,
    onCreate: (label: String, fmt: CodeFormat, payload: String?, carrierHz: Int?, pattern: List<Int>?) -> Unit
) {
    var name by remember { mutableStateOf("Key") }
    var fmt by remember { mutableStateOf(CodeFormat.NEC) }
    var payload by remember { mutableStateOf("0x00:0x10") } // NEC addr:cmd
    var carrier by remember { mutableStateOf("38000") }
    var raw by remember { mutableStateOf("560,560,560,1690,...") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Key") },
        text = {
            var expanded by remember {mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())

                // Simple format selector
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        readOnly = true,
                        value = fmt.name,
                        onValueChange = {},
                        label = { Text("Format") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu( expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        CodeFormat.entries.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f.name, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    expanded = false
                                    fmt = f
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                when (fmt) {
                    CodeFormat.PRONTO -> OutlinedTextField(value = payload, onValueChange = { payload = it }, label = { Text("Pronto hex") }, modifier = Modifier.fillMaxWidth())
                    CodeFormat.NEC, CodeFormat.RC5, CodeFormat.RC6, CodeFormat.SAMSUNG -> OutlinedTextField(value = payload, onValueChange = { payload = it }, label = { Text("addr:cmd (hex)") }, modifier = Modifier.fillMaxWidth())
                    CodeFormat.RAW -> {
                        OutlinedTextField(value = carrier, onValueChange = { carrier = it }, label = { Text("Carrier Hz") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = raw, onValueChange = { raw = it }, label = { Text("Pattern (us, comma)") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (fmt) {
                    CodeFormat.PRONTO, CodeFormat.NEC, CodeFormat.RC5, CodeFormat.SAMSUNG, CodeFormat.RC6 -> onCreate(name, fmt, payload.trim(), null, null)
                    CodeFormat.RAW -> onCreate(name, fmt, null, carrier.toIntOrNull(), raw.split(",").mapNotNull { it.trim().toIntOrNull() })
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}