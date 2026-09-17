# 版本紀錄

本文件記錄使用者可見的重要變更，格式參考 [Keep a Changelog](https://keepachangelog.com/zh-TW/1.1.0/)。

## [1.0.0-prerelease.3] - 2026-09-17

### 新增

- GPS 約略定位、反向地理編碼與城市搜尋。
- 明天、後天及大後天天氣預報。
- 收藏城市、Room 天氣快取與 DataStore 選擇記憶。
- 濕度、降雨、風向、日照、月相、海拔及空氣品質資訊。
- 當地秒級時間、IANA 時區與 GMT offset。
- 依日出日落切換的 Material 3 淺色／深色主題。
- 手繪風格日夜天氣圖示與低幅度天空動畫。
- Adaptive、monochrome 與 legacy launcher icon。

### 改善

- 網路失敗時保留相同地點快取，不以全頁錯誤覆蓋內容。
- 狀態列與導覽列隨日夜主題切換。
- 城市搜尋提供 debounce、空結果、錯誤及快速清除操作。
