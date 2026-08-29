# 重新組織工具箱介面

根據用戶需求，將工具箱中的工具重新分組。
將「羅盤」、「尺規」、「車速」、「月經記錄」放在第一組。
將「添加小工具」、「數據管理」、「權限申請」（設置）、「打賞支持」放在第二組。
兩組之間使用水平分割線隔開。

## Proposed Changes

### [工具箱]

#### [MODIFY] [ToolsScreen.kt](file:///D:/IdeaProjects/Android/sswu-1/app/src/main/java/com/example/myTools/tools/ToolsScreen.kt)
- 將工具列表拆分為 `mainTools` 和 `otherTools`。
- 修改 `LazyVerticalGrid` 以顯示這兩組工具，並在中間插入一個跨列的 `HorizontalDivider`。

## Verification Plan

### Automated Tests
- 無

### Manual Verification
- 在設備上運行應用程序，導航至「工具箱」頁面。
- 驗證「月經記錄」下方是否有一條橫線。
- 驗證橫線下方是否依次排列著「添加小工具」、「數據管理」、「設置」（顯示為權限申請）和「打賞支持」。
- 點擊各個工具，確保其功能依然正常運作。
