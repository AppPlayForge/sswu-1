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
import com.example.myTools.ui.DeleteConfirmDialog
import com.example.myTools.ui.SearchableTopBar
import kotlin.math.abs

data class OpenedTxtFile(
    val uri: Uri,
    val fileName: String,
    val content: String
)

data class NoteColorOption(val name: String, val lightColor: Color, val darkColor: Color, val hex: String)

val NOTE_COLORS = listOf(
    NoteColorOption("預設", Color.Transparent, Color.Transparent, ""),
    NoteColorOption("珊瑚紅", Color(0xFFFCE8E6), Color(0xFF4A2020), "#FCE8E6"),
    NoteColorOption("暖活力橙", Color(0xFFFEF0D5), Color(0xFF4D3810), "#FEF0D5"),
    NoteColorOption("檸檬黃", Color(0xFFFFF8D6), Color(0xFF4D4610), "#FFF8D6"),
    NoteColorOption("薄荷綠", Color(0xFFE6F4EA), Color(0xFF1B3D2B), "#E6F4EA"),
    NoteColorOption("天空藍", Color(0xFFE8F0FE), Color(0xFF1B2F4E), "#E8F0FE"),
    NoteColorOption("丁香紫", Color(0xFFF3E8FD), Color(0xFF3B1E54), "#F3E8FD"),
    NoteColorOption("櫻花粉", Color(0xFFFDE8F3), Color(0xFF4E1D3B), "#FDE8F3")
)

data class TagColor(
    val containerColor: Color,
    val contentColor: Color
)

val TAG_COLOR_PALETTE = listOf(
    TagColor(Color(0xFFE8DEF8), Color(0xFF1D192B)), // 柔紫
    TagColor(Color(0xFFD3E3FD), Color(0xFF041E49)), // 晴藍
    TagColor(Color(0xFFC2F0C2), Color(0xFF0A380A)), // 薄荷綠
    TagColor(Color(0xFFFFDBCF), Color(0xFF380D00)), // 珊瑚橙
    TagColor(Color(0xFFFFF0B3), Color(0xFF332A00)), // 檸檬黃
    TagColor(Color(0xFFFFD8EC), Color(0xFF311027)), // 櫻花粉
    TagColor(Color(0xFFC7F0F0), Color(0xFF003737)), // 湖水綠
    TagColor(Color(0xFFE2E2E2), Color(0xFF1B1B1B))  // 高雅灰
)

fun getTagColor(tagName: String): TagColor {
    val cleanName = tagName.removePrefix("#").trim()
    val index = abs(cleanName.hashCode()) % TAG_COLOR_PALETTE.size
    return TAG_COLOR_PALETTE[index]
}

