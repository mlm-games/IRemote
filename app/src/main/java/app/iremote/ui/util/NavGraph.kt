package app.iremote.ui.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import app.iremote.AppGraph
import app.iremote.ui.EditRemoteScreen
import app.iremote.ui.RemoteListScreen
import app.iremote.ui.RemoteScreen
import app.iremote.ui.screens.SettingsScreen
import app.iremote.ui.util.Screen
import app.iremote.viewmodel.RemoteListViewModel
import app.iremote.viewmodel.RemoteViewModel
import app.iremote.viewmodel.SettingsViewModel
import kotlinx.serialization.Serializable

@Composable
fun NavGraph(
    backStack: NavBackStack<NavKey>,
    decorators: List<NavEntryDecorator<Any>>,
    listVM: RemoteListViewModel,
    settingsVM: SettingsViewModel
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeAt(backStack.lastIndex) },
        entryDecorators = decorators,
        entryProvider = entryProvider {

            entry<Screen.Home> {
                RemoteListScreen(
                    vm = listVM,
                    onOpenSettings = { backStack.add(Screen.Settings) },
                    onOpenRemote = { id -> backStack.add(Screen.Remote(id)) },
                    onCreateRemote = { backStack.add(Screen.EditRemote(-1)) }
                )
            }
            entry<Screen.Remote> { args ->
                // Create VM scoped to this entry
                val remoteVM: RemoteViewModel = viewModel {
                    RemoteViewModel(args.id, AppGraph.irRepo, AppGraph.settings)
                }
                
                RemoteScreen(
                    vm = remoteVM,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    onEdit = { backStack.add(Screen.EditRemote(args.id)) }
                )
            }
            entry<Screen.EditRemote> { args ->
                EditRemoteScreen(
                    repo = AppGraph.irRepo,
                    remoteId = args.id,
                    onClose = { backStack.removeAt(backStack.lastIndex) }
                )
            }
            entry<Screen.Settings> {
                SettingsScreen(vm = settingsVM)
            }
        }
    )
}

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Home : Screen

    @Serializable
    data class Remote(val id: Long) : Screen

    @Serializable
    data class EditRemote(val id: Long) : Screen // id -1 means create new

    @Serializable
    data object Settings : Screen
}