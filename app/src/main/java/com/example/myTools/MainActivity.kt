package com.example.myTools

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.myTools.ui.theme.RulerTheme

class MainActivity : ComponentActivity() {
    private var currentPage by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // 獲取啟動時的頁面索引
        currentPage = intent.getIntExtra("target_page", 0)

        setContent {
            RulerTheme {
                MainScreen(initialPage = currentPage)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 當 App 已在運行時點擊 Widget，更新頁面索引
        val targetPage = intent.getIntExtra("target_page", -1)
        if (targetPage != -1) {
            currentPage = targetPage
        }
    }
}
