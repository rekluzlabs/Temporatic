package com.rekluzlabs.temporatic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.temporatic.data.ScreenshotRecord
import com.rekluzlabs.temporatic.data.ScreenshotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScreenshotViewModel @Inject constructor(
    private val repository: ScreenshotRepository
) : ViewModel() {

    val allScreenshots: Flow<List<ScreenshotRecord>> = repository.getRecent(100)

    val allApps: Flow<List<String>> = repository.getAllApps()

    val totalCount: Flow<Int> = repository.totalCount()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    val isSelectionMode: Boolean get() = _selectedIds.value.isNotEmpty()

    fun toggleSelection(id: String) {
        _selectedIds.value = if (_selectedIds.value.contains(id)) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectAllScreenshots(ids: List<String>) {
        _selectedIds.value = ids.toSet()
    }

    fun selectNoneScreenshots() {
        _selectedIds.value = emptySet()
    }

    private val _selectedAppLabels = MutableStateFlow<Set<String>>(emptySet())
    val selectedAppLabels: StateFlow<Set<String>> = _selectedAppLabels.asStateFlow()

    private val _isAllSelected = MutableStateFlow(false)
    val isAllSelected: StateFlow<Boolean> = _isAllSelected.asStateFlow()

    fun toggleAllSelection() {
        _isAllSelected.value = !_isAllSelected.value
        if (_isAllSelected.value) {
            _selectedAppLabels.value = emptySet()
        }
    }

    fun toggleAppSelection(appLabel: String) {
        _isAllSelected.value = false
        _selectedAppLabels.value = if (_selectedAppLabels.value.contains(appLabel)) {
            _selectedAppLabels.value - appLabel
        } else {
            _selectedAppLabels.value + appLabel
        }
    }

    fun clearAppSelection() {
        _selectedAppLabels.value = emptySet()
        _isAllSelected.value = false
    }

    fun getScreenshotsForApp(appLabel: String): Flow<List<ScreenshotRecord>> {
        return repository.getByApp(appLabel)
    }

    fun countByApp(appLabel: String): Flow<Int> = repository.countByApp(appLabel)

    fun deleteScreenshot(record: ScreenshotRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            repository.deleteByIds(_selectedIds.value)
            _selectedIds.value = emptySet()
        }
    }

    fun deleteSelectedApps(apps: List<String>, deleteAll: Boolean) {
        viewModelScope.launch {
            if (deleteAll) {
                repository.deleteAll()
            } else {
                apps.forEach { appLabel ->
                    repository.deleteByApp(appLabel)
                }
            }
            _selectedAppLabels.value = emptySet()
            _isAllSelected.value = false
        }
    }
}
