package com.example.myTools.carspeed

import android.content.Context


//將所有與 SharedPreferences(共享偏好設定) 相關的讀寫操作封裝起來
class AppPreferences(context: Context) {

    // 定義 SharedPreferences 檔案的名稱和 Key
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_HELP_DIALOG_SHOWN = "help_dialog_shown"
        // 為螢幕恆亮模式定義一個 Key
        private const val KEY_WAKE_LOCK_MODE = "wake_lock_mode"
    }

    // 獲取 SharedPreferences 實例
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 檢查是否應該顯示新功能提示。
     * @return 如果從未顯示過，返回 true；否則返回 false。
     */
    fun shouldShowHelpDialog(): Boolean {
        // 預設值為 true (如果找不到這個 key，就代表是第一次)
        return sharedPreferences.getBoolean(KEY_HELP_DIALOG_SHOWN, true)
    }

    /**
     * 將新功能提示的狀態標記為「已顯示」。
     */
    fun setHelpDialogShown() {
        sharedPreferences.edit().putBoolean(KEY_HELP_DIALOG_SHOWN, false).apply()
    }


    // <<< 新增：一個函數，用於保存螢幕恆亮模式
    fun saveWakeLockMode(mode: WakeLockMode) {
        // SharedPreferences 不能直接存 Enum，但可以存它的名字 (String)
        sharedPreferences.edit().putString(KEY_WAKE_LOCK_MODE, mode.name).apply()
    }

    // <<< 新增：一個函數，用於讀取螢幕恆亮模式
    fun getWakeLockMode(): WakeLockMode {
        // 讀取儲存的字串，如果找不到，預設為 "OFF"
        val modeName = sharedPreferences.getString(KEY_WAKE_LOCK_MODE, WakeLockMode.OFF.name)
        return try {
            // 將字串轉換回 Enum
            WakeLockMode.valueOf(modeName ?: WakeLockMode.OFF.name)
        } catch (e: Exception) {
            // 如果轉換失敗（例如儲存的值損壞），返回安全的預設值
            WakeLockMode.OFF
        }
    }
}
