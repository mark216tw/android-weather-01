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
        assertEquals("🌑 新月", moonPhaseLabel(0.0))
        assertEquals("🌕 滿月", moonPhaseLabel(0.5))
        assertEquals("50 · 良好", aqiLabel(50))
        assertEquals("151 · 不健康", aqiLabel(151))
    }

    @Test fun `formats sunshine seconds as hours and minutes`() {
        assertEquals("2 小時 30 分", formatSunshineDuration(9_000.0))
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
}
