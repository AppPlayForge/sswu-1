package com.example.myTools

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.myTools.ui.theme.AppThemeScheme
import com.example.myTools.ui.theme.DarkModeConfig
import com.example.myTools.ui.theme.RulerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    private var currentPage by mutableIntStateOf(0)

    companion object {
        private val _themeScheme = MutableStateFlow(AppThemeScheme.DYNAMIC)
        val themeScheme = _themeScheme.asStateFlow()

        private val _darkModeConfig = MutableStateFlow(DarkModeConfig.FOLLOW_SYSTEM)
        val darkModeConfig = _darkModeConfig.asStateFlow()

        private val _isAppBlurred = MutableStateFlow(false)
        val isAppBlurred = _isAppBlurred.asStateFlow()

        fun updateTheme(context: Context, scheme: AppThemeScheme) {
            _themeScheme.value = scheme
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("theme_scheme", scheme.name)
                .apply()
        }

        fun updateDarkMode(context: Context, config: DarkModeConfig) {
            _darkModeConfig.value = config
            context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("dark_mode", config.name)
                .apply()
        }

        fun setAppBlurred(isBlurred: Boolean) {
            _isAppBlurred.value = isBlurred
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化主題與深色模式
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        
        val savedTheme = prefs.getString("theme_scheme", AppThemeScheme.DYNAMIC.name)
        _themeScheme.value = AppThemeScheme.valueOf(savedTheme ?: AppThemeScheme.DYNAMIC.name)

        val savedDarkMode = prefs.getString("dark_mode", DarkModeConfig.FOLLOW_SYSTEM.name)
        _darkModeConfig.value = DarkModeConfig.valueOf(savedDarkMode ?: DarkModeConfig.FOLLOW_SYSTEM.name)

        enableEdgeToEdge()

        // 獲取啟動時的頁面索引
        currentPage = intent.getIntExtra("target_page", 0)

        setContent {
            val currentTheme by themeScheme.collectAsState()
            val currentDarkMode by darkModeConfig.collectAsState()
            
            RulerTheme(
                themeScheme = currentTheme,
                darkModeConfig = currentDarkMode
            ) {
                MainScreen(initialPage = currentPage)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val targetPage = intent.getIntExtra("target_page", -1)
        if (targetPage != -1) {
            currentPage = targetPage
        }
    }
}
