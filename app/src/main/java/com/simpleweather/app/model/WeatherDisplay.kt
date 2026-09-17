package com.simpleweather.app.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

fun WeatherBundle.localDateTime(instant: Instant): LocalDateTime =
    LocalDateTime.ofInstant(instant, zoneId())

fun WeatherBundle.isDaylightAt(instant: Instant): Boolean {
    val localNow = localDateTime(instant)
    val today = daily.firstOrNull { it.date == localNow.toLocalDate().toString() } ?: return current.isDay
    val sunrise = today.sunrise?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    val sunset = today.sunset?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    return if (sunrise != null && sunset != null) !localNow.isBefore(sunrise) && localNow.isBefore(sunset) else current.isDay
}

fun WeatherBundle.timezoneLabel(instant: Instant): String {
    val zone = zoneId()
    val offset = zone.rules.getOffset(instant)
    return "$timezone · GMT${offset.formatOffset()}"
}

fun moonPhaseLabel(value: Double?): String = when {
    value == null -> "--"
    value < 0.03 || value >= 0.97 -> "新月"
    value < 0.22 -> "眉月"
    value < 0.28 -> "上弦月"
    value < 0.47 -> "盈凸月"
    value < 0.53 -> "滿月"
    value < 0.72 -> "虧凸月"
    value < 0.78 -> "下弦月"
    else -> "殘月"
}

fun aqiLabel(value: Int?): String = when {
    value == null -> "--"
    value <= 50 -> "$value · 良好"
    value <= 100 -> "$value · 普通"
    value <= 150 -> "$value · 敏感族群不健康"
    value <= 200 -> "$value · 不健康"
    value <= 300 -> "$value · 非常不健康"
    else -> "$value · 危害"
}

fun formatSunshineDuration(seconds: Double?): String {
    if (seconds == null) return "--"
    val totalMinutes = (seconds / 60).toInt().coerceAtLeast(0)
    return "${totalMinutes / 60} 小時 ${totalMinutes % 60} 分"
}

fun formatLocalTime(weather: WeatherBundle, instant: Instant): String =
    weather.localDateTime(instant).format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.TAIWAN))

fun WeatherBundle.futureForecasts(instant: Instant): List<DailyForecast> {
    val today = localDateTime(instant).toLocalDate()
    return daily.mapNotNull { forecast ->
        val date = runCatching { LocalDate.parse(forecast.date) }.getOrNull() ?: return@mapNotNull null
        if (date.isAfter(today)) date to forecast else null
    }.sortedBy { it.first }.take(3).map { it.second }
}

fun forecastDayLabel(date: String, today: LocalDate): String {
    val forecastDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
    return when (ChronoUnit.DAYS.between(today, forecastDate)) {
        1L -> "明天"
        2L -> "後天"
        3L -> "大後天"
        else -> forecastDate.format(DateTimeFormatter.ofPattern("M/d", Locale.TAIWAN))
    }
}

private fun WeatherBundle.zoneId(): ZoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC)

private fun ZoneOffset.formatOffset(): String {
    val totalMinutes = totalSeconds / 60
    val sign = if (totalMinutes >= 0) "+" else "-"
    val absolute = kotlin.math.abs(totalMinutes)
    return "%s%02d:%02d".format(Locale.ROOT, sign, absolute / 60, absolute % 60)
}
