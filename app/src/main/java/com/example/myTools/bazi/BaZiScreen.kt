package com.example.myTools.bazi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myTools.MainActivity
import com.example.myTools.tools.AppSettingsDialog
import com.example.myTools.tools.DataManagementDialog
import com.example.myTools.ui.BlurryContainer
import com.example.myTools.ui.DataManagementMenuItem
import com.example.myTools.ui.DeleteConfirmDialog
import com.example.myTools.ui.ManageTagsDialog
import com.example.myTools.ui.SearchableTopBar
import com.example.myTools.ui.ShareAppMenuItem
import com.example.myTools.ui.TagFilterRow
import com.example.myTools.ui.TrashDialog
import com.example.myTools.ui.TrashMenuItem
import com.example.myTools.ui.TrashedItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaZiScreen(
    onBack: (() -> Unit)? = null,
    viewModel: BaZiViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var menuExpanded by remember { mutableStateOf(false) }

    if (onBack != null) {
        BackHandler(enabled = !uiState.isSearchActive) {
            onBack()
        }
    }

    val listState = rememberLazyListState()
    var isFabVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset

        snapshotFlow {
            Triple(listState.isScrollInProgress, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }.collect { (isScrollInProgress, currentIndex, currentOffset) ->
            if (currentIndex == 0 && currentOffset == 0) {
                isFabVisible = true
            } else if (isScrollInProgress) {
                if (currentIndex > previousIndex) {
                    isFabVisible = false
                } else if (currentIndex < previousIndex) {
                    isFabVisible = true
                } else {
                    val diff = currentOffset - previousOffset
                    if (diff > 12) {
                        isFabVisible = false
                    } else if (diff < -12) {
                        isFabVisible = true
                    }
                }
            }
            previousIndex = currentIndex
            previousOffset = currentOffset
        }
    }

    LaunchedEffect(uiState.isAnyDialogOpen) {
        MainActivity.setAppBlurred(uiState.isAnyDialogOpen)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BlurryContainer(isBlur = uiState.isAnyDialogOpen) {
                SearchableTopBar(
                    title = "八字命盤",
                    isSearchActive = uiState.isSearchActive,
                    onSearchActiveChange = { viewModel.onSearchActiveChange(it) },
                    searchQuery = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
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
                                DataManagementMenuItem(
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.showDialog(BaZiDialogType.DATA_MANAGEMENT)
                                    }
                                )

                                ShareAppMenuItem(
                                    onDismissRequest = { menuExpanded = false }
                                )

                                HorizontalDivider()
                                TrashMenuItem(
                                    count = uiState.trashRecords.size,
                                    onClick = {
                                        menuExpanded = false
                                        viewModel.showDialog(BaZiDialogType.TRASH)
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                BlurryContainer(isBlur = uiState.isAnyDialogOpen) {
                    Surface(
                        modifier = Modifier
                            .size(96.dp)
                            .combinedClickable(
                                onClick = { viewModel.showDialog(BaZiDialogType.ADD) }
                            ),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "添加八字",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        BlurryContainer(
            isBlur = uiState.isAnyDialogOpen,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 標籤篩選橫條
                TagFilterRow(
                    allTags = uiState.allUniqueTags,
                    selectedTag = uiState.selectedTagFilter,
                    onTagSelected = { viewModel.onTagFilterSelect(it) }
                )

                if (uiState.filteredRecords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val emptyText = if (uiState.searchQuery.isEmpty() && uiState.selectedTagFilter == null) {
                            "暫無紀錄，請點擊右下角按鈕添加"
                        } else {
                            "未找到匹配的紀錄"
                        }
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.pinnedRecords.isNotEmpty()) {
                            item(key = "header_pinned") {
                                SectionHeader("置頂命盤")
                            }
                            items(uiState.pinnedRecords, key = { it.id }) { record ->
                                BaZiRecordItem(
                                    record = record,
                                    onClick = { viewModel.selectRecord(record) },
                                    onTogglePin = {
                                        val isPinnedNow = viewModel.togglePin(record.id)
                                        Toast.makeText(
                                            context,
                                            if (isPinnedNow) "已置頂 ${record.name}" else "已取消置頂 ${record.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onManageTags = { viewModel.manageTagsRecord(record) },
                                    onSelectTagFilter = { tag -> viewModel.onTagFilterSelect(tag) },
                                    onCopyInfo = {
                                        val info = formatBaZiInfo(record)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("BaZi Chart Info", info)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(
                                            context,
                                            "已複製 ${record.name} 的命盤資訊",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onEdit = { viewModel.editRecord(record) },
                                    onDelete = { viewModel.confirmDeleteRecord(record) }
                                )
                            }
                        }

                        if (uiState.otherRecords.isNotEmpty()) {
                            if (uiState.pinnedRecords.isNotEmpty()) {
                                item(key = "header_other") {
                                    SectionHeader("其它命盤")
                                }
                            }
                            items(uiState.otherRecords, key = { it.id }) { record ->
                                BaZiRecordItem(
                                    record = record,
                                    onClick = { viewModel.selectRecord(record) },
                                    onTogglePin = {
                                        val isPinnedNow = viewModel.togglePin(record.id)
                                        Toast.makeText(
                                            context,
                                            if (isPinnedNow) "已置頂 ${record.name}" else "已取消置頂 ${record.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onManageTags = { viewModel.manageTagsRecord(record) },
                                    onSelectTagFilter = { tag -> viewModel.onTagFilterSelect(tag) },
                                    onCopyInfo = {
                                        val info = formatBaZiInfo(record)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("BaZi Chart Info", info)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(
                                            context,
                                            "已複製 ${record.name} 的命盤資訊",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onEdit = { viewModel.editRecord(record) },
                                    onDelete = { viewModel.confirmDeleteRecord(record) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.activeDialog == BaZiDialogType.ADD) {
        AddBaZiDialog(
            onDismiss = { viewModel.showDialog(null) },
            onSave = { viewModel.saveRecord(it) }
        )
    }

    if (uiState.activeDialog == BaZiDialogType.SETTINGS) {
        AppSettingsDialog(onDismiss = { viewModel.showDialog(null) })
    }

    if (uiState.activeDialog == BaZiDialogType.DATA_MANAGEMENT) {
        DataManagementDialog(onDismiss = {
            viewModel.showDialog(null)
            viewModel.loadData()
        })
    }

    uiState.recordToEdit?.let { record ->
        AddBaZiDialog(
            initialRecord = record,
            onDismiss = { viewModel.editRecord(null) },
            onSave = { viewModel.saveRecord(it) }
        )
    }

    uiState.recordToDelete?.let { record ->
        DeleteConfirmDialog(
            message = "要將 ${record.name} 的八字紀錄移至回收站嗎？",
            onDismiss = { viewModel.confirmDeleteRecord(null) },
            onConfirm = {
                viewModel.moveToTrash(record.id)
                Toast.makeText(context, "已將 ${record.name} 移至回收站", Toast.LENGTH_SHORT).show()
            }
        )
    }

    uiState.selectedRecord?.let { record ->
        BaZiDetailDialog(
            record = record,
            onDismiss = { viewModel.selectRecord(null) }
        )
    }

    uiState.managingTagsRecord?.let { record ->
        ManageTagsDialog(
            title = "管理「${record.name}」的標籤",
            currentTags = record.safeTags,
            allAppTags = uiState.allUniqueTags,
            onDismiss = { viewModel.manageTagsRecord(null) },
            onSaveTags = { updatedTags ->
                viewModel.saveTags(record, updatedTags)
            }
        )
    }

    if (uiState.activeDialog == BaZiDialogType.TRASH) {
        val trashedItems = remember(uiState.trashRecords) {
            uiState.trashRecords.map { record ->
                val timeStr = "%02d:%02d".format(record.hour, record.minute)
                val typeStr = if (record.isLunar) "農曆" else "公曆"
                TrashedItem(
                    id = record.id,
                    title = "${record.name} (${record.gender})",
                    subtitle = "$typeStr: ${record.year}-${record.month}-${record.day} $timeStr",
                    deletedAt = record.deletedAt,
                    rawItem = record
                )
            }
        }

        TrashDialog(
            dialogTitle = "八字回收站",
            trashedItems = trashedItems,
            onDismiss = { viewModel.showDialog(null) },
            onRestore = { trashedItem ->
                viewModel.restoreFromTrash(trashedItem.id)
                Toast.makeText(context, "已還原 ${trashedItem.rawItem.name}", Toast.LENGTH_SHORT).show()
            },
            onPermanentlyDelete = { trashedItem ->
                viewModel.permanentlyDeleteFromTrash(trashedItem.id)
                Toast.makeText(context, "已徹底刪除 ${trashedItem.rawItem.name}", Toast.LENGTH_SHORT).show()
            },
            onEmptyTrash = {
                viewModel.emptyTrash()
                Toast.makeText(context, "已清空回收站", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
