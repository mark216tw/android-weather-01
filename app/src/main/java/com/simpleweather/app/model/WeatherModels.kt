package com.simpleweather.app.model

import kotlinx.serialization.Serializable

@Serializable
data class Place(
    val id: String,
    val name: String,
    val adminArea: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    val isCurrentLocation: Boolean = false,
    val isFavorite: Boolean = false,
) {
    val subtitle: String
        get() = listOfNotNull(adminArea, country).distinct().joinToString(" · ")

    val cacheKey: String
        get() = if (isCurrentLocation) "current_location" else "city:$id"
}

@Serializable
data class CurrentWeather(
    val observedAt: String,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val relativeHumidity: Int? = null,
    val precipitationProbability: Int? = null,
    val precipitation: Double? = null,
    val weatherCode: Int? = null,
    val windSpeed: Double? = null,
    val windDirection: Double? = null,
    val isDay: Boolean = true,
)

@Serializable
data class DailyForecast(
    val date: String,
    val weatherCode: Int,
    val maxTemperature: Double,
    val minTemperature: Double,
    val maxApparentTemperature: Double? = null,
    val minApparentTemperature: Double? = null,
    val precipitationProbability: Int? = null,
    val precipitationSum: Double? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val sunshineDurationSeconds: Double? = null,
    val moonPhase: Double? = null,
    val maxWindSpeed: Double? = null,
)

@Serializable
data class AirQuality(
    val observedAt: String? = null,
    val usAqi: Int? = null,
    val pm25: Double? = null,
)

@Serializable
data class WeatherBundle(
    val place: Place,
    val timezone: String,
    val utcOffsetSeconds: Int,
    val current: CurrentWeather,
    val daily: List<DailyForecast>,
    val fetchedAtEpochMillis: Long,
    val elevationMeters: Double? = null,
    val airQuality: AirQuality? = null,
)

enum class WeatherScene { CLEAR, CLOUDY, FOG, RAIN, SNOW, THUNDERSTORM }

data class WeatherCondition(
    val description: String,
    val scene: WeatherScene,
)

fun weatherCondition(code: Int?): WeatherCondition = when (code) {
    0 -> WeatherCondition("晴朗", WeatherScene.CLEAR)
    1 -> WeatherCondition("大致晴朗", WeatherScene.CLEAR)
    2 -> WeatherCondition("局部多雲", WeatherScene.CLOUDY)
    3 -> WeatherCondition("陰天", WeatherScene.CLOUDY)
    45, 48 -> WeatherCondition("有霧", WeatherScene.FOG)
    51, 53, 55 -> WeatherCondition("毛毛雨", WeatherScene.RAIN)
    56, 57 -> WeatherCondition("凍毛毛雨", WeatherScene.RAIN)
    61, 63, 65 -> WeatherCondition("下雨", WeatherScene.RAIN)
    66, 67 -> WeatherCondition("凍雨", WeatherScene.RAIN)
    71, 73, 75, 77 -> WeatherCondition("降雪", WeatherScene.SNOW)
    80, 81, 82 -> WeatherCondition("陣雨", WeatherScene.RAIN)
    85, 86 -> WeatherCondition("陣雪", WeatherScene.SNOW)
    95, 96, 99 -> WeatherCondition("雷雨", WeatherScene.THUNDERSTORM)
    else -> WeatherCondition("天氣資料不明", WeatherScene.CLOUDY)
}
