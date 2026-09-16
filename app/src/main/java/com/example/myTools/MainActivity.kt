package com.example.myTools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
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

        private val _isAppBlurred = MutableStateFlow(value = false)
        val isAppBlurred = _isAppBlurred.asStateFlow()

        private val _externalTxtUri = MutableStateFlow<Uri?>(null)
        val externalTxtUri = _externalTxtUri.asStateFlow()

        fun updateTheme(context: Context, scheme: AppThemeScheme) {
            _themeScheme.value = scheme
            context.getSharedPreferences("prefs", MODE_PRIVATE).edit {
                putString("theme_scheme", scheme.name)
            }
        }

        fun updateDarkMode(context: Context, config: DarkModeConfig) {
            _darkModeConfig.value = config
            context.getSharedPreferences("prefs", MODE_PRIVATE).edit {
                putString("dark_mode", config.name)
            }
        }

        fun setAppBlurred(isBlurred: Boolean) {
            _isAppBlurred.value = isBlurred
        }

        fun clearExternalTxtUri() {
            _externalTxtUri.value = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化主題與深色模式
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)

        val savedTheme = prefs.getString("theme_scheme", AppThemeScheme.DYNAMIC.name)
        _themeScheme.value = try {
            AppThemeScheme.valueOf(savedTheme ?: AppThemeScheme.DYNAMIC.name)
        } catch (_: Exception) {
            AppThemeScheme.DYNAMIC
        }

        val savedDarkMode = prefs.getString("dark_mode", DarkModeConfig.FOLLOW_SYSTEM.name)
        _darkModeConfig.value = try {
            DarkModeConfig.valueOf(savedDarkMode ?: DarkModeConfig.FOLLOW_SYSTEM.name)
        } catch (_: Exception) {
            DarkModeConfig.FOLLOW_SYSTEM
        }

        enableEdgeToEdge()

        // 獲取啟動時的頁面索引
        currentPage = intent.getIntExtra("target_page", 0)

        handleExternalFileIntent(intent)

        setContent {
            val currentTheme by themeScheme.collectAsState()
            val currentDarkMode by darkModeConfig.collectAsState()

            RulerTheme(
                themeScheme = currentTheme,
                darkModeConfig = currentDarkMode,
            ) {
                MainScreen(initialPage = currentPage)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetPage = intent.getIntExtra("target_page", -1)
        if (targetPage != -1) {
            currentPage = targetPage
        }
        handleExternalFileIntent(intent)
    }

    private fun handleExternalFileIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data = intent.data
        if (data != null && (action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT || action == Intent.ACTION_SEND)) {
            _externalTxtUri.value = data
            currentPage = 1 // 1 是記事本頁面 (BottomBarScreen.Note)
        }
    }
}
