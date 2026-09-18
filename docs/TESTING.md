# 測試指南

## 自動化測試

### Debug 單元測試

```powershell
.\gradlew.bat testDebugUnitTest
```

### Prerelease 單元測試

```powershell
.\gradlew.bat testPrereleaseUnitTest
```

### Android Lint

```powershell
.\gradlew.bat lintDebug
```

報告位置：

```text
app/build/reports/lint-results-debug.html
```

### 完整本機驗證

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug testPrereleaseUnitTest assemblePrerelease
```

## 單元測試範圍

- WMO code 至場景映射。
- 日間太陽與夜間月亮圖示種類。
- 日出包含、日落排除的日夜邊界。
- IANA 時區、當地秒級時間與 GMT offset。
- 月相、US AQI 與日照時數格式化。
- 依地點當地日期篩選明天、後天及大後天。

## 實機／模擬器檢查

自動化測試無法取代以下驗證：

1. 首次定位權限允許、約略位置、拒絕與永久拒絕。
2. GPS 關閉、定位逾時及舊位置降級。
3. 城市搜尋、空結果、清除輸入及快速連續輸入。
4. 收藏新增、去重、移除與 20 個上限。
5. 飛航模式下有快取與無快取畫面。
6. 日出前後及日落前後的主題、圖示與系統列切換。
7. 晴、多雲、霧、雨、雪及雷雨手繪圖示。
8. 小螢幕、平板直式及系統大字體。
9. 三鍵導覽、手勢導覽、瀏海與狀態列 Insets。
10. 背景依天氣及晝夜顯示正確靜態配色，且不產生背景動畫。
11. 資料未滿 15 分鐘不自動更新，達到 15 分鐘時更新；進入背景後停止週期更新。

## 發布前檢查

- Version Code 已遞增。
- 正式版不使用 debug signing key。
- `SPEC.md`、README 與版本紀錄同步。
- Open-Meteo 與 CAMS attribution 仍可見。
- APK 或 AAB 已在最低與目標 Android 版本驗證。
- Git 工作區未包含 `local.properties`、金鑰或憑證。
