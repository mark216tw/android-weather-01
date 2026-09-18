# 簡單天氣

「簡單天氣」是一款以單手操作、清楚資訊與沉浸式日夜介面為核心的 Android 天氣 App。App 可使用裝置約略位置取得所在地天氣，也能搜尋、收藏及切換城市；即使網路暫時中斷，仍會優先顯示最近一次成功取得的快取資料。

## 功能特色

- 目前天氣、明天、後天與大後天預報
- GPS 約略定位與城市搜尋替代流程
- 收藏最多 20 個城市並記住最後選擇
- 溫度、體感溫度、濕度、降雨、風速、風向與蒲福風級
- 日出、日落、日照時數、月相、海拔、氣壓、UV 與當地時間
- US AQI 與 PM2.5 空氣品質
- 依當地日出、日落自動切換淺色與深色主題
- 手繪風格天氣圖示、縮小的黃橘塗鴉太陽 App 圖示與鮮艷靜態天氣背景
- 緊湊半透明目前天氣資訊區塊，包含 AQI／PM2.5 分級描述與顏色
- 下拉更新、前景每 15 分鐘自動更新、離線快取及過期資料提示
- Adaptive Icon、Android 13 monochrome icon 與舊版圖示

## 畫面與平台

- 語言：繁體中文
- 顯示方向：直式
- 最低版本：Android 8.0（API 26）
- 目標版本：Android 16（API 36）
- 單位：攝氏、公里／小時、毫米

## 技術棧

- Kotlin、Coroutines、StateFlow
- Jetpack Compose、Material 3、ViewModel
- Retrofit、Kotlin Serialization、OkHttp
- Room、DataStore
- Google Play Services Location
- Open-Meteo Weather、Geocoding 與 Air Quality API

## 快速開始

### 必要環境

- JDK 17
- Android SDK 36
- Android Studio 或可執行 Gradle Wrapper 的環境

### 建置 Debug APK

Windows：

```powershell
.\gradlew.bat assembleDebug
```

macOS／Linux：

```bash
./gradlew assembleDebug
```

APK 會輸出至：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 建置 Prerelease APK

```powershell
.\gradlew.bat testPrereleaseUnitTest assemblePrerelease
```

輸出位置：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

Prerelease 使用獨立套件 ID `com.simpleweather.app.prerelease`，可與正式版並存；目前使用本機 debug key 簽署，僅供測試。

## 測試

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

目前單元測試涵蓋 WMO 天氣代碼、日夜圖示選擇、日出日落邊界、當地時間與時區、月相、AQI、日照格式及未來三日篩選。

## 文件

- [文件索引](docs/README.md)
- [使用指南](docs/USER_GUIDE.md)
- [系統架構](docs/ARCHITECTURE.md)
- [技術文件](docs/TECHNICAL.md)
- [系統設計](docs/SYSTEM_DESIGN.md)
- [測試指南](docs/TESTING.md)
- [隱私說明](docs/PRIVACY.md)
- [產品規格](SPEC.md)
- [貢獻指南](CONTRIBUTING.md)
- [版本紀錄](CHANGELOG.md)
- [安全政策](SECURITY.md)

## 資料來源

- 天氣、地理編碼及空氣品質 API：[Open-Meteo](https://open-meteo.com/)
- 空氣品質模型資料：Copernicus Atmosphere Monitoring Service（CAMS）

天氣與空氣品質為模型預報資料，僅供一般參考，不應作為災害應變、醫療或其他高風險決策的唯一依據。

## 授權

本專案程式碼以 [MIT License](LICENSE) 授權。第三方服務與相依套件仍適用各自的授權及使用條款。
