package com.example.myTools.note

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun NoteScreen() {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(NoteManager.loadList(context)) }
    var isGridView by rememberSaveable { mutableStateOf(NoteManager.isGridView(context)) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var editingNote by remember { mutableStateOf<NoteRecord?>(null) }
    var deletingNote by remember { mutableStateOf<NoteRecord?>(null) }
    var managingTagsNote by remember { mutableStateOf<NoteRecord?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var tagSortMode by remember { mutableStateOf(false) }

    var showDataManagementDialog by remember { mutableStateOf(false) }
    var showTrashDialog by remember { mutableStateOf(false) }
    var trashNotes by remember { mutableStateOf(NoteManager.loadTrashList(context)) }
    var openedTxtFile by remember { mutableStateOf<OpenedTxtFile?>(null) }

    // 彙整目前所有筆記中的不重複標籤
    val allUniqueTags = remember(notes) {
        notes.flatMap { it.getEffectiveTags() }.distinct().sorted()
    }

    // 監聽外部文件管理器發送的 .txt 開啟請求 (ACTION_VIEW / ACTION_EDIT)
    val externalUri by MainActivity.externalTxtUri.collectAsState()

    LaunchedEffect(externalUri) {
        val uri = externalUri
        if (uri != null) {
            when (val result = TextFileLoader.readTextFromUri(context, uri)) {
                is TextFileLoader.Result.Success -> {
                    openedTxtFile = OpenedTxtFile(uri, result.fileName, result.content)
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
                    openedTxtFile = OpenedTxtFile(uri, result.fileName, result.content)
                }
                is TextFileLoader.Result.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val filteredNotes = remember(searchQuery, selectedTagFilter, tagSortMode, notes) {
        var result = if (searchQuery.isBlank()) {
            notes
        } else {
            notes.filter { 
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

        result
    }

    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val otherNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

    val isAnyDialogOpen = editingNote != null || deletingNote != null || showDataManagementDialog || openedTxtFile != null || managingTagsNote != null || showTrashDialog

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

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BlurryContainer(isBlur = isAnyDialogOpen) {
                Column {
                    SearchableTopBar(
                        title = if (selectedTagFilter != null) "記事本 (#$selectedTagFilter)" else "記事本",
                        isSearchActive = isSearchActive,
                        onSearchActiveChange = { isSearchActive = it },
                        searchQuery = searchQuery,
                        onQueryChange = { searchQuery = it },
                        actions = {
                            // 切換單欄/雙欄檢視
                            IconButton(onClick = {
                                val newGridView = !isGridView
                                isGridView = newGridView
                                NoteManager.setGridView(context, newGridView)
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
                                            tagSortMode = !tagSortMode
                                        },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                                    )
                                    if (selectedTagFilter != null) {
                                        DropdownMenuItem(
                                            text = { Text("清除標籤篩選") },
                                            onClick = {
                                                menuExpanded = false
                                                selectedTagFilter = null
                                            },
                                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) }
                                        )
                                    }

                                    DataManagementMenuItem(
                                        text = "導出/導入筆記本",
                                        onClick = {
                                            menuExpanded = false
                                            showDataManagementDialog = true
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
                                            trashNotes = NoteManager.loadTrashList(context)
                                            showTrashDialog = true
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
                                onClick = { selectedTagFilter = null },
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
                                        selectedTagFilter = if (isSelected) null else tag
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
                            editingNote = NoteRecord()
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
                            TextButton(onClick = { selectedTagFilter = null }) {
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
                                    onClick = { editingNote = note },
                                    onTogglePin = {
                                        NoteManager.togglePinRecord(context, note.id)
                                        notes = NoteManager.loadList(context)
                                    },
                                    onManageTags = { managingTagsNote = note },
                                    onSelectTagFilter = { selectedTagFilter = it },
                                    onSaveAsTxt = {
                                        val fn = NoteManager.saveNoteToDownloads(context, note)
                                        if (fn != null) {
                                            Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { deletingNote = note }
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
                                    onClick = { editingNote = note },
                                    onTogglePin = {
                                        NoteManager.togglePinRecord(context, note.id)
                                        notes = NoteManager.loadList(context)
                                    },
                                    onManageTags = { managingTagsNote = note },
                                    onSelectTagFilter = { selectedTagFilter = it },
                                    onSaveAsTxt = {
                                        val fn = NoteManager.saveNoteToDownloads(context, note)
                                        if (fn != null) {
                                            Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { deletingNote = note }
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
                                    onClick = { editingNote = note },
                                    onTogglePin = {
                                        NoteManager.togglePinRecord(context, note.id)
                                        notes = NoteManager.loadList(context)
                                    },
                                    onManageTags = { managingTagsNote = note },
                                    onSelectTagFilter = { selectedTagFilter = it },
                                    onSaveAsTxt = {
                                        val fn = NoteManager.saveNoteToDownloads(context, note)
                                        if (fn != null) {
                                            Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { deletingNote = note }
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
                                    onClick = { editingNote = note },
                                    onTogglePin = {
                                        NoteManager.togglePinRecord(context, note.id)
                                        notes = NoteManager.loadList(context)
                                    },
                                    onManageTags = { managingTagsNote = note },
                                    onSelectTagFilter = { selectedTagFilter = it },
                                    onSaveAsTxt = {
                                        val fn = NoteManager.saveNoteToDownloads(context, note)
                                        if (fn != null) {
                                            Toast.makeText(context, "已另存至下載文件夾：$fn", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "儲存失敗", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { deletingNote = note }
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
            onDismiss = { editingNote = null },
            onSave = { updatedNote ->
                NoteManager.addOrUpdateRecord(context, updatedNote)
                notes = NoteManager.loadList(context)
                editingNote = null
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            },
            onDelete = { noteToDelete ->
                editingNote = null
                deletingNote = noteToDelete
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
            onDismiss = { managingTagsNote = null },
            onSaveTags = { updatedTags ->
                val updatedNote = note.copy(tags = updatedTags)
                NoteManager.addOrUpdateRecord(context, updatedNote)
                notes = NoteManager.loadList(context)
                managingTagsNote = null
                Toast.makeText(context, "已更新標籤", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 移至回收站確認對話框
    deletingNote?.let { note ->
        DeleteConfirmDialog(
            title = "移至回收站",
            message = "確定要將「${note.title.ifBlank { "未命名筆記" }}」移至回收站嗎？稍後可隨時從回收站還原。",
            onDismiss = { deletingNote = null },
            onConfirm = {
                NoteManager.moveToTrash(context, note.id)
                notes = NoteManager.loadList(context)
                trashNotes = NoteManager.loadTrashList(context)
                deletingNote = null
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
            onDismiss = { showTrashDialog = false },
            onRestore = { trashedItem ->
                NoteManager.restoreFromTrash(context, trashedItem.id)
                notes = NoteManager.loadList(context)
                trashNotes = NoteManager.loadTrashList(context)
                Toast.makeText(context, "已還原「${trashedItem.title}」", Toast.LENGTH_SHORT).show()
            },
            onPermanentlyDelete = { trashedItem ->
                NoteManager.permanentlyDeleteFromTrash(context, trashedItem.id)
                trashNotes = NoteManager.loadTrashList(context)
                Toast.makeText(context, "已徹底刪除「${trashedItem.title}」", Toast.LENGTH_SHORT).show()
            },
            onEmptyTrash = {
                NoteManager.emptyTrash(context)
                trashNotes = emptyList()
                Toast.makeText(context, "已清空回收站", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 手機 TXT 內容查看與編輯器
    openedTxtFile?.let { file ->
        TxtEditorDialog(
            uri = file.uri,
            fileName = file.fileName,
            initialContent = file.content,
            onDismiss = { openedTxtFile = null },
            onImportAsNote = { importedText ->
                val count = NoteManager.importNotesFromTxt(context, importedText, file.fileName.removeSuffix(".txt"))
                notes = NoteManager.loadList(context)
                openedTxtFile = null
                Toast.makeText(context, "成功匯入 $count 筆筆記到記事本！", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 數據管理與備份對話框 (包含全量與單獨筆記本導出/導入)
    if (showDataManagementDialog) {
        DataManagementDialog(onDismiss = {
            showDataManagementDialog = false
            notes = NoteManager.loadList(context)
        })
    }
}
