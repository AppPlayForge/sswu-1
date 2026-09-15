package com.example.myTools.note

import android.content.Context
import androidx.lifecycle.ViewModel
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

class NoteViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(NoteUiState())
    val uiState: StateFlow<NoteUiState> = _uiState.asStateFlow()

    fun loadData(context: Context) {
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

    fun setGridView(context: Context, isGrid: Boolean) {
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

    fun saveNote(context: Context, note: NoteRecord) {
        NoteManager.addOrUpdateRecord(context, note)
        loadData(context)
        _uiState.update { it.copy(editingNote = null) }
    }

    fun togglePin(context: Context, noteId: Long) {
        NoteManager.togglePinRecord(context, noteId)
        loadData(context)
    }

    fun moveToTrash(context: Context, noteId: Long) {
        NoteManager.moveToTrash(context, noteId)
        loadData(context)
        _uiState.update { it.copy(deletingNote = null) }
    }

    fun restoreFromTrash(context: Context, noteId: Long) {
        NoteManager.restoreFromTrash(context, noteId)
        loadData(context)
    }

    fun permanentlyDeleteFromTrash(context: Context, noteId: Long) {
        NoteManager.permanentlyDeleteFromTrash(context, noteId)
        loadData(context)
    }

    fun emptyTrash(context: Context) {
        NoteManager.emptyTrash(context)
        loadData(context)
    }

    fun saveTags(context: Context, note: NoteRecord, tags: List<String>) {
        val updatedNote = note.copy(tags = tags)
        NoteManager.addOrUpdateRecord(context, updatedNote)
        loadData(context)
        _uiState.update { it.copy(managingTagsNote = null) }
    }

    fun importNotesFromTxt(context: Context, content: String, defaultTitle: String): Int {
        val count = NoteManager.importNotesFromTxt(context, content, defaultTitle)
        if (count > 0) {
            loadData(context)
        }
        return count
    }
}
