package com.example.myTools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HomeRepairService
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

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
    data object BaZi : BottomBarScreen(
        "bazi", "八字", 
        Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh
    )
    data object Birthday : BottomBarScreen(
        "birthday", "生日", 
        Icons.Filled.Cake, Icons.Outlined.Cake,
        badgeCount = null // 移除示例 Badge
    )
    data object Tools : BottomBarScreen(
        "tools", "工具", 
        Icons.Filled.HomeRepairService, Icons.Outlined.HomeRepairService
    )
}

@Composable
fun MainBottomBarDynamic(navController: NavHostController) {
    val screens = listOf(
        BottomBarScreen.Almanac,
        BottomBarScreen.BaZi,
        BottomBarScreen.Birthday,
        BottomBarScreen.Tools
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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
                    AddItem(
                        screen = screen,
                        currentDestination = currentDestination,
                        navController = navController
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
    navController: NavHostController
) {
    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
    
    // 1. 色彩轉場
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "bgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "contentColor"
    )

    // 2. 彈跳縮放
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // 3. 搖擺效果 (Wobble)
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            rotation.animateTo(
                targetValue = 10f,
                animationSpec = spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow)
            )
            rotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessLow)
            )
        }
    }

    // 4. 動態寬度 (Weight)
    val weight by animateFloatAsState(
        targetValue = if (isSelected) 1.5f else 1f,
        label = "weight"
    )

    Box(
        modifier = Modifier
            .height(48.dp)
            .weight(weight)
            .background(color = backgroundColor, shape = RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 去除默認漣漪
            ) {
                navController.navigate(screen.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            BadgedBox(
                badge = {
                    if (screen.badgeCount != null) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            val badgeText = if (screen.badgeCount > 99) "99+" else screen.badgeCount.toString()
                            Text(badgeText)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                    contentDescription = screen.title,
                    tint = contentColor,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(scale)
                        .graphicsLayer {
                            rotationZ = rotation.value
                        }
                )
            }
            
            // 5. 文字伸縮
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = screen.title,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1
                )
            }
        }
    }
}
