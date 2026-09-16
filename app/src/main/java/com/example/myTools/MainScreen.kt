package com.example.myTools

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.myTools.caliper.CaliperScreen
import com.example.myTools.carspeed.AppPreferences
import com.example.myTools.carspeed.CarSpeedScreen
import com.example.myTools.luopan.LuopanScreen
import com.example.myTools.note.NoteScreen
import com.example.myTools.period.PeriodTrackerScreen
import com.example.myTools.tools.AppSettingsDialog
import com.example.myTools.tools.ToolsScreen
import com.example.myTools.ui.BlurryContainer



@Composable
fun MainScreen(initialPage: Int = 0) {
    val context = LocalContext.current
    val view = LocalView.current
    val appPreferences = remember { AppPreferences(context) }
    var bottomBarUpdateTrigger by remember { mutableIntStateOf(0) }

    val currentBottomBarScreens = remember(bottomBarUpdateTrigger) {
        val slots = appPreferences.getBottomBarSlots()
        val customScreens = slots.map { BottomBarScreen.fromRoute(it) }
        customScreens + BottomBarScreen.Tools
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 固定的首頁路由為黃曆頁面
    val rootRoute = BottomBarScreen.Almanac.route

    // 目標啟動頁 (除非從外部 Intent 傳入特定的 initialPage，否則預設開啟黃曆首頁)
    val targetRoute = remember(initialPage) {
        when (initialPage) {
            1 -> BottomBarScreen.Note.route
            2 -> BottomBarScreen.Birthday.route
            3 -> BottomBarScreen.Tools.route
            else -> BottomBarScreen.Almanac.route
        }
    }

    // 當從外部 Intent 啟動非黃曆頁面時進行二級跳轉
    LaunchedEffect(targetRoute) {
        if (targetRoute != rootRoute && currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo(rootRoute) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    var isBottomBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(currentRoute) {
        // 羅盤、尺規、車速三個頁面預設隱藏底欄，以釋放最大螢幕空間
        val fullScreenRoutes = setOf(
            BottomBarScreen.Luopan.route,
            BottomBarScreen.Caliper.route,
            BottomBarScreen.CarSpeed.route
        )
        val shouldShowBottomBar = currentRoute !in fullScreenRoutes
        isBottomBarVisible = shouldShowBottomBar

        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.show(WindowInsetsCompat.Type.systemBars())
        insetsController.isAppearanceLightStatusBars = shouldShowBottomBar
    }
    val isAppBlurred by MainActivity.isAppBlurred.collectAsState()

    // 檢查是否需要自動重新開啟權限對話框 (例如從系統設定返回)
    var showAutoReopenPermissionDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (appPreferences.shouldAutoReopenPermissionDialog()) {
            appPreferences.setAutoReopenPermissionDialog(false)
            showAutoReopenPermissionDialog = true
        }
    }

    val navigateToAlmanac = {
        if (currentRoute != rootRoute) {
            navController.navigate(rootRoute) {
                popUpTo(rootRoute) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    val isAtAlmanac = currentRoute == rootRoute || currentRoute == null
    BackHandler(enabled = !isAtAlmanac) {
        navigateToAlmanac()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BlurryContainer(isBlur = isAppBlurred) {
                    MainBottomBarDynamic(
                        navController = navController,
                        screens = currentBottomBarScreens
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = rootRoute,
            enterTransition = {
                fadeIn(animationSpec = tween(180))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(180))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isBottomBarVisible) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            composable(BottomBarScreen.Almanac.route) {
                AlmanacScreen(modifier = Modifier.fillMaxSize())
            }
            composable(BottomBarScreen.Note.route) {
                NoteScreen(onBack = navigateToAlmanac)
            }
            composable(BottomBarScreen.Birthday.route) {
                LunarBirthdayScreen(onBack = navigateToAlmanac)
            }
            composable(BottomBarScreen.BaZi.route) {
                BaZiScreen(onBack = navigateToAlmanac)
            }
            composable(BottomBarScreen.Luopan.route) {
                LuopanScreen(onBack = navigateToAlmanac)
            }
            composable(BottomBarScreen.Caliper.route) {
                CaliperScreen(onBack = navigateToAlmanac)
            }
            composable(BottomBarScreen.CarSpeed.route) {
                CarSpeedScreen(onBack = navigateToAlmanac)
            }
            composable(BottomBarScreen.PeriodTracker.route) {
                PeriodTrackerScreen(onBack = navigateToAlmanac)
            }
            composable(BottomBarScreen.Tools.route) {
                ToolsScreen(
                    onToggleBottomBar = { isBottomBarVisible = it },
                    onBottomBarUpdated = { bottomBarUpdateTrigger++ },
                    onSelectBottomBarTab = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    if (showAutoReopenPermissionDialog) {
        AppSettingsDialog(onDismiss = { showAutoReopenPermissionDialog = false })
    }
}
