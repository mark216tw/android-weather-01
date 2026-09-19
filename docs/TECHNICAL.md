# 技術文件

## 建置設定

| 項目 | 值 |
| --- | --- |
| Namespace | `com.simpleweather.app` |
| 正式套件 ID | `com.simpleweather.app` |
| Prerelease 套件 ID | `com.simpleweather.app.prerelease` |
| Min SDK | 26 |
| Target / Compile SDK | 36 |
| Java Toolchain | 17 |
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.0 |
| Kotlin | 2.2.10 |

主要版本以 `app/build.gradle.kts` 與根目錄 `build.gradle.kts` 為準。

## Build Types

- `debug`：開發與偵錯。
- `release`：正式建置，簽署設定由發布者提供。
- `prerelease`：以 debug 為基礎、不可偵錯、使用 `.prerelease` 套件後綴及本機 debug key，僅供測試。

## API

### 天氣預報

```text
GET https://api.open-meteo.com/v1/forecast
```

重要參數：

- `timezone=auto`
- `forecast_days=4`
- `temperature_unit=celsius`
- `wind_speed_unit=kmh`
- `precipitation_unit=mm`
- `current`：溫度、體感、濕度、降雨機率、降雨、WMO code、風速、風向、海平面氣壓、能見度、雲量、UV、日夜
- `daily`：溫度、體感、降雨、日出日落、日照、月相及最大風速

### 城市搜尋

```text
GET https://geocoding-api.open-meteo.com/v1/search
```

使用 `count=10`、`language=zh`、`format=json`。搜尋輸入至少 2 字元並套用 300 毫秒 debounce。

### 空氣品質

```text
GET https://air-quality-api.open-meteo.com/v1/air-quality
```

目前要求 `us_aqi,pm2_5`。空品請求採可選降級，不影響天氣主內容。

## 網路設定

- 連線逾時：10 秒
- 讀取逾時：15 秒
- JSON：忽略未知欄位，避免 API 新增欄位造成解析失敗
- DTO 欄位採可空設計

## 定位

1. 接受 `ACCESS_COARSE_LOCATION` 或 `ACCESS_FINE_LOCATION`。
2. 最近位置需在 30 分鐘內且精度不大於 10 公里。
3. 不符合時使用 `getCurrentLocation`，逾時 10 秒。
4. 若單次定位失敗，可降級使用 24 小時內最近位置並提示使用者。
5. Android Geocoder 反查地名逾時 5 秒；失敗時使用「目前位置」。
6. 不持續監聽位置，不要求背景權限。

## 快取與識別

- 城市快取鍵：`city:{Open-Meteo ID}`。
- GPS 快取鍵：`current_location`。
- 缺少城市 ID 時，使用國家代碼、正規化名稱及四位小數經緯度建立 fallback ID。
- 快取超過 2 小時視為過舊，但網路失敗時仍可顯示。
- 非收藏城市超過 30 天可清理。
- 收藏與天氣快取分開保存。
- `WeatherBundle` 以 Kotlin Serialization JSON 存入 Room；新增可選欄位需提供預設值以相容舊快取。

Room schema 位於：

```text
app/schemas/com.simpleweather.app.data.local.WeatherDatabase/1.json
```

## 時區與日夜

- API 時間視為查詢地點當地時間。
- IANA 時區用於顯示時間及處理夏令時間。
- 每秒更新畫面上的當地時間。
- App 位於前景時每 15 分鐘檢查天氣資料年齡，資料達 15 分鐘才自動更新；回到前景時立即執行一次相同檢查。
- App 進入背景後停止前景自動更新迴圈。
- 目前時間落在當日日出至日落之間時使用淺色主題，否則使用深色主題。
- 若日出日落缺值，才降級使用 API `is_day`。
- 預報以地點當地日期排除今天，再取明天、後天與大後天。

## 授權與 Attribution

專案程式碼採 MIT License。使用 Open-Meteo 與 CAMS 資料時，UI 保留以下來源：

```text
天氣資料：Open-Meteo.com
空氣品質資料：CAMS / Open-Meteo
```
