package com.example.myTools

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myTools.almanac.AlmanacScreen
import com.example.myTools.bazi.BaZiScreen
import com.example.myTools.birthday.LunarBirthdayScreen
import com.example.myTools.tools.ToolsScreen
import com.example.myTools.ui.BlurryContainer

private fun getRouteIndex(route: String?): Int {
    return when (route) {
        BottomBarScreen.Almanac.route -> 0
        BottomBarScreen.BaZi.route -> 1
        BottomBarScreen.Birthday.route -> 2
        BottomBarScreen.Tools.route -> 3
        else -> 0
    }
}

@Composable
fun MainScreen(initialPage: Int = 0) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 將 initialPage 轉換為路由
    val initialRoute = remember(initialPage) {
        when (initialPage) {
            0 -> BottomBarScreen.Almanac.route
            1 -> BottomBarScreen.BaZi.route
            2 -> BottomBarScreen.Birthday.route
            3 -> BottomBarScreen.Tools.route
            else -> BottomBarScreen.Almanac.route
        }
    }

    // 當 initialPage 改變時（例如從外部啟動），進行導航
    LaunchedEffect(initialPage) {
        if (currentRoute != initialRoute) {
            navController.navigate(initialRoute) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(currentRoute) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
        insetsController.isAppearanceLightStatusBars = true
    }

    var isBottomBarVisible by remember { mutableStateOf(true) }
    val isAppBlurred by MainActivity.isAppBlurred.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BlurryContainer(isBlur = isAppBlurred) {
                    MainBottomBarDynamic(navController = navController)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            enterTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                } else {
                    slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                }
            },
            exitTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                } else {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                } else {
                    slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                }
            },
            popExitTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route)
                val targetIndex = getRouteIndex(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                } else {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isBottomBarVisible) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            composable(BottomBarScreen.Almanac.route) {
                AlmanacScreen(modifier = Modifier.fillMaxSize())
            }
            composable(BottomBarScreen.BaZi.route) {
                BaZiScreen()
            }
            composable(BottomBarScreen.Birthday.route) {
                LunarBirthdayScreen()
            }
            composable(BottomBarScreen.Tools.route) {
                ToolsScreen(onToggleBottomBar = { isBottomBarVisible = it })
            }
        }
    }
}