@Composable
fun TagChip(
    tagName: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val tagColor = getTagColor(tagName)
    val displayText = if (tagName.startsWith("#")) tagName else "#$tagName"

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else tagColor.containerColor,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else tagColor.contentColor,
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            if (onDelete != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "刪除標籤",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onDelete() }
                )
            }
        }
    }
}

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

                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("導出/導入筆記本") },
                                        onClick = {
                                            menuExpanded = false
                                            showDataManagementDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = null) }
                                    )

                                    HorizontalDivider()
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

                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("垃圾桶 (${trashNotes.size})") },
                                        onClick = {
                                            menuExpanded = false
                                            trashNotes = NoteManager.loadTrashList(context)
                                            showTrashDialog = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
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
        NoteEditorDialog(
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
        NoteTagsDialog(
            note = note,
            allAppTags = allUniqueTags,
            onDismiss = { managingTagsNote = null },
            onSaveTags = { updatedNote ->
                NoteManager.addOrUpdateRecord(context, updatedNote)
                notes = NoteManager.loadList(context)
                managingTagsNote = null
                Toast.makeText(context, "已更新標籤", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 移至垃圾桶確認對話框
    deletingNote?.let { note ->
        DeleteConfirmDialog(
            title = "移至垃圾桶",
            message = "確定要將「${note.title.ifBlank { "未命名筆記" }}」移至垃圾桶嗎？稍後可隨時從垃圾桶恢復。",
            onDismiss = { deletingNote = null },
            onConfirm = {
                NoteManager.moveToTrash(context, note.id)
                notes = NoteManager.loadList(context)
                trashNotes = NoteManager.loadTrashList(context)
                deletingNote = null
                Toast.makeText(context, "已移至垃圾桶，可以在垃圾桶中恢復", Toast.LENGTH_LONG).show()
            }
        )
    }

    // 垃圾桶對話框
    if (showTrashDialog) {
        RecycleBinDialog(
            onDismiss = { showTrashDialog = false },
            onNotesUpdated = {
                notes = NoteManager.loadList(context)
                trashNotes = NoteManager.loadTrashList(context)
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

@Composable
fun NoteTagsDialog(
    note: NoteRecord,
    allAppTags: List<String>,
    onDismiss: () -> Unit,
    onSaveTags: (NoteRecord) -> Unit
) {
    var newTagInput by remember { mutableStateOf("") }
    var currentTags by remember { mutableStateOf(note.getEffectiveTags()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理筆記標籤", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "已設定的標籤：",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (currentTags.isEmpty()) {
                    Text(
                        text = "暫無標籤 (可在內容中使用 #標籤 自動添加)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentTags.forEach { tag ->
                            TagChip(
                                tagName = tag,
                                onDelete = {
                                    currentTags = currentTags.filter { it != tag }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    label = { Text("新增標籤") },
                    placeholder = { Text("輸入標籤名稱，例如：工作") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val clean = newTagInput.removePrefix("#").trim()
                            if (clean.isNotBlank() && !currentTags.contains(clean)) {
                                currentTags = currentTags + clean
                                newTagInput = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "添加標籤")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (allAppTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "常用標籤：",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allAppTags.forEach { tag ->
                            val isAdded = currentTags.contains(tag)
                            FilterChip(
                                selected = isAdded,
                                onClick = {
                                    currentTags = if (isAdded) {
                                        currentTags.filter { it != tag }
                                    } else {
                                        currentTags + tag
                                    }
                                },
                                label = { Text("#$tag") },
                                leadingIcon = if (isAdded) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val updatedTags = currentTags.distinct()
                val updatedNote = note.copy(tags = updatedTags)
                onSaveTags(updatedNote)
                onDismiss()
            }) {
                Text("確定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: NoteRecord,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onManageTags: () -> Unit,
    onSelectTagFilter: (String) -> Unit,
    onSaveAsTxt: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val colorOpt = NOTE_COLORS.find { it.hex == note.colorHex }
    val cardBg = when {
        colorOpt != null && colorOpt.hex.isNotEmpty() -> if (isDark) colorOpt.darkColor else colorOpt.lightColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    var cardMenuExpanded by remember { mutableStateOf(false) }
    val effectiveTags = remember(note.title, note.content, note.tags) { note.getEffectiveTags() }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { cardMenuExpanded = true }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = cardBg,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (colorOpt != null && colorOpt.hex.isNotEmpty()) {
                if (isDark) {
                    colorOpt.lightColor.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                }
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = note.content.take(20).ifBlank { "無標題" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box {
                    if (note.isPinned) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "取消置頂",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = cardMenuExpanded,
                        onDismissRequest = { cardMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (note.isPinned) "取消置頂" else "置頂筆記") },
                            onClick = {
                                cardMenuExpanded = false
                                onTogglePin()
                            },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("標籤") },
                            onClick = {
                                cardMenuExpanded = false
                                onManageTags()
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("複製內容") },
                            onClick = {
                                cardMenuExpanded = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val copyText = if (note.title.isNotBlank()) "${note.title}\n\n${note.content}" else note.content
                                val clip = ClipData.newPlainText("Note Content", copyText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "已複製筆記內容到剪貼簿", Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("另存為 .txt 文件") },
                            onClick = {
                                cardMenuExpanded = false
                                onSaveAsTxt()
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("刪除筆記", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                cardMenuExpanded = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (effectiveTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    effectiveTags.forEach { tag ->
                        TagChip(
                            tagName = tag,
                            onClick = { onSelectTagFilter(tag) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.getFormattedDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorDialog(
    note: NoteRecord,
    onDismiss: () -> Unit,
    onSave: (NoteRecord) -> Unit,
    onDelete: (NoteRecord) -> Unit,
    onSaveAsTxt: (NoteRecord) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    var isPinned by remember { mutableStateOf(note.isPinned) }
    var selectedColorHex by remember { mutableStateOf(note.colorHex ?: "") }

    val isDark = isSystemInDarkTheme()
    val colorOpt = NOTE_COLORS.find { it.hex == selectedColorHex }
    val dialogBg = when {
        colorOpt != null && colorOpt.hex.isNotEmpty() -> if (isDark) colorOpt.darkColor else colorOpt.lightColor
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val detectedTags = remember(title, content) {
        NoteRecord.extractHashtags("$title $content")
    }

    Dialog(
        onDismissRequest = {
            if (title.isNotBlank() || content.isNotBlank()) {
                onSave(note.copy(title = title, content = content, isPinned = isPinned, colorHex = selectedColorHex.ifEmpty { null }))
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 頂部導航列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (title.isNotBlank() || content.isNotBlank()) {
                            onSave(note.copy(title = title, content = content, isPinned = isPinned, colorHex = selectedColorHex.ifEmpty { null }))
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "置頂",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = {
                            val currentNote = note.copy(title = title, content = content, isPinned = isPinned, colorHex = selectedColorHex.ifEmpty { null })
                            onSaveAsTxt(currentNote)
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "另存為 TXT")
                        }

                        if (note.id != 0L) {
                            IconButton(onClick = { onDelete(note) }) {
                                Icon(Icons.Default.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = {
                                onSave(note.copy(title = title, content = content, isPinned = isPinned, colorHex = selectedColorHex.ifEmpty { null }))
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("保存")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 標題輸入框
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("標題", style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.outline)) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // 內文輸入框
                Box(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("記事文本 (輸入 #標籤 可自動建立分類)...", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.outline)) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (detectedTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "已識別標籤：",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            detectedTags.forEach { tag ->
                                TagChip(tagName = tag)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 工具列與顏色選擇器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            content = if (content.endsWith(" ") || content.isEmpty() || content.endsWith("\n")) {
                                "$content#"
                            } else {
                                "$content #"
                            }
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tag,
                            contentDescription = "插入 #標籤",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "顏色",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NOTE_COLORS.forEach { opt ->
                            val isSelected = selectedColorHex == opt.hex
                            val circleColor = if (opt.hex.isEmpty()) MaterialTheme.colorScheme.surfaceVariant else if (isDark) opt.darkColor else opt.lightColor

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = opt.hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${content.length} 字",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TxtEditorDialog(
    uri: Uri,
    fileName: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onImportAsNote: (String) -> Unit
) {
    val context = LocalContext.current
    var content by remember { mutableStateOf(initialContent) }
    var isEditMode by remember { mutableStateOf(false) }
    var isModified by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 標題與操作按鈕
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "字數: ${content.length} | 行數: ${content.lines().size}${if (isModified) " (已修改)" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isModified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isEditMode = !isEditMode }) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = if (isEditMode) "切換為查看模式" else "切換為編輯模式",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "關閉")
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // 內文展示 / 編輯區
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    if (isEditMode) {
                        TextField(
                            value = content,
                            onValueChange = {
                                content = it
                                isModified = true
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                fontSize = 15.sp
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 22.sp,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 底部操作列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("TXT Content", content)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已複製全部文字到剪貼簿", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("複製全文")
                    }

                    Button(
                        onClick = { onImportAsNote(content) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("匯入為記事本")
                    }
                }
            }
        }
    }
}

@Composable
fun RecycleBinDialog(
    onDismiss: () -> Unit,
    onNotesUpdated: () -> Unit
) {
    val context = LocalContext.current
    var trashNotes by remember { mutableStateOf(NoteManager.loadTrashList(context)) }
    var showEmptyConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 標題列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "垃圾桶 (${trashNotes.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (trashNotes.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showEmptyConfirm = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "清空垃圾桶",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "關閉")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 提示標語
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "垃圾桶內的筆記可隨時恢復，清空後將無法復原。",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (trashNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "垃圾桶是空的",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(trashNotes, key = { it.id }) { note ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = note.title.ifBlank { note.content.take(20).ifBlank { "未命名筆記" } },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (note.content.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 20.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = note.getFormattedDeletedDate(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    NoteManager.restoreFromTrash(context, note.id)
                                                    trashNotes = NoteManager.loadTrashList(context)
                                                    onNotesUpdated()
                                                    Toast.makeText(context, "已恢復筆記「${note.title.ifBlank { "筆記" }}」", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restore,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "恢復",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    NoteManager.permanentlyDeleteFromTrash(context, note.id)
                                                    trashNotes = NoteManager.loadTrashList(context)
                                                    onNotesUpdated()
                                                    Toast.makeText(context, "已永久刪除", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteForever,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "刪除",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEmptyConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyConfirm = false },
            title = { Text("清空垃圾桶", fontWeight = FontWeight.Bold) },
            text = { Text("確定要永久刪除垃圾桶中的所有筆記嗎？此操作無法恢復。") },
            confirmButton = {
                Button(
                    onClick = {
                        NoteManager.emptyTrash(context)
                        trashNotes = emptyList()
                        showEmptyConfirm = false
                        onNotesUpdated()
                        Toast.makeText(context, "已清空垃圾桶", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
