package com.simpleweather.app.model

import java.time.Instant
import java.time.LocalDate
import com.simpleweather.app.ui.WeatherIconKind
import com.simpleweather.app.ui.weatherIconKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDisplayTest {
    private val weather = WeatherBundle(
        place = Place("1", "台北", latitude = 25.0, longitude = 121.5),
        timezone = "Asia/Taipei",
        utcOffsetSeconds = 28_800,
        current = CurrentWeather(observedAt = "2026-09-17T12:00", isDay = true),
        daily = listOf(
            DailyForecast(
                date = "2026-09-17",
                weatherCode = 0,
                maxTemperature = 30.0,
                minTemperature = 22.0,
                sunrise = "2026-09-17T05:40",
                sunset = "2026-09-17T17:56",
            )
        ),
        fetchedAtEpochMillis = 0,
    )

    @Test fun `sunrise is day and sunset is night`() {
        assertFalse(weather.isDaylightAt(Instant.parse("2026-09-16T21:39:59Z")))
        assertTrue(weather.isDaylightAt(Instant.parse("2026-09-16T21:40:00Z")))
        assertTrue(weather.isDaylightAt(Instant.parse("2026-09-17T09:55:59Z")))
        assertFalse(weather.isDaylightAt(Instant.parse("2026-09-17T09:56:00Z")))
    }

    @Test fun `clear night selects moon instead of sun`() {
        assertEquals(WeatherIconKind.MOON, weatherIconKind(0, isDay = false))
        assertEquals(WeatherIconKind.SUN, weatherIconKind(0, isDay = true))
    }

    @Test fun `formats local time and timezone`() {
        val instant = Instant.parse("2026-09-17T04:34:56Z")
        assertEquals("12:34:56", formatLocalTime(weather, instant))
        assertEquals("Asia/Taipei · GMT+08:00", weather.timezoneLabel(instant))
    }

    @Test fun `maps moon phase and US AQI labels`() {
        assertEquals("新月", moonPhaseLabel(0.0))
        assertEquals("滿月", moonPhaseLabel(0.5))
        assertEquals("50 · 良好", aqiLabel(50))
        assertEquals("151 · 不健康", aqiLabel(151))
    }

    @Test fun `maps AQI and PM25 health descriptions at boundaries`() {
        assertEquals("良好（0–50）", aqiDescription(50))
        assertEquals("普通（51–100）", aqiDescription(51))
        assertEquals("對敏感族群不健康（101–150）", aqiDescription(150))
        assertEquals("對所有族群不健康（151–200）", aqiDescription(151))
        assertEquals("良好（0.0–15.4 μg/m³）", pm25Description(15.4))
        assertEquals("普通（15.5–35.4 μg/m³）", pm25Description(15.5))
        assertEquals("對敏感族群不健康（35.5–54.4 μg/m³）", pm25Description(35.5))
        assertEquals("對所有族群不健康（54.5–150.4 μg/m³）", pm25Description(54.5))
        assertEquals("危害（>150.4 μg/m³）", pm25Description(150.5))
    }

    @Test fun `maps UV index descriptions at boundaries`() {
        assertEquals("低量級", uvDescription(2.9))
        assertEquals("中量級", uvDescription(3.0))
        assertEquals("高量級", uvDescription(6.0))
        assertEquals("過量級", uvDescription(8.0))
        assertEquals("危險級", uvDescription(11.0))
    }

    @Test fun `maps sea level pressure descriptions at boundaries`() {
        assertEquals("強烈低壓 · 可能伴隨強風、豪雨", pressureDescription(979.9))
        assertEquals("低氣壓 · 多雲、較易降雨", pressureDescription(980.0))
        assertEquals("接近平均 · 標準海平面約 1013 hPa", pressureDescription(1011.0))
        assertEquals("高氣壓 · 通常晴朗、少雲", pressureDescription(1020.0))
        assertEquals("強高氣壓 · 通常穩定、較乾燥", pressureDescription(1040.0))
    }

    @Test fun `formats and classifies visibility`() {
        assertEquals("12.4 km", formatVisibility(12_400.0))
        assertEquals("10 km", formatVisibility(10_000.0))
        assertEquals("良好 · 視野清晰", visibilityDescription(10_000.0))
        assertEquals("普通 · 遠景略受影響", visibilityDescription(5_000.0))
        assertEquals("不佳 · 行車請留意", visibilityDescription(1_000.0))
        assertEquals("很差 · 視線明顯受限", visibilityDescription(999.0))
    }

    @Test fun `maps cloud cover descriptions at boundaries`() {
        assertEquals("晴朗", cloudCoverDescription(20))
        assertEquals("大致晴朗", cloudCoverDescription(21))
        assertEquals("局部多雲", cloudCoverDescription(41))
        assertEquals("多雲", cloudCoverDescription(61))
        assertEquals("陰天", cloudCoverDescription(81))
    }

    @Test fun `formats sunshine seconds as hours and minutes`() {
        assertEquals("2 小時 30 分", formatSunshineDuration(9_000.0))
    }

    @Test fun `formats wind force from kilometers per hour`() {
        assertEquals("0級 · 無風", windForceLabel(0.72))
        assertEquals("1級 · 軟風", windForceLabel(1.08))
        assertEquals("5級 · 清風", windForceLabel(36.0))
        assertEquals("13級 · 強烈颶風", windForceLabel(183.24))
        assertEquals("16級 · 超強颶風", windForceLabel(183.6))
    }

    @Test fun `selects and labels the next three local forecast days`() {
        val forecasts = listOf("2026-09-17", "2026-09-18", "2026-09-19", "2026-09-20").map { date ->
            DailyForecast(date, 0, 30.0, 22.0)
        }
        val result = weather.copy(daily = forecasts).futureForecasts(Instant.parse("2026-09-17T04:00:00Z"))

        assertEquals(listOf("2026-09-18", "2026-09-19", "2026-09-20"), result.map { it.date })
        assertEquals("明天", forecastDayLabel(result[0].date, LocalDate.parse("2026-09-17")))
        assertEquals("後天", forecastDayLabel(result[1].date, LocalDate.parse("2026-09-17")))
        assertEquals("大後天", forecastDayLabel(result[2].date, LocalDate.parse("2026-09-17")))
    }

    @Test fun `selects today through the next six local forecast days`() {
        val forecasts = (17..24).map { day ->
            DailyForecast("2026-09-$day", 0, 30.0, 22.0)
        }
        val result = weather.copy(daily = forecasts).forecastDays(Instant.parse("2026-09-17T04:00:00Z"))

        assertEquals(7, result.size)
        assertEquals("今天", forecastDayLabel(result[0].date, LocalDate.parse("2026-09-17")))
        assertEquals("星期一", forecastDayLabel(result[4].date, LocalDate.parse("2026-09-17")))
        assertEquals("星期三", forecastDayLabel(result[6].date, LocalDate.parse("2026-09-17")))
    }
}
