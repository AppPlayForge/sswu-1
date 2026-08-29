package com.example.myTools.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

/**
 * 通用的可搜索頂欄組件
 * 封裝了動畫切換、返回鍵處理以及搜索與常規模式的切換邏輯
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTopBar(
    title: String,
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    placeholder: String = "搜索名稱..."
) {
    // 處理返回鍵：如果搜索處於激活狀態，點擊返回鍵關閉搜索
    if (isSearchActive) {
        BackHandler {
            onSearchActiveChange(false)
            onQueryChange("")
        }
    }

    AnimatedContent(
        targetState = isSearchActive,
        transitionSpec = {
            if (targetState) {
                // 進入搜索模式：從右向左滑入
                (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                // 退出搜索模式：從左向右滑入
                (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "SearchableTopBarAnimation",
        modifier = modifier
    ) { searchActive ->
        if (searchActive) {
            // 搜索模式 UI
            CommonSearchTopBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onCloseClick = {
                    onSearchActiveChange(false)
                    onQueryChange("")
                },
                placeholder = placeholder
            )
        } else {
            // 常規模式 UI
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = navigationIcon,
                actions = {
                    // 默認顯示搜索圖標，改為在左側
                    IconButton(onClick = { onSearchActiveChange(true) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // 渲染傳入的自定義按鈕（如 3 點菜單），會出現在搜索圖標右側（即最右邊）
                    actions()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}
