package com.simpleweather.app.data

import com.simpleweather.app.data.local.FavoriteDao
import com.simpleweather.app.data.local.FavoriteEntity
import com.simpleweather.app.data.local.UserPreferences
import com.simpleweather.app.data.local.WeatherCacheDao
import com.simpleweather.app.data.local.WeatherCacheEntity
import com.simpleweather.app.data.remote.ForecastApi
import com.simpleweather.app.data.remote.ForecastResponse
import com.simpleweather.app.data.remote.AirQualityApi
import com.simpleweather.app.data.remote.AirQualityResponse
import com.simpleweather.app.data.remote.GeocodingApi
import com.simpleweather.app.data.remote.GeocodingResultDto
import com.simpleweather.app.model.CurrentWeather
import com.simpleweather.app.model.AirQuality
import com.simpleweather.app.model.DailyForecast
import com.simpleweather.app.model.Place
import com.simpleweather.app.model.WeatherBundle
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class WeatherRepository(
    private val forecastApi: ForecastApi,
    private val airQualityApi: AirQualityApi,
    private val geocodingApi: GeocodingApi,
    private val favoriteDao: FavoriteDao,
    private val cacheDao: WeatherCacheDao,
    private val preferences: UserPreferences,
    private val json: Json,
) {
    val favorites: Flow<List<Place>> = favoriteDao.observeAll().map { entities ->
        entities.map { it.toPlace() }
    }

    suspend fun selectedPlace(): Place? = preferences.selectedPlace()

    suspend fun select(place: Place) = preferences.setSelectedPlace(place)

    suspend fun cached(place: Place): WeatherBundle? = cacheDao.get(place.cacheKey)?.payload
        ?.let { runCatching { json.decodeFromString<WeatherBundle>(it) }.getOrNull() }

    suspend fun refresh(place: Place): WeatherBundle = coroutineScope {
        val weatherRequest = async { forecastApi.forecast(place.latitude, place.longitude) }
        val airQualityRequest = async {
            runCatching { airQualityApi.current(place.latitude, place.longitude) }.getOrNull()
        }
        val response = weatherRequest.await()
        val bundle = response.toBundle(place, System.currentTimeMillis(), airQualityRequest.await())
        cacheDao.upsert(
            WeatherCacheEntity(
                cacheKey = place.cacheKey,
                payload = json.encodeToString(bundle),
                fetchedAt = bundle.fetchedAtEpochMillis,
            )
        )
        bundle
    }

    suspend fun search(keyword: String): List<Place> = geocodingApi.search(keyword.trim()).results
        .mapNotNull { it.toPlace() }

    suspend fun addFavorite(place: Place): FavoriteResult {
        if (place.isCurrentLocation) return FavoriteResult.NotAllowed
        if (favoriteDao.contains(place.id)) return FavoriteResult.AlreadyExists
        if (favoriteDao.count() >= 20) return FavoriteResult.LimitReached
        favoriteDao.insert(place.toEntity())
        return FavoriteResult.Added
    }

    suspend fun removeFavorite(place: Place) = favoriteDao.delete(place.id)

    suspend fun markRationaleSeen() = preferences.markLocationRationaleSeen()
    suspend fun hasSeenRationale(): Boolean = preferences.hasSeenLocationRationale()

    suspend fun cleanOldCache() {
        val favoriteKeys = favoriteDao.observeAll().first().map { "city:${it.id}" }
        cacheDao.deleteOld(
            before = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
            favoriteKeys = favoriteKeys,
        )
    }

    private fun ForecastResponse.toBundle(
        place: Place,
        fetchedAt: Long,
        airQualityResponse: AirQualityResponse?,
    ): WeatherBundle {
        val currentDto = current ?: error("回應缺少目前天氣")
        val dailyDto = daily ?: error("回應缺少每日預報")
        val forecasts = dailyDto.time.indices.mapNotNull { index ->
            val date = dailyDto.time.getOrNull(index) ?: return@mapNotNull null
            val code = dailyDto.weatherCode.getOrNull(index) ?: return@mapNotNull null
            val max = dailyDto.maxTemperature.getOrNull(index) ?: return@mapNotNull null
            val min = dailyDto.minTemperature.getOrNull(index) ?: return@mapNotNull null
            DailyForecast(
                date = date,
                weatherCode = code,
                maxTemperature = max,
                minTemperature = min,
                maxApparentTemperature = dailyDto.maxApparentTemperature.getOrNull(index),
                minApparentTemperature = dailyDto.minApparentTemperature.getOrNull(index),
                precipitationProbability = dailyDto.precipitationProbability.getOrNull(index),
                precipitationSum = dailyDto.precipitationSum.getOrNull(index),
                sunrise = dailyDto.sunrise.getOrNull(index),
                sunset = dailyDto.sunset.getOrNull(index),
                sunshineDurationSeconds = dailyDto.sunshineDuration.getOrNull(index),
                moonPhase = dailyDto.moonPhase.getOrNull(index),
                maxWindSpeed = dailyDto.maxWindSpeed.getOrNull(index),
                uvIndexMax = dailyDto.uvIndexMax.getOrNull(index),
            )
        }
        require(forecasts.isNotEmpty()) { "回應缺少可用預報" }
        val resolvedPlace = place.copy(timezone = timezone ?: place.timezone)
        return WeatherBundle(
            place = resolvedPlace,
            timezone = timezone ?: place.timezone ?: "UTC",
            utcOffsetSeconds = utcOffsetSeconds ?: 0,
            current = CurrentWeather(
                observedAt = currentDto.time.orEmpty(),
                temperature = currentDto.temperature,
                apparentTemperature = currentDto.apparentTemperature,
                relativeHumidity = currentDto.relativeHumidity,
                precipitationProbability = currentDto.precipitationProbability,
                precipitation = currentDto.precipitation,
                weatherCode = currentDto.weatherCode,
                windSpeed = currentDto.windSpeed,
                windDirection = currentDto.windDirection,
                pressureMsl = currentDto.pressureMsl,
                visibilityMeters = currentDto.visibility,
                cloudCover = currentDto.cloudCover,
                uvIndex = currentDto.uvIndex,
                isDay = currentDto.isDay != 0,
            ),
            daily = forecasts.take(4),
            fetchedAtEpochMillis = fetchedAt,
            elevationMeters = elevation,
            airQuality = airQualityResponse?.current?.let {
                AirQuality(observedAt = it.time, usAqi = it.usAqi, pm25 = it.pm25)
            },
        )
    }

    private fun GeocodingResultDto.toPlace(): Place? {
        val safeName = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        val normalizedName = safeName.replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
        val normalizedCountry = countryCode.orEmpty().lowercase(Locale.ROOT)
        val fallback = "fallback:$normalizedCountry:$normalizedName:${"%.4f".format(Locale.ROOT, lat)}:${"%.4f".format(Locale.ROOT, lon)}"
        return Place(
            id = id?.toString() ?: fallback,
            name = safeName,
            adminArea = admin1,
            country = country,
            countryCode = countryCode,
            latitude = lat,
            longitude = lon,
            timezone = timezone,
        )
    }

    private fun FavoriteEntity.toPlace() = Place(
        id, name, adminArea, country, countryCode, latitude, longitude, timezone, isFavorite = true
    )

    private fun Place.toEntity() = FavoriteEntity(
        id, name, adminArea, country, countryCode, latitude, longitude, timezone, System.currentTimeMillis()
    )
}

enum class FavoriteResult { Added, AlreadyExists, LimitReached, NotAllowed }
