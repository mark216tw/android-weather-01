package com.simpleweather.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface ForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("temperature_unit") temperatureUnit: String = "celsius",
        @Query("wind_speed_unit") windSpeedUnit: String = "kmh",
        @Query("precipitation_unit") precipitationUnit: String = "mm",
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
    ): ForecastResponse

    companion object {
        const val CURRENT_FIELDS = "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_direction_10m,pressure_msl,visibility,cloud_cover,uv_index,is_day"
        const val DAILY_FIELDS = "weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,precipitation_probability_max,precipitation_sum,sunrise,sunset,sunshine_duration,moon_phase,wind_speed_10m_max,uv_index_max"
    }
}

interface AirQualityApi {
    @GET("v1/air-quality")
    suspend fun current(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("timezone") timezone: String = "auto",
        @Query("current") current: String = "us_aqi,pm2_5",
    ): AirQualityResponse
}

interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") keyword: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "zh",
        @Query("format") format: String = "json",
    ): GeocodingResponse
}

@Serializable
data class ForecastResponse(
    val elevation: Double? = null,
    val timezone: String? = null,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    val current: CurrentDto? = null,
    val daily: DailyDto? = null,
)

@Serializable
data class CurrentDto(
    val time: String? = null,
    @SerialName("temperature_2m") val temperature: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity: Int? = null,
    @SerialName("precipitation_probability") val precipitationProbability: Int? = null,
    val precipitation: Double? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    @SerialName("wind_speed_10m") val windSpeed: Double? = null,
    @SerialName("wind_direction_10m") val windDirection: Double? = null,
    @SerialName("pressure_msl") val pressureMsl: Double? = null,
    val visibility: Double? = null,
    @SerialName("cloud_cover") val cloudCover: Int? = null,
    @SerialName("uv_index") val uvIndex: Double? = null,
    @SerialName("is_day") val isDay: Int? = null,
)

@Serializable
data class DailyDto(
    val time: List<String?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val maxTemperature: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val minTemperature: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_max") val maxApparentTemperature: List<Double?> = emptyList(),
    @SerialName("apparent_temperature_min") val minApparentTemperature: List<Double?> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    val sunrise: List<String?> = emptyList(),
    val sunset: List<String?> = emptyList(),
    @SerialName("sunshine_duration") val sunshineDuration: List<Double?> = emptyList(),
    @SerialName("moon_phase") val moonPhase: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m_max") val maxWindSpeed: List<Double?> = emptyList(),
    @SerialName("uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
)

@Serializable
data class AirQualityResponse(val current: AirQualityCurrentDto? = null)

@Serializable
data class AirQualityCurrentDto(
    val time: String? = null,
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
)

@Serializable
data class GeocodingResponse(val results: List<GeocodingResultDto> = emptyList())

@Serializable
data class GeocodingResultDto(
    val id: Long? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val admin1: String? = null,
)
