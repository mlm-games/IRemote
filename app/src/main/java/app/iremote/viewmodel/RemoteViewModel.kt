package app.iremote.ir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.iremote.data.repository.SettingsRepository
import app.iremote.ir.data.RemoteKeyEntity
import app.iremote.ir.data.RemoteWithKeys
import app.iremote.ir.repo.IrRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class RemoteViewModel(
    private val remoteId: Long,
    private val repo: IrRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    val remote: StateFlow<RemoteWithKeys?> =
        repo.remoteWithKeys(remoteId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var repeatJob: Job? = null

    val uiEvents = MutableSharedFlow<String>() // toasts, errors

    fun sendOnce(key: RemoteKeyEntity) {
        val r = repo.send(key)

//        when (val r = repo.send(key)) {
//            is Result.isSuccess -> Unit
//            is Result.isFailure -> viewModelScope.launch { uiEvents.emit("Send failed: ${r.exceptionOrNull()?.message}") }
//        }
    }

    fun startRepeat(key: RemoteKeyEntity) {
        stopRepeat()
        if (!key.repeatWhileHeld) { sendOnce(key); return }
        val delayMs = (key.holdRepeatDelayMs).coerceIn(60, 500)
        repeatJob = viewModelScope.launch(Dispatchers.IO) {
            // initial press
            repo.send(key)
            // sustain
            while (isActive) {
                delay(delayMs.toLong())
                repo.send(key)
            }
        }
    }

    fun stopRepeat() { repeatJob?.cancel(); repeatJob = null }
}