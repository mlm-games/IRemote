package app.iremote.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.iremote.data.repository.SettingsRepository
import app.iremote.ir.data.IrRepository
import app.iremote.ir.data.RemoteProfileEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RemoteListViewModel(
    private val repo: IrRepository,
    settings: SettingsRepository
) : ViewModel() {
    val hasEmitter = MutableStateFlow(repo.hasEmitter())
    private val query = MutableStateFlow("")
    private val all: Flow<List<RemoteProfileEntity>> = repo.remotes()
    private val sortMode: Flow<Int> = settings.settingsFlow.map { it.defaultSort }

    val remotes: StateFlow<List<RemoteProfileEntity>> =
        combine(all, query, sortMode) { list, q, sort ->
            val t = q.trim().lowercase()
            var filtered = if (t.isBlank()) list else list.filter {
                it.name.lowercase().contains(t) ||
                        (it.brand?.lowercase()?.contains(t) == true) ||
                        (it.model?.lowercase()?.contains(t) == true)
            }

            filtered = when (sort) {
                0 -> filtered.sortedBy { it.name.lowercase() }
                1 -> filtered.sortedByDescending { it.updatedAt }
                2 -> filtered.sortedByDescending { it.createdAt }
                else -> filtered
            }

            filtered
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


    fun setQuery(q: String) { query.value = q }

    fun toggleFavorite(item: RemoteProfileEntity) = viewModelScope.launch {
        repo.saveRemote(item.copy(favorite = !item.favorite))
    }

    fun deleteRemote(item: RemoteProfileEntity) = viewModelScope.launch {
        repo.deleteRemote(item)
    }
}