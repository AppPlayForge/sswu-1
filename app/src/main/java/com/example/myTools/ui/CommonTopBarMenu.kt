package com.example.myTools.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 通用選單：分享 App
 */
@Composable
fun ShareAppMenuItem(
    onDismissRequest: () -> Unit,
    customText: String? = null
) {
    val context = LocalContext.current
    DropdownMenuItem(
        text = { Text("分享應用") },
        onClick = {
            onDismissRequest()
            ShareUtils.shareApp(context, customText)
        },
        leadingIcon = { Icon(Icons.Default.Share, contentDescription = "分享應用") }
    )
}

/**
 * 通用選單：數據管理
 */
@Composable
fun DataManagementMenuItem(
    onClick: () -> Unit,
    text: String = "數據管理"
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = { Icon(Icons.Default.CloudSync, contentDescription = text) }
    )
}

/**
 * 通用選單：回收站
 */
@Composable
fun TrashMenuItem(
    count: Int,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("回收站 ($count)") },
        onClick = onClick,
        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = "回收站") }
    )
}

/**
 * 通用選單：打賞支持
 */
@Composable
fun SupportMenuItem(
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text("打賞支持") },
        onClick = onClick,
        leadingIcon = { Icon(Icons.Default.VolunteerActivism, contentDescription = "打賞支持") }
    )
}

/**
 * 通用選單：權限 / 應用設置
 */
@Composable
fun AppSettingsMenuItem(
    onClick: () -> Unit,
    text: String = "權限管理"
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = text) }
    )
}
