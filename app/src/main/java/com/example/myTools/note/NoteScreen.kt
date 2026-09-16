package com.example.myTools.note

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myTools.MainActivity
import com.example.myTools.tools.DataManagementDialog
import com.example.myTools.ui.BlurryContainer
import com.example.myTools.ui.DataManagementMenuItem
import com.example.myTools.ui.DeleteConfirmDialog
import com.example.myTools.ui.ManageTagsDialog
import com.example.myTools.ui.SearchableTopBar
import com.example.myTools.ui.ShareAppMenuItem
import com.example.myTools.ui.TagChip
import com.example.myTools.ui.TrashDialog
import com.example.myTools.ui.TrashMenuItem
import com.example.myTools.ui.TrashedItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteScreen(
    onBack: (() -> Unit)? = null,
    viewModel: NoteViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }

    val notes = uiState.notes
    val isGridView = uiState.isGridView
    val isSearchActive = uiState.isSearchActive
    val searchQuery = uiState.searchQuery
    val editingNote = uiState.editingNote
    val deletingNote = uiState.deletingNote
    val managingTagsNote = uiState.managingTagsNote
    val selectedTagFilter = uiState.selectedTagFilter
    val tagSortMode = uiState.tagSortMode
    val openedTxtFile = uiState.openedTxtFile
    val trashNotes = uiState.trashNotes
    val showTrashDialog = uiState.activeDialog == NoteDialogType.TRASH
    val showDataManagementDialog = uiState.activeDialog == NoteDialogType.DATA_MANAGEMENT

    val allUniqueTags = uiState.allUniqueTags
    val filteredNotes = uiState.filteredNotes
    val pinnedNotes = uiState.pinnedNotes
    val otherNotes = uiState.otherNotes
    val isAnyDialogOpen = uiState.isAnyDialogOpen

    // 監聽外部文件管理器發送的 .txt 開啟請求 (ACTION_VIEW / ACTION_EDIT)
    val externalUri by MainActivity.externalTxtUri.collectAsState()

    LaunchedEffect(externalUri) {
        val uri = externalUri
        if (uri != null) {
            when (val result = TextFileLoader.readTextFromUri(context, uri)) {
                is TextFileLoader.Result.Success -> {
                    viewModel.setOpenedTxtFile(OpenedTxtFile(uri, result.fileName, result.content))
                }
                is TextFileLoader.Result.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
            MainActivity.clearExternalTxtUri()
        }
    }

    // 開啟手機文字檔案 Launcher
    val openTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            when (val result = TextFileLoader.readTextFromUri(context, uri)) {
                is TextFileLoader.Result.Success -> {
                    viewModel.setOpenedTxtFile(OpenedTxtFile(uri, result.fileName, result.content))
                }
                is TextFileLoader.Result.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    var isFabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isGridView, gridState, listState) {
        var previousIndex = if (isGridView) gridState.firstVisibleItemIndex else listState.firstVisibleItemIndex
        var previousOffset = if (isGridView) gridState.firstVisibleItemScrollOffset else listState.firstVisibleItemScrollOffset

        snapshotFlow {
            if (isGridView) {
                Triple(gridState.isScrollInProgress, gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
            } else {
                Triple(listState.isScrollInProgress, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
        }.collect { (_, currentIndex, currentOffset) ->
            if (currentIndex == 0 && currentOffset == 0) {
                isFabVisible = true
            } else {
                if (currentIndex < previousIndex) {
                    // 向下滑動 / 復原 FAB
                    isFabVisible = true
                } else if (currentIndex > previousIndex) {
                    // 向上滑動 / 隱藏 FAB
                    isFabVisible = false
                } else {
                    val diff = currentOffset - previousOffset
                    if (diff < -10) {
                        // 向下滑動 / 復原 FAB
                        isFabVisible = true
                    } else if (diff > 10) {
                        // 向上滑動 / 隱藏 FAB
                        isFabVisible = false
                    }
                }
            }
            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    LaunchedEffect(isAnyDialogOpen) {
        MainActivity.setAppBlurred(isAnyDialogOpen)
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BlurryContainer(isBlur = isAnyDialogOpen) {
                Column {
                    SearchableTopBar(
                        title = if (selectedTagFilter != null) "記事本 (#$selectedTagFilter)" else "記事本",
                        isSearchActive = isSearchActive,
                        onSearchActiveChange = { viewModel.onSearchActiveChange(it) },
                        searchQuery = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (onBack != null) {
                                    onBack()
                                } else {
                                    backDispatcher?.onBackPressed()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            // 切換單欄/雙欄檢視
                            IconButton(onClick = {
                                viewModel.setGridView(!isGridView)
                            }) {
                                Icon(
                                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                    contentDescription = "切換視圖",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "更多",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (tagSortMode) "預設按時間排序" else "按標籤排序") },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.toggleTagSortMode()
                                        },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                                    )
                                    if (selectedTagFilter != null) {
                                        DropdownMenuItem(
                                            text = { Text("清除標籤篩選") },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.onTagFilterSelect(null)
                                            },
                                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) }
                                        )
                                    }

                                    DataManagementMenuItem(
                                        text = "導出/導入筆記本",
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.showDialog(NoteDialogType.DATA_MANAGEMENT)
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text("查看/編輯文字檔案") },
                                        onClick = {
                                            menuExpanded = false
                                            try {
                                                openTxtLauncher.launch(arrayOf("text/*", "application/json", "application/xml", "application/javascript", "*/*"))
                                            } catch (_: Exception) {
                                                Toast.makeText(context, "無法開啟檔案選擇器", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) }
                                    )

                                    ShareAppMenuItem(
                                        onDismissRequest = { menuExpanded = false }
                                    )

                                    HorizontalDivider()
                                    TrashMenuItem(
                                        count = trashNotes.size,
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.showDialog(NoteDialogType.TRASH)
                                        }
                                    )
                                }
                            }
                        }
                    )

                    // 頂部標籤快速分類導向列
                    if (allUniqueTags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedTagFilter == null,
                                onClick = { viewModel.onTagFilterSelect(null) },
                                label = { Text("全部 (${notes.size})") },
                                leadingIcon = if (selectedTagFilter == null) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )

                            allUniqueTags.forEach { tag ->
                                val count = notes.count { it.getEffectiveTags().contains(tag) }
                                val isSelected = selectedTagFilter == tag
                                TagChip(
                                    tagName = "$tag ($count)",
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.onTagFilterSelect(if (isSelected) null else tag)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                BlurryContainer(isBlur = isAnyDialogOpen) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.setEditingNote(NoteRecord())
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        text = {
                            Text(
                                text = "新增筆記",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(18.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 6.dp
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        BlurryContainer(
            isBlur = isAnyDialogOpen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTagFilter != null) "沒有找到相關筆記" else "暫無筆記，點擊右下角 + 新增筆記",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (selectedTagFilter != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.onTagFilterSelect(null) }) {
                                Text("清除標籤篩選 (#$selectedTagFilter)")
                            }
                        }
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = { GridItemSpan(2) }) {
                                SectionHeader("置頂筆記")
                            }
                            items(pinnedNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { viewModel.setEditingNote(note) },
                                    onTogglePin = { viewModel.togglePin(note.id) },
                                    onManageTags = { viewModel.setManagingTagsNote(note) },
                                    onSelectTagFilter = { viewModel.onTagFilterSelect(it) },
                                    onSaveAsTxt = {
                                        val fn = NoteManager.saveNoteToDownloads(context, note)
                                        if (fn != null) {
                                            Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { viewModel.setDeletingNote(note) }
                                )
                            }
                        }

                        if (otherNotes.isNotEmpty()) {
                            if (pinnedNotes.isNotEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    SectionHeader("其它筆記")
                                }
                            }
                            items(otherNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { viewModel.setEditingNote(note) },
                                    onTogglePin = { viewModel.togglePin(note.id) },
                                    onManageTags = { viewModel.setManagingTagsNote(note) },
                                    onSelectTagFilter = { viewModel.onTagFilterSelect(it) },
                                    onSaveAsTxt = {
                                        val fn = NoteManager.saveNoteToDownloads(context, note)
                                        if (fn != null) {
                                            Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { viewModel.setDeletingNote(note) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (pinnedNotes.isNotEmpty()) {
                            item { SectionHeader("置頂筆記") }
                            items(pinnedNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { viewModel.setEditingNote(note) },
                                    onTogglePin = { viewModel.togglePin(note.id) },
                                    onManageTags = { viewModel.setManagingTagsNote(note) },
                                    onSelectTagFilter = { viewModel.onTagFilterSelect(it) },
                                    onSaveAsTxt = {
                                        val fn = NoteManager.saveNoteToDownloads(context, note)
                                        if (fn != null) {
                                            Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { viewModel.setDeletingNote(note) }
                                )
                            }
                        }

                        if (otherNotes.isNotEmpty()) {
                            if (pinnedNotes.isNotEmpty()) {
                                item { SectionHeader("其它筆記") }
                            }
                            items(otherNotes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { viewModel.setEditingNote(note) },
                                    onTogglePin = { viewModel.togglePin(note.id) },
                                    onManageTags = { viewModel.setManagingTagsNote(note) },
                                    onSelectTagFilter = { viewModel.onTagFilterSelect(it) },
                                    onSaveAsTxt = {
                                        val fn = NoteManager.saveNoteToDownloads(context, note)
                                        if (fn != null) {
                                            Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { viewModel.setDeletingNote(note) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 筆記編輯對話框 / 頁面
    editingNote?.let { note ->
        NoteEditDialog(
            note = note,
            onDismiss = { viewModel.setEditingNote(null) },
            onSave = { updatedNote ->
                viewModel.saveNote(updatedNote)
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            },
            onDelete = { noteToDelete ->
                viewModel.setEditingNote(null)
                viewModel.setDeletingNote(noteToDelete)
            },
            onSaveAsTxt = { noteToSave ->
                val fn = NoteManager.saveNoteToDownloads(context, noteToSave)
                if (fn != null) {
                    Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // 標籤管理對話框
    managingTagsNote?.let { note ->
        ManageTagsDialog(
            title = "管理「${note.title.ifBlank { "未命名筆記" }}」的標籤",
            currentTags = note.getEffectiveTags(),
            allAppTags = allUniqueTags,
            onDismiss = { viewModel.setManagingTagsNote(null) },
            onSaveTags = { updatedTags ->
                viewModel.saveTags(note, updatedTags)
                Toast.makeText(context, "已更新標籤", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 移至回收站確認對話框
    deletingNote?.let { note ->
        DeleteConfirmDialog(
            title = "移至回收站",
            message = "確定要將「${note.title.ifBlank { "未命名筆記" }}」移至回收站嗎？稍後可隨時從回收站還原。",
            onDismiss = { viewModel.setDeletingNote(null) },
            onConfirm = {
                viewModel.moveToTrash(note.id)
                Toast.makeText(context, "已移至回收站", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 回收站對話框
    if (showTrashDialog) {
        val trashedItems = remember(trashNotes) {
            trashNotes.map { note ->
                TrashedItem(
                    id = note.id,
                    title = note.title.ifBlank { "未命名筆記" },
                    subtitle = note.content,
                    deletedAt = note.deletedAt,
                    rawItem = note
                )
            }
        }

        TrashDialog(
            dialogTitle = "記事本回收站",
            trashedItems = trashedItems,
            onDismiss = { viewModel.showDialog(null) },
            onRestore = { trashedItem ->
                viewModel.restoreFromTrash(trashedItem.id)
                Toast.makeText(context, "已還原「${trashedItem.title}」", Toast.LENGTH_SHORT).show()
            },
            onPermanentlyDelete = { trashedItem ->
                viewModel.permanentlyDeleteFromTrash(trashedItem.id)
                Toast.makeText(context, "已徹底刪除「${trashedItem.title}」", Toast.LENGTH_SHORT).show()
            },
            onEmptyTrash = {
                viewModel.emptyTrash()
                Toast.makeText(context, "已清空回收站", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 手機 TXT 內容查看與編輯器
    openedTxtFile?.let { file ->
        TxtEditorDialog(
            fileName = file.fileName,
            initialContent = file.content,
            onDismiss = { viewModel.setOpenedTxtFile(null) },
            onImportAsNote = { importedText ->
                val count = viewModel.importNotesFromTxt(importedText, file.fileName.removeSuffix(".txt"))
                viewModel.setOpenedTxtFile(null)
                Toast.makeText(context, "成功匯入 $count 筆筆記到記事本！", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 數據管理與備份對話框 (包含全量與單獨筆記本導出/導入)
    if (showDataManagementDialog) {
        DataManagementDialog(onDismiss = {
            viewModel.showDialog(null)
            viewModel.loadData()
        })
    }
}
