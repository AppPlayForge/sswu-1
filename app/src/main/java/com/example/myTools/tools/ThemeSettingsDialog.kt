package com.example.myTools.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myTools.MainActivity
import com.example.myTools.ui.theme.AppThemeScheme
import com.example.myTools.ui.theme.DarkModeConfig

@Composable
fun ThemeSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val currentTheme by MainActivity.themeScheme.collectAsState()
    val currentDarkMode by MainActivity.darkModeConfig.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "主題設定",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "主題配色",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 配色選項列表 - 按照截圖順序
                val schemes = listOf(
                    SchemeItem("依照系統 (Dynamic)", AppThemeScheme.DYNAMIC, isDynamic = true),
                    SchemeItem("海洋藍", AppThemeScheme.OCEAN, colors = listOf(Color(0xFF3F51B5), Color(0xFF607D8B), Color(0xFF9575CD))),
                    SchemeItem("草原綠", AppThemeScheme.PRAIRIE, colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C), Color(0xFF546E7A))),
                    SchemeItem("活力橙", AppThemeScheme.ORANGE, colors = listOf(Color(0xFFA15D0D), Color(0xFF8D6E63), Color(0xFF6D8345))),
                    SchemeItem("浪漫粉", AppThemeScheme.PINK, colors = listOf(Color(0xFFAD4E6D), Color(0xFF8D6E63), Color(0xFFA15D0D))),
                    SchemeItem("優雅紫", AppThemeScheme.PURPLE, colors = listOf(Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFFA15D0D))),
                    SchemeItem("極致黑灰", AppThemeScheme.MONOCHROME, colors = listOf(Color(0xFF424242), Color(0xFF757575), Color(0xFFBDBDBD))),
                    SchemeItem("大地紅", AppThemeScheme.EARTH, colors = listOf(Color(0xFFB34A47), Color(0xFF8D6E63), Color(0xFFA15D0D)))
                )

                Column {
                    schemes.forEach { item ->
                        SchemeRow(
                            item = item,
                            isSelected = currentTheme == item.scheme,
                            onClick = { MainActivity.updateTheme(context, item.scheme) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "深色模式",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                DarkModeRow("依照系統", currentDarkMode == DarkModeConfig.FOLLOW_SYSTEM) {
                    MainActivity.updateDarkMode(context, DarkModeConfig.FOLLOW_SYSTEM)
                }
                DarkModeRow("淺色模式", currentDarkMode == DarkModeConfig.LIGHT) {
                    MainActivity.updateDarkMode(context, DarkModeConfig.LIGHT)
                }
                DarkModeRow("深色模式", currentDarkMode == DarkModeConfig.DARK) {
                    MainActivity.updateDarkMode(context, DarkModeConfig.DARK)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onDismiss) {
                        Text("完成", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

data class SchemeItem(
    val label: String,
    val scheme: AppThemeScheme,
    val isDynamic: Boolean = false,
    val colors: List<Color> = emptyList()
)

@Composable
fun SchemeRow(item: SchemeItem, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.label,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (item.isDynamic) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Row(modifier = Modifier.padding(end = 8.dp)) {
                item.colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    }
}

@Composable
fun DarkModeRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
