package com.example.myTools.bazi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class BaZiDialogType {
    ADD,
    SETTINGS,
    DATA_MANAGEMENT,
    TRASH
}

data class BaZiUiState(
    val records: List<BaZiRecord> = emptyList(),
    val trashRecords: List<BaZiRecord> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedTagFilter: String? = null,
    val recordToEdit: BaZiRecord? = null,
    val recordToDelete: BaZiRecord? = null,
    val selectedRecord: BaZiRecord? = null,
    val managingTagsRecord: BaZiRecord? = null,
    val activeDialog: BaZiDialogType? = null
) {
    val allUniqueTags: List<String>
        get() = records.flatMap { it.safeTags }.distinct().sorted()

    val filteredRecords: List<BaZiRecord>
        get() {
            var list = records
            if (selectedTagFilter != null) {
                list = list.filter { it.safeTags.contains(selectedTagFilter) }
            }
            if (searchQuery.isNotEmpty()) {
                list = list.filter { it.name.contains(searchQuery, ignoreCase = true) }
            }
            return list
        }

    val pinnedRecords: List<BaZiRecord>
        get() = filteredRecords.filter { it.isPinned }

    val otherRecords: List<BaZiRecord>
        get() = filteredRecords.filter { !it.isPinned }

    val isAnyDialogOpen: Boolean
        get() = activeDialog != null || recordToEdit != null || recordToDelete != null ||
                selectedRecord != null || managingTagsRecord != null
}

class BaZiViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(BaZiUiState())
    val uiState: StateFlow<BaZiUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val context = getApplication<Application>()
        val activeList = BaZiManager.loadList(context)
        val trashList = BaZiManager.loadTrashList(context)
        _uiState.update {
            it.copy(
                records = activeList,
                trashRecords = trashList
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSearchActiveChange(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active) }
    }

    fun onTagFilterSelect(tag: String?) {
        _uiState.update { it.copy(selectedTagFilter = tag) }
    }

    fun showDialog(dialogType: BaZiDialogType?) {
        _uiState.update { it.copy(activeDialog = dialogType) }
    }

    fun selectRecord(record: BaZiRecord?) {
        _uiState.update { it.copy(selectedRecord = record) }
    }

    fun editRecord(record: BaZiRecord?) {
        _uiState.update { it.copy(recordToEdit = record) }
    }

    fun confirmDeleteRecord(record: BaZiRecord?) {
        _uiState.update { it.copy(recordToDelete = record) }
    }

    fun manageTagsRecord(record: BaZiRecord?) {
        _uiState.update { it.copy(managingTagsRecord = record) }
    }

    fun saveRecord(record: BaZiRecord) {
        val context = getApplication<Application>()
        BaZiManager.addOrUpdateRecord(context, record)
        loadData()
        _uiState.update {
            it.copy(
                activeDialog = if (it.activeDialog == BaZiDialogType.ADD) null else it.activeDialog,
                recordToEdit = null
            )
        }
    }

    fun togglePin(recordId: Long): Boolean {
        val context = getApplication<Application>()
        val updated = BaZiManager.togglePinRecord(context, recordId)
        loadData()
        return updated.find { it.id == recordId }?.isPinned == true
    }

    fun moveToTrash(recordId: Long) {
        val context = getApplication<Application>()
        BaZiManager.moveToTrash(context, recordId)
        loadData()
        _uiState.update { it.copy(recordToDelete = null) }
    }

    fun restoreFromTrash(recordId: Long) {
        val context = getApplication<Application>()
        BaZiManager.restoreFromTrash(context, recordId)
        loadData()
    }

    fun permanentlyDeleteFromTrash(recordId: Long) {
        val context = getApplication<Application>()
        BaZiManager.permanentlyDeleteFromTrash(context, recordId)
        loadData()
    }

    fun emptyTrash() {
        val context = getApplication<Application>()
        BaZiManager.emptyTrash(context)
        loadData()
    }

    fun saveTags(record: BaZiRecord, tags: List<String>) {
        val context = getApplication<Application>()
        val updatedRecord = record.copy(tags = tags)
        BaZiManager.addOrUpdateRecord(context, updatedRecord)
        loadData()
        _uiState.update { it.copy(managingTagsRecord = null) }
    }
}
