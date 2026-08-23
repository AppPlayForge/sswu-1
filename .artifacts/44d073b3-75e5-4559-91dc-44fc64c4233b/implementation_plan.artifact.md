# 優化「添加八字紀錄」UI 界面與字體顏色統一

本計劃旨在提升「添加八字紀錄」對話框的視覺美感與交互體驗，確保字體顏色、間距及佈局與整體 Material 3 主題保持一致。

## Proposed Changes

### [Component Name] Bazi UI

#### [MODIFY] [AddBaZiDialog.kt](file:///D:/IdeaProjects/Android/sswu-1/app/src/main/java/com/example/myTools/bazi/AddBaZiDialog.kt)
- **標題與段落優化**：
    - 將「出生時間」、「出生時分」、「出生地點」等段落標題統一設置為 `MaterialTheme.colorScheme.primary` 顏色，並優化其與分割線的間距。
    - 增加段落標題的字體粗細，使其更具結構感。
- **佈局對齊優化**：
    - 調整「年/月/日」輸入框的權重，使其在視覺上更對稱（例如：1f, 1f, 1f 或更合理的分配）。
    - 調整「性別」與「農曆模式」行的對齊方式，增加與輸入框之間的縱向間距。
- **性別選擇器優化**：
    - 將性別選擇按鈕優化為更具「切換感」的樣式，確保選中與未選中狀態的顏色對比度適中且符合主題色。
- **字體顏色統一**：
    - 所有輸入框的 `label` 統一使用 `labelMedium` 或 `labelLarge` 並確保顏色為 `onSurfaceVariant`。
    - 段落標題統一使用 `titleSmall` 或 `titleMedium` 並著以 `primary` 色。
- **細節調整**：
    - 調整對話框的內邊距 (Padding) 和元素間距 (Spacing)，使整體佈局不顯得擁擠。

## Verification Plan

### Manual Verification
- **部署並運行應用**：
    - 打開「添加八字紀錄」對話框。
    - 檢查標題顏色是否統一為主題色（如大地紅）。
    - 檢查輸入框佈局是否整齊，特別是日期和時間部分。
    - 驗證性別按鈕的選中狀態是否美觀且易於區分。
    - 確保在切換農曆模式時，佈局不會發生意外跳動。
- **視覺對比**：
    - 與用戶提供的截圖進行對比，確保優化後的介面更專業、色彩更和諧。
