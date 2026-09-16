package com.example.myTools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myTools.carspeed.AppPreferences
import com.example.myTools.ui.RedBadgeNumber
import com.example.myTools.utils.AppBadgeManager

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int? = null,
) {
    data object Almanac : BottomBarScreen(
        "almanac", "黃曆", 
        Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth
    )
    data object Note : BottomBarScreen(
        "note", "記事本", 
        Icons.Filled.Description, Icons.Outlined.Description
    )
    data object Birthday : BottomBarScreen(
        "birthday", "生日", 
        Icons.Filled.Cake, Icons.Outlined.Cake
    )
    data object BaZi : BottomBarScreen(
        "bazi", "八字", 
        Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh
    )
    data object Luopan : BottomBarScreen(
        "luopan", "羅盤", 
        Icons.Filled.Explore, Icons.Outlined.Explore
    )
    data object Caliper : BottomBarScreen(
        "caliper", "尺規", 
        Icons.Filled.Straighten, Icons.Outlined.Straighten
    )
    data object CarSpeed : BottomBarScreen(
        "carspeed", "車速", 
        Icons.Filled.Speed, Icons.Outlined.Speed
    )
    data object PeriodTracker : BottomBarScreen(
        "period", "月經記錄", 
        Icons.Filled.CalendarToday, Icons.Outlined.CalendarToday
    )
    data object Tools : BottomBarScreen(
        "tools", "工具", 
        Icons.Filled.HomeRepairService, Icons.Outlined.HomeRepairService
    )

    companion object {
        val ALL_CUSTOMIZABLE = listOf(
            Almanac, Note, Birthday, BaZi, Luopan, Caliper, CarSpeed, PeriodTracker
        )

        fun fromRoute(route: String?): BottomBarScreen {
            return ALL_CUSTOMIZABLE.firstOrNull { it.route == route }
                ?: if (route == Tools.route) Tools else Almanac
        }
    }
}

@Composable
fun MainBottomBarDynamic(
    navController: NavHostController,
    screens: List<BottomBarScreen> = listOf(
        BottomBarScreen.Almanac,
        BottomBarScreen.Note,
        BottomBarScreen.Birthday,
        BottomBarScreen.Tools
    )
) {
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    val bottomBarSlots = remember { appPreferences.getBottomBarSlots() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val upcomingBirthdayCount by AppBadgeManager.upcomingBirthdayCount.collectAsState()
    val upcomingPeriodCount by AppBadgeManager.upcomingPeriodCount.collectAsState()
    val isCarSpeedRecording by AppBadgeManager.isCarSpeedRecording.collectAsState()

    LaunchedEffect(Unit) {
        AppBadgeManager.refreshBirthdayBadges(context)
        AppBadgeManager.refreshPeriodBadges(context)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val badgeCount = AppBadgeManager.getBadgeCountForRoute(
                        route = screen.route,
                        bottomBarSlots = bottomBarSlots,
                        upcomingBirthdayCount = upcomingBirthdayCount,
                        isCarSpeedRecording = isCarSpeedRecording,
                        upcomingPeriodCount = upcomingPeriodCount
                    )
                    AddItem(
                        screen = screen,
                        currentDestination = currentDestination,
                        navController = navController,
                        badgeCount = badgeCount
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.AddItem(
    screen: BottomBarScreen,
    currentDestination: NavDestination?,
    navController: NavHostController,
    badgeCount: Int = 0
) {
    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

    // 1. 色彩轉場
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "contentColor"
    )

    // 2. 縮放動畫 (輕微縮放，使用 GPU graphicsLayer 避免觸發重新測量 layout)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .height(48.dp)
            .weight(1f)
            .background(color = backgroundColor, shape = RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 去除預設漣漪
            ) {
                if (!isSelected) {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                    contentDescription = screen.title,
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )

                if (badgeCount > 0) {
                    RedBadgeNumber(
                        count = badgeCount,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 4.dp)
                    )
                }
            }

            // 3. 文字漸變與滑動（使用 200ms tween 確保動畫流暢快速）
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(animationSpec = tween(180)) + expandHorizontally(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(180)) + shrinkHorizontally(animationSpec = tween(180))
            ) {
                Text(
                    text = screen.title,
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                    maxLines = 1
                )
            }
        }
    }
}
