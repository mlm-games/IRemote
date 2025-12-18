package app.iremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.iremote.data.repository.AppSettings
import app.iremote.ui.EditRemoteScreen
import app.iremote.ui.RemoteListScreen
import app.iremote.ui.RemoteScreen
import app.iremote.viewmodel.RemoteListViewModel
import app.iremote.viewmodel.RemoteViewModel
import app.iremote.ui.screens.SettingsScreen
import app.iremote.ui.theme.MainTheme
import app.iremote.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private inline fun <reified T : ViewModel> vm(crossinline create: () -> T) =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
        }

    private val settingsVM: SettingsViewModel by viewModels {
        vm { SettingsViewModel(application) }
    }

    private val listVM: RemoteListViewModel by viewModels {
        vm { RemoteListViewModel(AppGraph.irRepo, AppGraph.settings) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(applicationContext)

        setContent {
            val settings by AppGraph.settings.flow.collectAsState(initial = AppSettings())
            val dark = when (settings.themeMode) {
                0 -> isSystemInDarkTheme()
                1 -> false
                else -> true
            }

            MainTheme(darkTheme = dark, useAuroraTheme = settings.useAuroraTheme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") {
                            RemoteListScreen(
                                vm = listVM,
                                onOpenSettings = { nav.navigate("settings") },
                                onOpenRemote = { id -> nav.navigate("remote/$id") },
                                onCreateRemote = { nav.navigate("edit/-1") }
                            )
                        }
                        composable(
                            "remote/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStack ->
                            val id = backStack.arguments?.getLong("id") ?: -1L
                            val remoteVM: RemoteViewModel = viewModelFactoryRemote(id)
                            RemoteScreen(
                                vm = remoteVM,
                                onBack = { nav.popBackStack() },
                                onEdit = { nav.navigate("edit/$id") }
                            )
                        }
                        composable(
                            "edit/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.LongType })
                        ) { backStack ->
                            val id = backStack.arguments?.getLong("id") ?: -1L
                            EditRemoteScreen(
                                repo = AppGraph.irRepo,
                                remoteId = id,
                                onClose = { nav.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(vm = settingsVM)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun viewModelFactoryRemote(remoteId: Long): RemoteViewModel {
        val factory = vm { RemoteViewModel(remoteId, AppGraph.irRepo, AppGraph.settings) }
        return viewModel(factory = factory)
    }
}
