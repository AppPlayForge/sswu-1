package com.example.myTools

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myTools.almanac.AlmanacScreen
import com.example.myTools.bazi.BaZiScreen
import com.example.myTools.birthday.LunarBirthdayScreen
import com.example.myTools.tools.ToolsScreen

@Composable
fun MainScreen(initialPage: Int = 0) {
    // 0=黃曆, 1=八字, 2=生日, 3=工具箱
    var selectedIndex by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(initialPage) }
    
    // 當 initialPage 改變時（例如從 Widget 進入），更新選中的索引
    LaunchedEffect(initialPage) {
        selectedIndex = initialPage
    }

    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(selectedIndex) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        // 統一顯示系統欄，並設置為淺色狀態欄（即深色圖標）
        insetsController.show(WindowInsetsCompat.Type.systemBars())
        insetsController.isAppearanceLightStatusBars = true
    }

    // 統一導航欄顏色，跟隨主題
    val targetBarColor = MaterialTheme.colorScheme.surface
    val navBarColor by animateColorAsState(targetValue = targetBarColor, animationSpec = tween(500), label = "BarColor")
    val targetContentColor = MaterialTheme.colorScheme.onSurface
    val navContentColor by animateColorAsState(targetValue = targetContentColor, animationSpec = tween(500), label = "ContentColor")

    var isBottomBarVisible by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    data class NavItem(
        val index: Int,
        val label: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector
    )

    val navItems = listOf(
        NavItem(0, "黃曆", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
        NavItem(1, "八字", Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh),
        NavItem(2, "生日", Icons.Filled.Cake, Icons.Outlined.Cake),
        NavItem(3, "工具箱", Icons.Filled.HomeRepairService, Icons.Outlined.HomeRepairService)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), // 調深背景，增加對比
        bottomBar = {
            if (isBottomBarVisible) {
                val primaryColor = MaterialTheme.colorScheme.primary
                NavigationBar(containerColor = navBarColor, contentColor = navContentColor, tonalElevation = 3.dp) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            icon = { 
                                AnimatedNavIcon(
                                    isSelected = selectedIndex == item.index,
                                    selectedIcon = item.selectedIcon,
                                    unselectedIcon = item.unselectedIcon,
                                    label = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selectedIndex == item.index,
                            onClick = { selectedIndex = item.index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = primaryColor,
                                selectedTextColor = primaryColor,
                                indicatorColor = primaryColor.copy(alpha = 0.1f),
                                unselectedIconColor = navContentColor.copy(alpha = 0.6f),
                                unselectedTextColor = navContentColor.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val bottomPadding = if (isBottomBarVisible) innerPadding.calculateBottomPadding() else 0.dp
        Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
            when (selectedIndex) {
                0 -> AlmanacScreen(modifier = Modifier.fillMaxSize())
                1 -> BaZiScreen()
                2 -> LunarBirthdayScreen()
                3 -> ToolsScreen(onToggleBottomBar = { isBottomBarVisible = it })
            }
        }
    }
}

/**
 * 自定義的動畫導航圖標
 * [isSelected] 是否被選中
 * [selectedIcon] 選中時顯示的實心圖標
 * [unselectedIcon] 未選中時顯示的輪廓圖標
 */
@Composable
fun AnimatedNavIcon(
    isSelected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String
) {
    // 1. 縮放動畫 (同前)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    // 2. 搖擺旋轉動畫
    // 當選中時，目標旋轉 15 度
    // 使用 HighBouncy (高彈跳) 讓它在到達 15 度時會像彈簧一樣晃動幾下
    val rotation by animateFloatAsState(
        targetValue = if (isSelected) 15f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy, // 增加搖擺的彈性
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconRotation"
    )

    // 3. 組合動畫應用
    Icon(
        imageVector = if (isSelected) selectedIcon else unselectedIcon,
        contentDescription = label,
        modifier = Modifier
            .scale(scale)          // 應用縮放
            .graphicsLayer { 
                rotationZ = rotation // 應用 Z 軸旋轉 (搖擺)
            }
    )
}
