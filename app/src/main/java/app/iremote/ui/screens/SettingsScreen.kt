package app.iremote.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import app.iremote.data.repository.AppSettings
import app.iremote.data.repository.Appearance
import app.iremote.data.repository.General
import app.iremote.data.repository.System
import app.iremote.ui.components.AppTopBar
import app.iremote.viewmodel.SettingsViewModel
import io.github.mlmgames.settings.core.SettingField
import io.github.mlmgames.settings.core.types.Button
import io.github.mlmgames.settings.core.types.Dropdown
import io.github.mlmgames.settings.core.types.Slider
import io.github.mlmgames.settings.core.types.Toggle
import io.github.mlmgames.settings.ui.components.SettingsAction
import io.github.mlmgames.settings.ui.components.SettingsItem
import io.github.mlmgames.settings.ui.components.SettingsToggle
import io.github.mlmgames.settings.ui.dialogs.DropdownSettingDialog
import io.github.mlmgames.settings.ui.dialogs.SliderSettingDialog
import java.util.Locale
import kotlin.reflect.KClass

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val settings by vm.settings.collectAsState()
    val schema = vm.schema

    var showDropdown by remember { mutableStateOf(false) }
    var showSlider by remember { mutableStateOf(false) }
    var currentField by remember { mutableStateOf<SettingField<AppSettings, *>?>(null) }

    // Group fields by category
    val grouped = remember(schema) { schema.groupedByCategory() }

    val cfg = LocalConfiguration.current
    val gridCells = remember(cfg.screenWidthDp) { GridCells.Adaptive(minSize = 420.dp) }

    // Category display names
    val categoryNames: Map<KClass<*>, String> = mapOf(
        General::class to "General",
        Appearance::class to "Appearance",
        System::class to "System"
    )

    MyScreenScaffold(title = "Settings") { _ ->
        LazyVerticalGrid(
            columns = gridCells,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Iterate through ordered categories
            schema.orderedCategories().forEach { category ->
                val fieldsInCategory = grouped[category] ?: return@forEach
                if (fieldsInCategory.isEmpty()) return@forEach

                // Category header
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = categoryNames[category] ?: category.simpleName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Settings items
                items(fieldsInCategory, key = { it.name }) { field ->
                    val meta = field.meta ?: return@items  // Skip @Persisted-only fields
                    val enabled = schema.isEnabled(settings, field)

                    when (meta.type) {
                        Toggle::class -> {
                            @Suppress("UNCHECKED_CAST")
                            val boolField = field as SettingField<AppSettings, Boolean>
                            SettingsToggle(
                                title = meta.title,
                                description = meta.description.takeIf { it.isNotBlank() },
                                checked = boolField.get(settings),
                                enabled = enabled,
                                onCheckedChange = { vm.updateSetting(field.name, it) }
                            )
                        }

                        Dropdown::class -> {
                            @Suppress("UNCHECKED_CAST")
                            val intField = field as SettingField<AppSettings, Int>
                            val idx = intField.get(settings)
                            SettingsItem(
                                title = meta.title,
                                subtitle = meta.options.getOrNull(idx) ?: "Unknown",
                                description = meta.description.takeIf { it.isNotBlank() },
                                enabled = enabled,
                                onClick = {
                                    currentField = field
                                    showDropdown = true
                                }
                            )
                        }

                        Slider::class -> {
                            @Suppress("UNCHECKED_CAST")
                            val floatField = field as? SettingField<AppSettings, Float>
                            @Suppress("UNCHECKED_CAST")
                            val intField = field as? SettingField<AppSettings, Int>

                            val valueText = when {
                                floatField != null -> String.format(Locale.getDefault(), "%.1f", floatField.get(settings))
                                intField != null -> intField.get(settings).toString()
                                else -> ""
                            }

                            SettingsItem(
                                title = meta.title,
                                subtitle = valueText,
                                description = meta.description.takeIf { it.isNotBlank() },
                                enabled = enabled,
                                onClick = {
                                    currentField = field
                                    showSlider = true
                                }
                            )
                        }

                        Button::class -> {
                            SettingsAction(
                                title = meta.title,
                                description = meta.description.takeIf { it.isNotBlank() },
                                buttonText = "Run",
                                enabled = enabled,
                                onClick = { vm.performAction(field.name) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dropdown Dialog
    val cf = currentField
    if (showDropdown && cf?.meta != null) {
        val meta = cf.meta!!
        @Suppress("UNCHECKED_CAST")
        val intField = cf as SettingField<AppSettings, Int>

        DropdownSettingDialog(
            title = meta.title,
            options = meta.options,
            selectedIndex = intField.get(settings),
            onDismiss = { showDropdown = false },
            onOptionSelected = { i ->
                vm.updateSetting(cf.name, i)
                showDropdown = false
            }
        )
    }

    // Slider Dialog
    if (showSlider && cf?.meta != null) {
        val meta = cf.meta!!

        @Suppress("UNCHECKED_CAST")
        val floatField = cf as? SettingField<AppSettings, Float>
        @Suppress("UNCHECKED_CAST")
        val intField = cf as? SettingField<AppSettings, Int>

        val currentValue = when {
            floatField != null -> floatField.get(settings)
            intField != null -> intField.get(settings).toFloat()
            else -> 0f
        }

        SliderSettingDialog(
            title = meta.title,
            currentValue = currentValue,
            min = meta.min,
            max = meta.max,
            step = meta.step,
            onDismiss = { showSlider = false },
            onValueSelected = { value ->
                when {
                    intField != null -> vm.updateSetting(cf.name, value.toInt())
                    floatField != null -> vm.updateSetting(cf.name, value)
                }
                showSlider = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreenScaffold(
    title: String,
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                AppTopBar(
                    title = {
                        Text(
                            title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
//                    colors = TopAppBarDefaults.topAppBarColors(
//                        containerColor = MaterialTheme.colorScheme.surface,
//                        titleContentColor = MaterialTheme.colorScheme.onSurface
//                    ),
                    actions = actions
                )
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            content(paddingValues)
        }
    }
}
