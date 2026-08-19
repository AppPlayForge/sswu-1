package com.example.myTools.tools

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.myTools.MainActivity
import com.example.myTools.caliper.CaliperScreen
import com.example.myTools.carspeed.CarSpeedScreen
import com.example.myTools.luopan.LuopanScreen
import com.example.myTools.ui.BlurryContainer
import com.example.myTools.ui.theme.AppThemeScheme

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
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    // 動態調整系統狀態列圖示顏色
    LaunchedEffect(selectedTool) {
        onToggleBottomBar(selectedTool != Tool.Luopan.title && selectedTool != Tool.CarSpeed.title && selectedTool != Tool.Caliper.title)
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars =
            (selectedTool != Tool.Luopan.title && selectedTool != Tool.CarSpeed.title && selectedTool != Tool.Caliper.title)
    }

    val isAnyDialogOpen = showSettingsDialog || showThemeDialog

    LaunchedEffect(isAnyDialogOpen) {
        MainActivity.setAppBlurred(isAnyDialogOpen)
    }

    if (showSettingsDialog) {
        AppSettingsDialog(onDismiss = { showSettingsDialog = false })
    }
    
    if (showThemeDialog) {
        ThemeSettingsDialog(onDismiss = { showThemeDialog = false })
    }

    BackHandler(enabled = selectedTool != null) {
        selectedTool = null
    }

    if (selectedTool == null) {
        Scaffold(
            topBar = {
                BlurryContainer(isBlur = isAnyDialogOpen) {
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
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            BlurryContainer(
                isBlur = isAnyDialogOpen,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 主題設置入口
                    ThemeEntrySection(onClick = { showThemeDialog = true })
                    
                    val tools = listOf(
                        Tool.Luopan,
                        Tool.Caliper,
                        Tool.CarSpeed,
                        Tool.Widget,
                        Tool.Settings,
                        Tool.Support
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
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
            }
        }
    } else {
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
fun ThemeEntrySection(onClick: () -> Unit) {
    val currentTheme by MainActivity.themeScheme.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Palette, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "個性化主題", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "當前配色: ${getThemeLabel(currentTheme)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getThemeLabel(scheme: AppThemeScheme): String {
    return when(scheme) {
        AppThemeScheme.DYNAMIC -> "依照系統"
        AppThemeScheme.OCEAN -> "海洋藍"
        AppThemeScheme.PRAIRIE -> "草原綠"
        AppThemeScheme.ORANGE -> "活力橙"
        AppThemeScheme.PINK -> "浪漫粉"
        AppThemeScheme.PURPLE -> "優雅紫"
        AppThemeScheme.MONOCHROME -> "極致黑灰"
        AppThemeScheme.EARTH -> "大地紅"
        AppThemeScheme.BLUE -> "經典藍"
    }
}

@Composable
fun ToolItem(tool: Tool, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
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
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = tool.title,
                style = MaterialTheme.typography.labelLarge,
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
        appWidgetManager.requestPinAppWidget(myProvider, null, null)
    } else {
        Toast.makeText(context, "您的手機啟動器不支持自動添加小工具，請手動長按桌面添加", Toast.LENGTH_LONG).show()
    }
}
