package com.example.myTools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

object ShareUtils {

    /**
     * 分享 App 功能：包含複製分享內容至剪貼簿與啟動系統分享面板
     */
    fun shareApp(context: Context, customText: String? = null) {
        val shareText = customText ?: run {
            val downloadUrl = "https://play.google.com/store/apps/details?id=com.example.ruler"
            "八字命盤工具：專業八字、農曆、擇日、筆記綜合工具。下載體驗：$downloadUrl"
        }

        // 自動複製到剪貼簿，解決部分通訊軟體（如微信/QQ/LINE）無法自動讀取 Intent 文字的問題
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("App Share", shareText)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "分享內容已複製到剪貼板", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            // 忽略剪貼簿異常
        }

        // 啟動系統分享 chooser
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享給好友"))
        } catch (_: Exception) {
            Toast.makeText(context, "無法開啟分享選單", Toast.LENGTH_SHORT).show()
        }
    }
}
