package com.simpleweather.app.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.DayOfWeek
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

fun aqiDescription(value: Int?): String? = when {
    value == null -> null
    value <= 50 -> "良好（0–50）"
    value <= 100 -> "普通（51–100）"
    value <= 150 -> "對敏感族群不健康（101–150）"
    value <= 200 -> "對所有族群不健康（151–200）"
    value <= 300 -> "非常不健康（201–300）"
    else -> "危害（301–500）"
}

fun pm25Description(value: Double?): String? = when {
    value == null -> null
    value <= 15.4 -> "良好（0.0–15.4 μg/m³）"
    value <= 35.4 -> "普通（15.5–35.4 μg/m³）"
    value <= 54.4 -> "對敏感族群不健康（35.5–54.4 μg/m³）"
    value <= 150.4 -> "對所有族群不健康（54.5–150.4 μg/m³）"
    else -> "危害（>150.4 μg/m³）"
}

fun uvDescription(value: Double?): String? = when {
    value == null -> null
    value < 3.0 -> "低量級"
    value < 6.0 -> "中量級"
    value < 8.0 -> "高量級"
    value < 11.0 -> "過量級"
    else -> "危險級"
}

fun pressureDescription(value: Double?): String? = when {
    value == null -> null
    value < 980.0 -> "強烈低壓 · 可能伴隨強風、豪雨"
    value < 1011.0 -> "低氣壓 · 多雲、較易降雨"
    value < 1020.0 -> "接近平均 · 標準海平面約 1013 hPa"
    value < 1040.0 -> "高氣壓 · 通常晴朗、少雲"
    else -> "強高氣壓 · 通常穩定、較乾燥"
}

fun formatVisibility(valueMeters: Double?): String {
    if (valueMeters == null) return "--"
    val kilometers = valueMeters.coerceAtLeast(0.0) / 1_000.0
    return if (kilometers % 1.0 == 0.0) {
        "${kilometers.toInt()} km"
    } else {
        "%.1f km".format(Locale.TAIWAN, kilometers)
    }
}

fun visibilityDescription(valueMeters: Double?): String? = when {
    valueMeters == null -> null
    valueMeters >= 10_000.0 -> "良好 · 視野清晰"
    valueMeters >= 5_000.0 -> "普通 · 遠景略受影響"
    valueMeters >= 1_000.0 -> "不佳 · 行車請留意"
    else -> "很差 · 視線明顯受限"
}

fun cloudCoverDescription(value: Int?): String? = when {
    value == null -> null
    value <= 20 -> "晴朗"
    value <= 40 -> "大致晴朗"
    value <= 60 -> "局部多雲"
    value <= 80 -> "多雲"
    else -> "陰天"
}

fun windForceLabel(speedKmh: Double?): String? {
    val metersPerSecond = speedKmh?.div(3.6) ?: return null
    return when {
        metersPerSecond <= 0.2 -> "0級 · 無風"
        metersPerSecond <= 1.5 -> "1級 · 軟風"
        metersPerSecond <= 3.3 -> "2級 · 輕風"
        metersPerSecond <= 5.4 -> "3級 · 微風"
        metersPerSecond <= 7.9 -> "4級 · 和風"
        metersPerSecond <= 10.7 -> "5級 · 清風"
        metersPerSecond <= 13.8 -> "6級 · 強風"
        metersPerSecond <= 17.1 -> "7級 · 疾風"
        metersPerSecond <= 20.7 -> "8級 · 大風"
        metersPerSecond <= 24.4 -> "9級 · 烈風"
        metersPerSecond <= 28.4 -> "10級 · 狂風"
        metersPerSecond <= 32.6 -> "11級 · 暴風"
        metersPerSecond <= 36.9 -> "12級 · 颶風"
        metersPerSecond <= 50.9 -> "13級 · 強烈颶風"
        else -> "16級 · 超強颶風"
    }
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

fun WeatherBundle.forecastDays(instant: Instant): List<DailyForecast> {
    val today = localDateTime(instant).toLocalDate()
    return daily.mapNotNull { forecast ->
        val date = runCatching { LocalDate.parse(forecast.date) }.getOrNull() ?: return@mapNotNull null
        if (!date.isBefore(today)) date to forecast else null
    }.sortedBy { it.first }.take(7).map { it.second }
}

fun forecastDayLabel(date: String, today: LocalDate): String {
    val forecastDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return date
    return when (ChronoUnit.DAYS.between(today, forecastDate)) {
        0L -> "今天"
        1L -> "明天"
        2L -> "後天"
        3L -> "大後天"
        else -> "星期${forecastDate.weekdayName()}"
    }
}

private fun LocalDate.weekdayName(): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "一"
    DayOfWeek.TUESDAY -> "二"
    DayOfWeek.WEDNESDAY -> "三"
    DayOfWeek.THURSDAY -> "四"
    DayOfWeek.FRIDAY -> "五"
    DayOfWeek.SATURDAY -> "六"
    DayOfWeek.SUNDAY -> "日"
}

private fun WeatherBundle.zoneId(): ZoneId = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneOffset.UTC)

private fun ZoneOffset.formatOffset(): String {
    val totalMinutes = totalSeconds / 60
    val sign = if (totalMinutes >= 0) "+" else "-"
    val absolute = kotlin.math.abs(totalMinutes)
    return "%s%02d:%02d".format(Locale.ROOT, sign, absolute / 60, absolute % 60)
}
