package com.example.myTools

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import com.example.myTools.tools.ToolsScreen
import com.example.myTools.ui.BlurryContainer

private fun getRouteIndex(route: String?, screens: List<BottomBarScreen>): Int {
    val index = screens.indexOfFirst { it.route == route }
    return if (index >= 0) index else 0
}

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

    // 將 initialPage 轉換為路由 (預設開啟首頁為「黃曆」)
    val initialRoute = remember(initialPage) {
        when (initialPage) {
            0 -> BottomBarScreen.Almanac.route
            1 -> BottomBarScreen.Note.route
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

    val isAtStartDestination = currentRoute == initialRoute || currentRoute == null
    BackHandler(enabled = !isAtStartDestination) {
        navController.navigate(initialRoute) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
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
            startDestination = initialRoute,
            enterTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route, currentBottomBarScreens)
                val targetIndex = getRouteIndex(targetState.destination.route, currentBottomBarScreens)
                if (targetIndex >= initialIndex) {
                    slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                } else {
                    slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                }
            },
            exitTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route, currentBottomBarScreens)
                val targetIndex = getRouteIndex(targetState.destination.route, currentBottomBarScreens)
                if (targetIndex >= initialIndex) {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                } else {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route, currentBottomBarScreens)
                val targetIndex = getRouteIndex(targetState.destination.route, currentBottomBarScreens)
                if (targetIndex <= initialIndex) {
                    slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                } else {
                    slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                }
            },
            popExitTransition = {
                val initialIndex = getRouteIndex(initialState.destination.route, currentBottomBarScreens)
                val targetIndex = getRouteIndex(targetState.destination.route, currentBottomBarScreens)
                if (targetIndex <= initialIndex) {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(300))
                } else {
                    slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(300))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (isBottomBarVisible) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            composable(BottomBarScreen.Almanac.route) {
                AlmanacScreen(modifier = Modifier.fillMaxSize())
            }
            composable(BottomBarScreen.Note.route) {
                NoteScreen()
            }
            composable(BottomBarScreen.Birthday.route) {
                LunarBirthdayScreen()
            }
            composable(BottomBarScreen.BaZi.route) {
                BaZiScreen(onBack = {
                    navController.navigate(initialRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(BottomBarScreen.Luopan.route) {
                LuopanScreen(onBack = {
                    navController.navigate(initialRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(BottomBarScreen.Caliper.route) {
                CaliperScreen(onBack = {
                    navController.navigate(initialRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(BottomBarScreen.CarSpeed.route) {
                CarSpeedScreen(onBack = {
                    navController.navigate(initialRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(BottomBarScreen.PeriodTracker.route) {
                PeriodTrackerScreen(onBack = {
                    navController.navigate(initialRoute) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
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
}
