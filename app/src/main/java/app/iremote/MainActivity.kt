package app.iremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import app.iremote.data.repository.AppSettings
import app.iremote.ui.theme.MainTheme
import app.iremote.ui.util.NavGraph
import app.iremote.ui.util.Screen
import app.iremote.viewmodel.RemoteListViewModel
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
                    val backStack = rememberNavBackStack(Screen.Home)
                    BackHandler(enabled = backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    }

                    NavGraph(
                        backStack = backStack,
                        decorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator()
                        )
                        ,
                        listVM = listVM,
                        settingsVM = settingsVM
                    )
                }
            }
        }
    }
}