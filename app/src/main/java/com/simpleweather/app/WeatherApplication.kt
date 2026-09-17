package com.simpleweather.app

import android.app.Application
import androidx.room.Room
import com.google.android.gms.location.LocationServices
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.simpleweather.app.data.LocationProvider
import com.simpleweather.app.data.WeatherRepository
import com.simpleweather.app.data.local.UserPreferences
import com.simpleweather.app.data.local.WeatherDatabase
import com.simpleweather.app.data.remote.ForecastApi
import com.simpleweather.app.data.remote.AirQualityApi
import com.simpleweather.app.data.remote.GeocodingApi
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class WeatherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val converter = json.asConverterFactory("application/json".toMediaType())
    private val forecastApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(http)
        .addConverterFactory(converter)
        .build()
        .create(ForecastApi::class.java)
    private val geocodingApi = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(http)
        .addConverterFactory(converter)
        .build()
        .create(GeocodingApi::class.java)
    private val airQualityApi = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/")
        .client(http)
        .addConverterFactory(converter)
        .build()
        .create(AirQualityApi::class.java)
    private val database = Room.databaseBuilder(application, WeatherDatabase::class.java, "weather.db").build()
    private val preferences = UserPreferences(application, json)

    val repository = WeatherRepository(
        forecastApi,
        airQualityApi,
        geocodingApi,
        database.favoriteDao(),
        database.cacheDao(),
        preferences,
        json,
    )
    val locationProvider = LocationProvider(
        application,
        LocationServices.getFusedLocationProviderClient(application),
    )
}
