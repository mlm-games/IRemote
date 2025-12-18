package app.iremote.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.iremote.AppGraph
import app.iremote.data.repository.AppSettings
import app.iremote.data.repository.AppSettingsSchema
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppGraph.settings

    val schema = AppSettingsSchema

    val settings: StateFlow<AppSettings> = repository.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, schema.default)

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events

    fun updateSetting(propertyName: String, value: Any) {
        viewModelScope.launch {
            repository.set(propertyName, value)
        }
    }

    fun performAction(propertyName: String) {
        viewModelScope.launch {
            when (propertyName) {
                else -> _events.emit(UiEvent.Toast("No action attached"))
            }
        }
    }

    sealed class UiEvent {
        data class Toast(val message: String) : UiEvent()
    }
}