package com.simpleweather.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simpleweather.app.model.DailyForecast
import com.simpleweather.app.model.Place
import com.simpleweather.app.model.WeatherBundle
import com.simpleweather.app.model.WeatherScene
import com.simpleweather.app.model.aqiDescription
import com.simpleweather.app.model.cloudCoverDescription
import com.simpleweather.app.model.formatLocalTime
import com.simpleweather.app.model.formatSunshineDuration
import com.simpleweather.app.model.formatVisibility
import com.simpleweather.app.model.forecastDayLabel
import com.simpleweather.app.model.futureForecasts
import com.simpleweather.app.model.localDateTime
import com.simpleweather.app.model.moonPhaseLabel
import com.simpleweather.app.model.pm25Description
import com.simpleweather.app.model.pressureDescription
import com.simpleweather.app.model.timezoneLabel
import com.simpleweather.app.model.uvDescription
import com.simpleweather.app.model.visibilityDescription
import com.simpleweather.app.model.windForceLabel
import com.simpleweather.app.model.weatherCondition
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    state: WeatherUiState,
    viewModel: WeatherViewModel,
    requestPermission: () -> Unit,
    openSettings: () -> Unit,
    now: Instant,
    isDaylight: Boolean,
) {
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val condition = weatherCondition(state.weather?.current?.weatherCode)
    WeatherBackground(condition.scene, isDaylight)
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { scaffoldPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing || state.isLocating,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
        ) {
            when {
                state.isLoading && state.weather == null -> LoadingContent()
                state.weather != null -> WeatherContent(state, viewModel, now, isDaylight)
                else -> EmptyState(state, viewModel, requestPermission, openSettings)
            }
        }
    }

    if (state.showLocationRationale) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLocationRationale,
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            title = { Text("顯示所在地天氣") },
            text = { Text("簡單天氣只會在使用 App 時取得大約位置，用來查詢當地天氣，不會在背景追蹤位置。") },
            confirmButton = {
                Button(onClick = { viewModel.acceptLocationRationale(); requestPermission() }) { Text("繼續") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLocationRationale) { Text("改用城市搜尋") }
            },
        )
    }

    if (state.showPlaceSheet) {
        PlaceSheet(state, viewModel)
    }
}

