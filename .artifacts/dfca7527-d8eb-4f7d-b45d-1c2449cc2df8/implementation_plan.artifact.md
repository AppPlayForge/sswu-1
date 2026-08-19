# 實現彈窗背景模糊效果

當 app 中的對話框（卡片）彈出時，對底層頁面進行模糊化處理，以增強視覺層次感和焦點。

## 用戶審查要求

- 本方案使用 `Modifier.blur()`，這在 Android 12 (API 31) 及以上版本有效。
- 由於項目的 `minSdk` 已設置為 31，因此可以全面支持。

## 提議的變更

### [UI 組件]

#### [NEW] [BlurryContainer.kt](file:///D:/IdeaProjects/Android/sswu-1/app/src/main/java/com/example/myTools/ui/BlurryContainer.kt)
創建一個可複用的組件，封裝模糊邏輯。

### [功能頁面更新]

#### [MODIFY] [LunarBirthdayScreen.kt](file:///D:/IdeaProjects/Android/sswu-1/app/src/main/java/com/example/myTools/birthday/LunarBirthdayScreen.kt)
在農曆生日頁面應用模糊效果。

#### [MODIFY] [ToolsScreen.kt](file:///D:/IdeaProjects/Android/sswu-1/app/src/main/java/com/example/myTools/tools/ToolsScreen.kt)
在工具箱頁面（設置和主題對話框彈出時）應用模糊效果。

## 驗證計劃

### 手動驗證
1. 打開「農曆生日提醒」頁面。
2. 點擊右下角的「+」號彈出新增對話框，觀察背景是否模糊。
3. 長按卡片彈出編輯或刪除確認對話框，觀察背景是否模糊。
4. 打開「工具箱」頁面。
5. 點擊「設置」或「個性化主題」，觀察背景是否模糊。
