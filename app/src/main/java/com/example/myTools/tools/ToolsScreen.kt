package com.example.myTools.tools

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.example.myTools.BuildConfig
import com.example.myTools.MainActivity
import com.example.myTools.R
import com.example.myTools.caliper.CaliperScreen
import com.example.myTools.carspeed.CarSpeedScreen
import com.example.myTools.luopan.LuopanScreen
import com.example.myTools.period.PeriodTrackerScreen
import com.example.myTools.ui.BlurryContainer
import com.example.myTools.ui.theme.AppThemeScheme
import java.text.SimpleDateFormat
import java.util.*

sealed class Tool(val title: String, val icon: ImageVector) {
    object Luopan : Tool("羅盤", Icons.Default.Explore)
    object Caliper : Tool("尺規", Icons.Default.Straighten)
    object CarSpeed : Tool("車速", Icons.Default.Speed)
    object PeriodTracker : Tool("月經記錄", Icons.Default.CalendarMonth)
    object Widget : Tool("添加小工具", Icons.Default.Dashboard)
    object DataManagement : Tool("數據管理", Icons.Default.CloudSync)
    object Settings : Tool("權限申請", Icons.Default.Settings)
    object Support : Tool("打賞支持", Icons.Default.VolunteerActivism)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(onToggleBottomBar: (Boolean) -> Unit) {
    var selectedTool by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showDataManagementDialog by rememberSaveable { mutableStateOf(false) }
    var versionClickCount by remember { mutableIntStateOf(0) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showGeneratorDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    // 動態調整系統狀態列圖示顏色與底部欄顯示
    LaunchedEffect(selectedTool) {
        val shouldShowBottomBar = selectedTool != Tool.Luopan.title && 
                                 selectedTool != Tool.CarSpeed.title && 
                                 selectedTool != Tool.Caliper.title &&
                                 selectedTool != Tool.PeriodTracker.title
        
        onToggleBottomBar(shouldShowBottomBar)
        
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = shouldShowBottomBar
    }

    val isAnyDialogOpen = showSettingsDialog || showThemeDialog || showDataManagementDialog || showPasswordDialog || showGeneratorDialog || selectedTool == Tool.Support.title

    LaunchedEffect(isAnyDialogOpen) {
        MainActivity.setAppBlurred(isAnyDialogOpen)
    }

    if (showSettingsDialog) {
        AppSettingsDialog(onDismiss = { showSettingsDialog = false })
    }
    
    if (showThemeDialog) {
        ThemeSettingsDialog(onDismiss = { showThemeDialog = false })
    }

    if (showDataManagementDialog) {
        DataManagementDialog(onDismiss = { showDataManagementDialog = false })
    }

    if (showPasswordDialog) {
        var passwordInput by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("開發者驗證", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("請輸入密碼解鎖激活碼生成器：", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    TextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("密碼") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (ActivationSecret.verifyPassword(passwordInput)) {
                        showPasswordDialog = false
                        showGeneratorDialog = true
                    } else {
                        Toast.makeText(context, "密碼錯誤", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("確認")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showGeneratorDialog) {
        var devDeviceId by remember { mutableStateOf("") }
        var generatedCode by remember { mutableStateOf("") }
        var historyList by remember { mutableStateOf(ActivationSecret.getHistory(context)) }

        Dialog(
            onDismissRequest = { showGeneratorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "激活碼生成器",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showGeneratorDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "關閉")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("輸入目標設備 ID 生成激活碼：", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = devDeviceId,
                            onValueChange = { devDeviceId = it },
                            label = { Text("設備 ID") },
                            singleLine = true,
                            trailingIcon = {
                                if (devDeviceId.isNotEmpty()) {
                                    IconButton(onClick = { devDeviceId = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "清空")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            devDeviceId = DataManagementUtils.getDeviceId(context)
                        }) {
                            Text("帶入本機")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (devDeviceId.isNotBlank()) {
                                val trimmedId = devDeviceId.trim()
                                generatedCode = DataManagementUtils.generateValidCode(trimmedId)
                                ActivationSecret.saveHistory(context, trimmedId, generatedCode)
                                historyList = ActivationSecret.getHistory(context)
                            } else {
                                Toast.makeText(context, "請輸入設備 ID", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("生成激活碼")
                    }

                    if (generatedCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("當前生成的激活碼：", style = MaterialTheme.typography.labelMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SelectionContainer(modifier = Modifier.weight(1f)) {
                                        Text(
                                            generatedCode,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Activation Code", generatedCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "已複製激活碼", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "複製", modifier = Modifier.size(18.dp))
                                    }
                                }

                                if (devDeviceId.trim() == DataManagementUtils.getDeviceId(context)) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = {
                                            if (DataManagementUtils.activate(context, generatedCode)) {
                                                Toast.makeText(context, "本機已成功激活！", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "激活失敗", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("直接激活本機")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "生成歷史記錄 (${historyList.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (historyList.isNotEmpty()) {
                            TextButton(onClick = {
                                ActivationSecret.clearHistory(context)
                                historyList = emptyList()
                            }) {
                                Text("清空歷史", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (historyList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暫無生成記錄", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(historyList) { item ->
                                val dateStr = remember(item.timestamp) {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "設備 ID: ${item.deviceId}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "激活碼: ${item.code}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Activation Code", item.code)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "已複製激活碼", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "複製", modifier = Modifier.size(16.dp))
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

    BackHandler(enabled = selectedTool != null) {
        selectedTool = null
    }

    if (selectedTool == null || selectedTool == Tool.Support.title) {
        Scaffold(
            topBar = {
                BlurryContainer(isBlur = isAnyDialogOpen) {
                    TopAppBar(
                        title = { 
                            Text(
                                text = "工具箱", 
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ) 
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
                                    DropdownMenuItem(
                                        text = { Text("聯繫作者") },
                                        onClick = {
                                            menuExpanded = false
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = "mailto:sswuss@outlook.com".toUri()
                                                putExtra(Intent.EXTRA_SUBJECT, "App 反饋")
                                            }
                                            try { context.startActivity(intent) } catch (_: Exception) {}
                                        },
                                        leadingIcon = { Icon(Icons.Default.Email, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("我們的頻道") },
                                        onClick = {
                                            menuExpanded = false
                                            val intent = Intent(Intent.ACTION_VIEW, "https://youtu.be/SDCEfVyvQis".toUri())
                                            context.startActivity(intent)
                                        },
                                        leadingIcon = { Icon(Icons.Default.PlayCircle, null, tint = Color.Red) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, "https://m.bilibili.com/space/297639121".toUri())
                                                context.startActivity(intent)
                                            }) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_bilibili),
                                                    contentDescription = "Bilibili",
                                                    tint = Color.Unspecified
                                                )
                                            }
                                        }
                                    )
                                    val appName = stringResource(id = R.string.app_name)
                                    DropdownMenuItem(
                                        text = { Text("分享 App") },
                                        onClick = {
                                            menuExpanded = false
                                            val downloadUrl = "https://github.com/AppPlayForge/sswu-1.git"
                                            val shareText = "推薦你使用「$appName」，有很多實用的工具！下載地址：$downloadUrl"

                                            // 自動複製到剪貼板，解決微信/QQ等應用無法自動獲取文本的問題
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("App Share", shareText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "分享內容已複製到剪貼板", Toast.LENGTH_SHORT).show()

                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "分享給好友"))
                                        },
                                        leadingIcon = { Icon(Icons.Default.Share, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("其它應用") },
                                        onClick = {
                                            menuExpanded = false
                                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/AppPlayForge".toUri())
                                            context.startActivity(intent)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Code, null) }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("版本號: v${BuildConfig.VERSION_NAME}") },
                                        onClick = {
                                            versionClickCount++
                                            if (versionClickCount >= 6) {
                                                versionClickCount = 0
                                                menuExpanded = false
                                                showPasswordDialog = true
                                            } else if (versionClickCount >= 3) {
                                                Toast.makeText(context, "再點擊 ${6 - versionClickCount} 次進入開發者模式", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
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
                    
                    val firstGroup = listOf(
                        Tool.Luopan,
                        Tool.Caliper,
                        Tool.CarSpeed,
                        Tool.PeriodTracker
                    )
                    val secondGroup = listOf(
                        Tool.Widget,
                        Tool.DataManagement,
                        Tool.Settings,
                        Tool.Support
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // 第一組 (3列)
                        firstGroup.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { tool ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ToolItem(tool = tool) {
                                            selectedTool = tool.title
                                        }
                                    }
                                }
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )

                        // 第二組 (4列)
                        secondGroup.chunked(4).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { tool ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        ToolItem(tool = tool, isSmall = true) {
                                            when (tool) {
                                                is Tool.Widget -> requestPinWidget(context)
                                                is Tool.Settings -> showSettingsDialog = true
                                                is Tool.DataManagement -> showDataManagementDialog = true
                                                else -> selectedTool = tool.title
                                            }
                                        }
                                    }
                                }
                                repeat(4 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTool == Tool.Support.title) {
            Dialog(
                onDismissRequest = { selectedTool = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .fillMaxHeight(0.85f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SupportScreen(modifier = Modifier.fillMaxSize())
                        IconButton(
                            onClick = { selectedTool = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, null)
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
                Tool.PeriodTracker.title -> PeriodTrackerScreen(onBack = { selectedTool = null })
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
fun ToolItem(tool: Tool, isSmall: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(if (isSmall) 12.dp else 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                0.5.dp, 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                RoundedCornerShape(if (isSmall) 12.dp else 16.dp)
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(if (isSmall) 4.dp else 8.dp)
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                modifier = Modifier.size(if (isSmall) 24.dp else 48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(if (isSmall) 4.dp else 6.dp))
            Text(
                text = tool.title,
                style = if (isSmall) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
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
