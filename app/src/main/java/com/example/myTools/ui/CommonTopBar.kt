package com.example.myTools.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopBar(
    title: String,
    onSettingsClick: () -> Unit,
    actionIcon: ImageVector = Icons.Default.MoreVert,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    titleColor: Color = MaterialTheme.colorScheme.primary // 預設使用主色調，讓標題更適配主題
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
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(actionIcon, contentDescription = "更多")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        )
    )
}
