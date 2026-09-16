package com.example.myTools.note

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class NoteDialogType {
    DATA_MANAGEMENT,
    TRASH
}

data class NoteUiState(
    val notes: List<NoteRecord> = emptyList(),
    val trashNotes: List<NoteRecord> = emptyList(),
    val isGridView: Boolean = true,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedTagFilter: String? = null,
    val tagSortMode: Boolean = false,
    val editingNote: NoteRecord? = null,
    val deletingNote: NoteRecord? = null,
    val openedTxtFile: OpenedTxtFile? = null,
    val managingTagsNote: NoteRecord? = null,
    val activeDialog: NoteDialogType? = null
) {
    val allUniqueTags: List<String>
        get() = notes.flatMap { it.getEffectiveTags() }.distinct().sorted()

    val filteredNotes: List<NoteRecord>
        get() {
            var result = notes

            if (searchQuery.isNotBlank()) {
                result = result.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.content.contains(searchQuery, ignoreCase = true) ||
                            it.getEffectiveTags().any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                }
            }

            if (selectedTagFilter != null) {
                result = result.filter { it.getEffectiveTags().contains(selectedTagFilter) }
            }

            if (tagSortMode) {
                result = result.sortedWith(
                    compareByDescending<NoteRecord> { it.isPinned }
                        .thenBy { it.getEffectiveTags().firstOrNull() ?: "zzz" }
                        .thenByDescending { it.updatedAt }
                )
            }

            return result
        }

    val pinnedNotes: List<NoteRecord>
        get() = filteredNotes.filter { it.isPinned }

    val otherNotes: List<NoteRecord>
        get() = filteredNotes.filter { !it.isPinned }

    val isAnyDialogOpen: Boolean
        get() = editingNote != null || deletingNote != null || activeDialog != null ||
                openedTxtFile != null || managingTagsNote != null
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val context = getApplication<Application>()
        val activeList = NoteManager.loadList(context)
        val trashList = NoteManager.loadTrashList(context)
        val isGrid = NoteManager.isGridView(context)
        _uiState.update {
            it.copy(
                notes = activeList,
                trashNotes = trashList,
                isGridView = isGrid
            )
        }
    }

    fun setGridView(isGrid: Boolean) {
        val context = getApplication<Application>()
        NoteManager.setGridView(context, isGrid)
        _uiState.update { it.copy(isGridView = isGrid) }
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

    fun toggleTagSortMode() {
        _uiState.update { it.copy(tagSortMode = !it.tagSortMode) }
    }

    fun showDialog(dialogType: NoteDialogType?) {
        _uiState.update { it.copy(activeDialog = dialogType) }
    }

    fun setEditingNote(note: NoteRecord?) {
        _uiState.update { it.copy(editingNote = note) }
    }

    fun setDeletingNote(note: NoteRecord?) {
        _uiState.update { it.copy(deletingNote = note) }
    }

    fun setOpenedTxtFile(file: OpenedTxtFile?) {
        _uiState.update { it.copy(openedTxtFile = file) }
    }

    fun setManagingTagsNote(note: NoteRecord?) {
        _uiState.update { it.copy(managingTagsNote = note) }
    }

    fun saveNote(note: NoteRecord) {
        val context = getApplication<Application>()
        NoteManager.addOrUpdateRecord(context, note)
        loadData()
        _uiState.update { it.copy(editingNote = null) }
    }

    fun togglePin(noteId: Long) {
        val context = getApplication<Application>()
        NoteManager.togglePinRecord(context, noteId)
        loadData()
    }

    fun moveToTrash(noteId: Long) {
        val context = getApplication<Application>()
        NoteManager.moveToTrash(context, noteId)
        loadData()
        _uiState.update { it.copy(deletingNote = null) }
    }

    fun restoreFromTrash(noteId: Long) {
        val context = getApplication<Application>()
        NoteManager.restoreFromTrash(context, noteId)
        loadData()
    }

    fun permanentlyDeleteFromTrash(noteId: Long) {
        val context = getApplication<Application>()
        NoteManager.permanentlyDeleteFromTrash(context, noteId)
        loadData()
    }

    fun emptyTrash() {
        val context = getApplication<Application>()
        NoteManager.emptyTrash(context)
        loadData()
    }

    fun saveTags(note: NoteRecord, tags: List<String>) {
        val context = getApplication<Application>()
        val updatedNote = note.copy(tags = tags)
        NoteManager.addOrUpdateRecord(context, updatedNote)
        loadData()
        _uiState.update { it.copy(managingTagsNote = null) }
    }

    fun importNotesFromTxt(content: String, defaultTitle: String): Int {
        val context = getApplication<Application>()
        val count = NoteManager.importNotesFromTxt(context, content, defaultTitle)
        if (count > 0) {
            loadData()
        }
        return count
    }
}
