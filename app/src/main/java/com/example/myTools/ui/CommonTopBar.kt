package com.example.myTools.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight

/**
 * 統一設計系統 TopBar（Slot API 版本）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.primary
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        ),
        modifier = modifier
    )
}

/**
 * 舊版相容引數重載
 */
@Composable
fun CommonTopBar(
    title: String,
    onSettingsClick: () -> Unit,
    actionIcon: ImageVector = Icons.Default.MoreVert,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.primary
) {
    CommonTopBar(
        title = title,
        containerColor = containerColor,
        titleColor = titleColor,
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(actionIcon, contentDescription = "更多", tint = titleColor)
            }
        }
    )
}
