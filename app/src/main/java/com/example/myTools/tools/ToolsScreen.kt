package com.example.myTools.tools

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.myTools.caliper.CaliperScreen
import com.example.myTools.carspeed.CarSpeedScreen
import com.example.myTools.luopan.LuopanScreen

sealed class Tool(val title: String, val icon: ImageVector) {
    object Luopan : Tool("羅盤", Icons.Default.Explore)
    object Caliper : Tool("尺規", Icons.Default.Straighten)
    object CarSpeed : Tool("車速", Icons.Default.Speed)
    object Widget : Tool("添加小工具", Icons.Default.Dashboard)
    object Settings : Tool("設置", Icons.Default.Settings)
    object Support : Tool("打賞支持", Icons.Default.VolunteerActivism)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(onToggleBottomBar: (Boolean) -> Unit) {
    var selectedTool by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    // 動態調整系統狀態列圖示顏色
    LaunchedEffect(selectedTool) {
        // 當進入羅盤頁面、車速頁面或尺規頁面（全屏工具）時，隱藏底部導航欄
        onToggleBottomBar(selectedTool != Tool.Luopan.title && selectedTool != Tool.CarSpeed.title && selectedTool != Tool.Caliper.title)

        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        // 當進入特定工具頁面（深色背景或全屏）時，調整狀態列圖示
        insetsController.isAppearanceLightStatusBars =
            (selectedTool != Tool.Luopan.title && selectedTool != Tool.CarSpeed.title && selectedTool != Tool.Caliper.title)
    }

    if (showSettingsDialog) {
        AppSettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    // 處理返回鍵，如果正在使用工具，按返回鍵回到菜單
    BackHandler(enabled = selectedTool != null) {
        selectedTool = null
    }

    if (selectedTool == null) {
        // 工具選擇菜單
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            "工具箱", 
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold 
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            val tools = listOf(
                Tool.Luopan,
                Tool.Caliper,
                Tool.CarSpeed,
                Tool.Widget,
                Tool.Settings,
                Tool.Support
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // 3列佈局
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                items(tools) { tool ->
                    ToolItem(tool = tool) {
                        when (tool) {
                            is Tool.Widget -> requestPinWidget(context)
                            is Tool.Settings -> showSettingsDialog = true
                            else -> selectedTool = tool.title
                        }
                    }
                }
            }
        }
    } else {
        // 具體的工具頁面
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
            when (selectedTool) {
                Tool.Luopan.title -> LuopanScreen(onBack = { selectedTool = null })
                Tool.Caliper.title -> CaliperScreen(onBack = { selectedTool = null })
                Tool.CarSpeed.title -> CarSpeedScreen(onBack = { selectedTool = null })
                Tool.Support.title -> SupportScreen()
            }
        }
    }
}

@Composable
fun ToolItem(tool: Tool, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp), // M3 標準中型容器圓角
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp, // 增加 M3 特有的色調層次感
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                0.5.dp, 
                MaterialTheme.colorScheme.outlineVariant, 
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary // 統一使用主色，保持專業感
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.labelLarge, // 使用 M3 標準標籤字體
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private fun requestPinWidget(context: Context) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val myProvider = ComponentName(context, LunarWidgetProvider::class.java)

    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        // 彈出系統請求添加小部件對話框（即截圖中的界面）
        // 用戶點擊「新增至主畫面」後，系統會自動在桌面尋找空位添加
        appWidgetManager.requestPinAppWidget(myProvider, null, null)
    } else {
        Toast.makeText(context, "您的手機啟動器不支持自動添加小工具，請手動長按桌面添加", Toast.LENGTH_LONG).show()
    }
}
