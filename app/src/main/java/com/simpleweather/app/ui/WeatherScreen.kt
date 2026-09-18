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
import com.simpleweather.app.model.aqiLabel
import com.simpleweather.app.model.formatLocalTime
import com.simpleweather.app.model.formatSunshineDuration
import com.simpleweather.app.model.forecastDayLabel
import com.simpleweather.app.model.futureForecasts
import com.simpleweather.app.model.localDateTime
import com.simpleweather.app.model.moonPhaseLabel
import com.simpleweather.app.model.timezoneLabel
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
        !isDay && scene == WeatherScene.CLEAR -> listOf(Color(0xFF101B38), Color(0xFF172752), Color(0xFF26365C))
        !isDay && (scene == WeatherScene.CLOUDY || scene == WeatherScene.FOG) -> listOf(Color(0xFF162339), Color(0xFF22334C), Color(0xFF34445E))
        !isDay && scene == WeatherScene.THUNDERSTORM -> listOf(Color(0xFF17152F), Color(0xFF27234A), Color(0xFF3B3158))
        !isDay && scene == WeatherScene.SNOW -> listOf(Color(0xFF1C2D48), Color(0xFF304A68), Color(0xFF526D86))
        !isDay -> listOf(Color(0xFF12233D), Color(0xFF203A59), Color(0xFF355671))
        scene == WeatherScene.CLEAR -> listOf(Color(0xFFE8F5FA), Color(0xFFF3F9FC), Color(0xFFFFFAEC))
        scene == WeatherScene.CLOUDY || scene == WeatherScene.FOG -> listOf(Color(0xFFE4EEF3), Color(0xFFF0F6F8), Color(0xFFF8FBFC))
        scene == WeatherScene.THUNDERSTORM -> listOf(Color(0xFFDDE5ED), Color(0xFFEBF0F5), Color(0xFFF6F8FA))
        scene == WeatherScene.SNOW -> listOf(Color(0xFFE8F3F7), Color(0xFFF5FAFC), Color.White)
        else -> listOf(Color(0xFFDCECF4), Color(0xFFEEF6F9), Color(0xFFF8FBFC))
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f), CircleShape)
                        .semantics { contentDescription = "搜尋或切換城市" },
                ) { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) }
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
                            modifier = Modifier.size(if (compact) 72.dp else 88.dp),
                        )
                        Text(
                            formatTemperature(weather.current.temperature),
                            color = contentColor,
                            fontSize = if (compact) 64.sp else 72.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = if (compact) 4.dp else 6.dp),
                        )
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("濕度", color = contentColor.copy(alpha = 0.65f), fontSize = 14.sp)
                            Text(
                                weather.current.relativeHumidity?.let { "$it%" } ?: "--",
                                color = contentColor,
                                fontSize = if (compact) 30.sp else 34.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(currentCondition.description, color = contentColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "體感 ${formatTemperature(weather.current.apparentTemperature)}",
                        color = contentColor.copy(alpha = 0.68f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
        }
        item { CurrentDetails(weather, now) }
        item { Text("三日預報", color = contentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) }
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
    val metrics = listOf(
        WeatherMetric("降雨機率", weather.current.precipitationProbability?.let { "$it%" } ?: "--", Icons.Default.WaterDrop, Color(0xFF25BFD3)),
        WeatherMetric("降雨", formatRain(weather.current.precipitation), Icons.Default.Grain, Color(0xFF5596EE)),
        WeatherMetric("風速", "${formatNumber(weather.current.windSpeed)} km/h ${windDirection(weather.current.windDirection)}", Icons.Default.Air, Color(0xFF54C2B1)),
        WeatherMetric("海拔高度", weather.elevationMeters?.let { "${it.roundToInt()} m" } ?: "--", Icons.Default.Landscape, Color(0xFF4EC98A)),
        WeatherMetric("日出", formatClock(today?.sunrise), Icons.Default.WbSunny, Color(0xFFFFA726)),
        WeatherMetric("日落", formatClock(today?.sunset), Icons.Default.NightsStay, Color(0xFFB268E8)),
        WeatherMetric("日照時數", formatSunshineDuration(today?.sunshineDurationSeconds), Icons.Default.AccessTime, Color(0xFFF4D03F)),
        WeatherMetric("月相", moonPhaseLabel(today?.moonPhase), Icons.Default.NightsStay, Color(0xFF83BDF4)),
        WeatherMetric("US AQI", aqiLabel(aqi), Icons.Default.Speed, aqiColor(aqi)),
        WeatherMetric("PM2.5", weather.airQuality?.pm25?.let { "${formatNumber(it)} μg/m³" } ?: "--", Icons.Default.BlurOn, Color(0xFFAAB5C7)),
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
        Text(metric.value, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
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
            Text("日出 ${formatClock(forecast.sunrise)} · 日落 ${formatClock(forecast.sunset)} · ${moonPhaseLabel(forecast.moonPhase)}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 13.sp)
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
    value <= 50 -> Color(0xFF55C98B)
    value <= 100 -> Color(0xFFE2BE36)
    value <= 150 -> Color(0xFFF39A3D)
    value <= 200 -> Color(0xFFEF6B69)
    value <= 300 -> Color(0xFFA974D6)
    else -> Color(0xFF9D6B72)
}

@Composable
private fun glassColor(): Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
