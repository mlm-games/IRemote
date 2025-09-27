package app.ape.ir.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ape.data.repository.SettingsRepository
import app.ape.ir.data.RemoteProfileEntity
import app.ape.ir.repo.IrRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RemoteListViewModel(
    private val repo: IrRepository,
    private val settings: SettingsRepository
) : ViewModel() {
    val hasEmitter = MutableStateFlow(repo.hasEmitter())
    val ranges = repo.capabilityRanges()

    private val query = MutableStateFlow("")
    private val all: Flow<List<RemoteProfileEntity>> = repo.remotes()

    val remotes: StateFlow<List<RemoteProfileEntity>> =
        combine(all, query) { list, q ->
            val t = q.trim().lowercase()
            if (t.isBlank()) list else list.filter {
                it.name.lowercase().contains(t) ||
                        (it.brand?.lowercase()?.contains(t) == true) ||
                        (it.model?.lowercase()?.contains(t) == true)
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setQuery(q: String) { query.value = q }

    fun toggleFavorite(item: RemoteProfileEntity) = viewModelScope.launch {
        repo.saveRemote(item.copy(favorite = !item.favorite))
    }
}