@Composable
private fun WeatherBackground(scene: WeatherScene, isDay: Boolean) {
    val colors = when {
        !isDay && scene == WeatherScene.CLEAR -> listOf(Color(0xFF172A58), Color(0xFF21427A), Color(0xFF385D91))
        !isDay && (scene == WeatherScene.CLOUDY || scene == WeatherScene.FOG) -> listOf(Color(0xFF203551), Color(0xFF315170), Color(0xFF4B6D8C))
        !isDay && scene == WeatherScene.THUNDERSTORM -> listOf(Color(0xFF241D4C), Color(0xFF3B2D6C), Color(0xFF5B4380))
        !isDay && scene == WeatherScene.SNOW -> listOf(Color(0xFF2A4770), Color(0xFF47749A), Color(0xFF7298B7))
        !isDay -> listOf(Color(0xFF1E3B65), Color(0xFF32618A), Color(0xFF4E83A4))
        scene == WeatherScene.CLEAR -> listOf(Color(0xFF8ADCF2), Color(0xFFBCECF5), Color(0xFFFFD88A))
        scene == WeatherScene.CLOUDY || scene == WeatherScene.FOG -> listOf(Color(0xFF9BC8D8), Color(0xFFC8E1E6), Color(0xFFE8F1E9))
        scene == WeatherScene.THUNDERSTORM -> listOf(Color(0xFF839FBD), Color(0xFFB4C5D5), Color(0xFFD4DCE4))
        scene == WeatherScene.SNOW -> listOf(Color(0xFF9DDBEA), Color(0xFFD0F0F5), Color(0xFFFFFFFF))
        else -> listOf(Color(0xFF71C6E5), Color(0xFFA6DCEB), Color(0xFFD3F0F2))
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors)))
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("正在查看天空…", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EmptyState(
    state: WeatherUiState,
    viewModel: WeatherViewModel,
    requestPermission: () -> Unit,
    openSettings: () -> Unit,
) {
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(colors = CardDefaults.cardColors(containerColor = glassColor()), shape = RoundedCornerShape(28.dp)) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.LocationOn, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                Text(if (state.locationUnavailable) "目前無法取得位置" else "選擇你的天氣地點", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text("可以重新定位，或直接搜尋城市。", textAlign = TextAlign.Center)
                Button(onClick = { if (state.needsPermission) requestPermission() else viewModel.locate() }) {
                    Icon(Icons.Default.MyLocation, null)
                    Text(" 重新定位")
                }
                OutlinedButton(onClick = viewModel::openPlaceSheet) {
                    Icon(Icons.Default.Search, null)
                    Text(" 搜尋城市")
                }
                if (state.locationUnavailable) {
                    TextButton(onClick = openSettings) {
                        Icon(Icons.Default.Settings, null)
                        Text(" 開啟系統設定")
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherContent(
    state: WeatherUiState,
    viewModel: WeatherViewModel,
    now: Instant,
    isDaylight: Boolean,
) {
    val weather = state.weather ?: return
    val currentCondition = weatherCondition(weather.current.weatherCode)
    val contentColor = MaterialTheme.colorScheme.onBackground
    val localToday = weather.localDateTime(now).toLocalDate()
    val futureForecasts = weather.futureForecasts(now)
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(weather.place.name, color = contentColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            if (weather.place.isCurrentLocation) "目前位置${if (state.locationIsStale) " · 較舊定位" else ""}" else weather.place.subtitle,
                            color = contentColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    Text(
                        "${formatLocalTime(weather, now)} · ${weather.timezoneLabel(now)}",
                        color = contentColor.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                IconButton(
                    onClick = viewModel::openPlaceSheet,
                    modifier = Modifier
                        .size(48.dp)
                        .offset(y = (-9).dp)
                        .semantics { contentDescription = "搜尋或切換城市" },
                ) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 360.dp
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        WeatherIcon(
                            weatherCode = weather.current.weatherCode,
                            isDay = isDaylight,
                            modifier = Modifier.size(if (compact) 84.dp else 104.dp),
                        )
                        Text(
                            formatTemperature(weather.current.temperature),
                            color = contentColor,
                            fontSize = if (compact) 72.sp else 82.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = if (compact) 4.dp else 6.dp),
                        )
                        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text("濕度", color = contentColor.copy(alpha = 0.65f), fontSize = 13.sp)
                            Text(weather.current.relativeHumidity?.let { "$it%" } ?: "--", color = contentColor, fontSize = if (compact) 27.sp else 30.sp, fontWeight = FontWeight.Bold)
                            Text("體感", color = contentColor.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            Text(formatTemperature(weather.current.apparentTemperature), color = contentColor, fontSize = if (compact) 20.sp else 23.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(currentCondition.description, color = contentColor, fontSize = if (compact) 31.sp else 35.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { CurrentDetails(weather, now) }
        items(futureForecasts) { forecast ->
            DailyCard(forecast, forecastDayLabel(forecast.date, localToday))
        }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val staleText = if (state.isCacheStale) " · 資料可能已過期" else ""
                Text("最後更新 ${formatUpdatedAt(weather)}$staleText", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("天氣資料：Open-Meteo.com", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
                Text("空氣品質資料：CAMS / Open-Meteo", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CurrentDetails(weather: WeatherBundle, now: Instant) {
    val localDate = weather.localDateTime(now).toLocalDate().toString()
    val today = weather.daily.firstOrNull { it.date == localDate } ?: weather.daily.firstOrNull()
    val aqi = weather.airQuality?.usAqi
    val pm25 = weather.airQuality?.pm25
    val metrics = listOf(
        WeatherMetric("降雨機率", weather.current.precipitationProbability?.let { "$it%" } ?: "--", Icons.Default.WaterDrop, Color(0xFF25BFD3)),
        WeatherMetric("降雨量", formatRain(weather.current.precipitation), Icons.Default.Grain, Color(0xFF5596EE)),
        WeatherMetric("風速", "${formatNumber(weather.current.windSpeed)} km/h", Icons.Default.Air, Color(0xFF54C2B1), secondary = listOfNotNull(windForceLabel(weather.current.windSpeed), windDirection(weather.current.windDirection).takeIf { it.isNotBlank() }).joinToString(" · ").takeIf { it.isNotBlank() }, secondaryColor = Color(0xFF54C2B1)),
        WeatherMetric("氣壓", weather.current.pressureMsl?.let { "${formatNumber(it)} hPa" } ?: "--", Icons.Default.Public, pressureColor(weather.current.pressureMsl), secondary = pressureDescription(weather.current.pressureMsl), secondaryColor = pressureColor(weather.current.pressureMsl)),
        WeatherMetric("能見度", formatVisibility(weather.current.visibilityMeters), Icons.Default.Visibility, visibilityColor(weather.current.visibilityMeters), secondary = visibilityDescription(weather.current.visibilityMeters), secondaryColor = visibilityColor(weather.current.visibilityMeters)),
        WeatherMetric("雲量", weather.current.cloudCover?.let { "$it%" } ?: "--", Icons.Default.Cloud, Color(0xFF8BA7C7), secondary = cloudCoverDescription(weather.current.cloudCover), secondaryColor = Color(0xFF8BA7C7)),
        WeatherMetric("海拔高度", weather.elevationMeters?.let { "${it.roundToInt()} m" } ?: "--", Icons.Default.Landscape, Color(0xFF4EC98A)),
        WeatherMetric("紫外線", formatNumber(weather.current.uvIndex), Icons.Default.WbSunny, uvColor(weather.current.uvIndex), secondary = uvDescription(weather.current.uvIndex), secondaryColor = uvColor(weather.current.uvIndex)),
        WeatherMetric("日出", formatClock(today?.sunrise), Icons.Default.WbSunny, Color(0xFFFFA726)),
        WeatherMetric("日落", formatClock(today?.sunset), Icons.Default.NightsStay, Color(0xFFB268E8)),
        WeatherMetric("日照時數", formatSunshineDuration(today?.sunshineDurationSeconds), Icons.Default.AccessTime, Color(0xFFF4D03F)),
        WeatherMetric("月相", moonPhaseLabel(today?.moonPhase), Icons.Default.NightsStay, Color(0xFF83BDF4), isMoonPhase = true),
        WeatherMetric("US AQI", aqi?.toString() ?: "--", Icons.Default.Speed, aqiColor(aqi), secondary = aqiDescription(aqi), secondaryColor = aqiColor(aqi)),
        WeatherMetric("PM2.5", pm25?.let { "${formatNumber(it)} μg/m³" } ?: "--", Icons.Default.BlurOn, pm25Color(pm25), secondary = pm25Description(pm25), secondaryColor = pm25Color(pm25)),
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = glassColor()),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowMetrics.forEach { metric ->
                    WeatherMetricValue(metric, Modifier.weight(1f))
                }
            }
        }
        }
    }
}

private data class WeatherMetric(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color,
    val secondary: String? = null,
    val secondaryColor: Color = accent,
    val isMoonPhase: Boolean = false,
)

@Composable
private fun WeatherMetricValue(metric: WeatherMetric, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(metric.icon, contentDescription = null, tint = metric.accent, modifier = Modifier.size(15.dp))
            Text(
                metric.label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            if (metric.isMoonPhase) {
                WeatherIcon(weatherCode = 0, isDay = false, modifier = Modifier.size(23.dp))
            }
            Text(metric.value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(start = if (metric.isMoonPhase) 4.dp else 0.dp))
        }
        metric.secondary?.let {
            Text(it, color = metric.secondaryColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun DailyCard(forecast: DailyForecast, title: String) {
    val condition = weatherCondition(forecast.weatherCode)
    Card(
        colors = CardDefaults.cardColors(containerColor = glassColor()),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherIcon(forecast.weatherCode, true, Modifier.size(38.dp))
                        Text(condition.description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    }
                }
                Text("${formatTemperature(forecast.maxTemperature)} / ${formatTemperature(forecast.minTemperature)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("體感 ${formatTemperature(forecast.maxApparentTemperature)} / ${formatTemperature(forecast.minApparentTemperature)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 13.sp)
            Text("降雨 ${forecast.precipitationProbability?.let { "$it%" } ?: "--"} · ${formatRain(forecast.precipitationSum)} · 最大風速 ${formatNumber(forecast.maxWindSpeed)} km/h", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 13.sp)
            Text("日出 ${formatClock(forecast.sunrise)} · 日落 ${formatClock(forecast.sunset)} · UV ${formatNumber(forecast.uvIndexMax)} · ${moonPhaseLabel(forecast.moonPhase)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceSheet(state: WeatherUiState, viewModel: WeatherViewModel) {
    ModalBottomSheet(onDismissRequest = viewModel::closePlaceSheet) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("城市與收藏", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::closePlaceSheet) { Icon(Icons.Default.Close, "關閉") }
            }
            TextField(
                value = state.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("輸入至少 2 個字元") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (state.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清除搜尋關鍵字")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(12.dp))
            if (state.searchQuery.isBlank()) {
                PlaceRow(
                    Place("current_location", "目前位置", latitude = 0.0, longitude = 0.0, isCurrentLocation = true),
                    false,
                    onClick = { viewModel.closePlaceSheet(); viewModel.locate() },
                    onFavorite = null,
                )
                if (state.favorites.isNotEmpty()) Text("收藏城市", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                state.favorites.forEach { place ->
                    PlaceRow(place, true, { viewModel.selectPlace(place) }, { viewModel.toggleFavorite(place) })
                }
            } else {
                if (state.isSearching) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.searchError?.let { Text(it, Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center) }
                state.searchResults.forEach { place ->
                    val favorite = state.favorites.any { it.id == place.id }
                    PlaceRow(place, favorite, { viewModel.selectPlace(place) }, { viewModel.toggleFavorite(place) })
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PlaceRow(place: Place, favorite: Boolean, onClick: () -> Unit, onFavorite: (() -> Unit)?) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (place.isCurrentLocation) Icons.Default.MyLocation else Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(place.name, fontWeight = FontWeight.SemiBold)
                if (place.subtitle.isNotBlank()) Text(place.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            if (onFavorite != null) {
                IconButton(onClick = onFavorite, modifier = Modifier.size(48.dp)) {
                    Icon(if (favorite) Icons.Filled.Star else Icons.Outlined.Star, if (favorite) "移除收藏" else "加入收藏", tint = if (favorite) Color(0xFFF4A825) else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private fun formatTemperature(value: Double?): String = value?.let { "${it.roundToInt()}°" } ?: "--"
private fun formatNumber(value: Double?): String = value?.let { if (it % 1.0 == 0.0) it.roundToInt().toString() else String.format(Locale.TAIWAN, "%.1f", it) } ?: "--"
private fun formatRain(value: Double?): String = "${formatNumber(value)} mm"
private fun formatClock(value: String?): String = value?.substringAfter("T")?.take(5) ?: "--"
private fun formatUpdatedAt(weather: WeatherBundle): String = runCatching {
    val zone = ZoneId.of(weather.timezone)
    Instant.ofEpochMilli(weather.fetchedAtEpochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("M/d HH:mm", Locale.TAIWAN))
}.getOrDefault("--")

private fun windDirection(degrees: Double?): String {
    if (degrees == null) return ""
    val directions = listOf("北", "東北", "東", "東南", "南", "西南", "西", "西北")
    return directions[((degrees / 45.0).roundToInt() % 8 + 8) % 8] + "風"
}

private fun aqiColor(value: Int?): Color = when {
    value == null -> Color(0xFFAAB5C7)
    value <= 50 -> Color(0xFF43A047)
    value <= 100 -> Color(0xFFFBC02D)
    value <= 150 -> Color(0xFFFB8C00)
    value <= 200 -> Color(0xFFE53935)
    value <= 300 -> Color(0xFF8E24AA)
    else -> Color(0xFF6D4C41)
}

private fun pm25Color(value: Double?): Color = when {
    value == null -> Color(0xFFAAB5C7)
    value <= 15.4 -> Color(0xFF43A047)
    value <= 35.4 -> Color(0xFFFBC02D)
    value <= 54.4 -> Color(0xFFFB8C00)
    value <= 150.4 -> Color(0xFFE53935)
    else -> Color(0xFF6D4C41)
}

private fun uvColor(value: Double?): Color = when {
    value == null -> Color(0xFFAAB5C7)
    value < 3.0 -> Color(0xFF43A047)
    value < 6.0 -> Color(0xFFFBC02D)
    value < 8.0 -> Color(0xFFFB8C00)
    value < 11.0 -> Color(0xFFE53935)
    else -> Color(0xFF8E24AA)
}

private fun pressureColor(value: Double?): Color = when {
    value == null -> Color(0xFFAAB5C7)
    value < 980.0 -> Color(0xFFE53935)
    value < 1011.0 -> Color(0xFFFB8C00)
    value < 1020.0 -> Color(0xFF6A9DE8)
    value < 1040.0 -> Color(0xFF43A047)
    else -> Color(0xFF00897B)
}

private fun visibilityColor(valueMeters: Double?): Color = when {
    valueMeters == null -> Color(0xFFAAB5C7)
    valueMeters >= 10_000.0 -> Color(0xFF43A047)
    valueMeters >= 5_000.0 -> Color(0xFFFBC02D)
    valueMeters >= 1_000.0 -> Color(0xFFFB8C00)
    else -> Color(0xFFE53935)
}

@Composable
private fun glassColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
