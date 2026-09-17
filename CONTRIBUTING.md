# 貢獻指南

感謝協助改善「簡單天氣」。提交變更前，請先確認修改符合 [SPEC.md](SPEC.md) 與既有繁體中文介面。

## 開發流程

1. Fork 儲存庫並建立功能分支。
2. 使用小範圍、目的明確的 commit。
3. 新功能或錯誤修正需補上適當測試。
4. 執行完整驗證：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

5. 建立 Pull Request，說明行為變更、測試結果與相關 Issue。

## 程式風格

- 使用 Kotlin 官方程式風格。
- UI 使用 Jetpack Compose 與既有 Material 3 主題。
- 優先使用可空模型安全處理外部 API 缺值。
- 不在主執行緒執行網路、資料庫或同步地名反查。
- 使用查詢地點時區處理日期，不使用裝置時區代替。
- 新增互動元件時提供至少 `48 dp` 點擊範圍及內容描述。
- 避免新增不必要的抽象層、背景工作或背景定位。

## Commit 建議

使用簡短、祈使語氣且能描述目的的訊息，例如：

```text
Add weather cache fallback
Fix night icon contrast
Document location privacy
```

## 問題回報

請提供 Android 版本、裝置型號、App 版本、重現步驟、預期結果與實際結果。請勿在 Issue 中張貼精確住址、GPS 座標、Token 或其他敏感資訊。
