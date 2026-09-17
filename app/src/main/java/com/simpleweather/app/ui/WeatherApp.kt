package com.simpleweather.app.ui

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.simpleweather.app.model.isDaylightAt
import java.time.Instant
import kotlinx.coroutines.delay

private val LightColors = lightColorScheme(
    primary = Color(0xFF087FA3),
    onPrimary = Color.White,
    background = Color(0xFFEDF6FA),
    onBackground = Color(0xFF143247),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF143247),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF58CAE9),
    onPrimary = Color(0xFF003549),
    background = Color(0xFF0A1426),
    onBackground = Color(0xFFE7F3FA),
    surface = Color(0xFF182842),
    onSurface = Color(0xFFE7F3FA),
)

@Composable
fun WeatherApp(
    state: WeatherUiState,
    viewModel: WeatherViewModel,
    requestPermission: () -> Unit,
    openSettings: () -> Unit,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Instant.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    val isDaylight = state.weather?.isDaylightAt(now) ?: true
    val activity = LocalActivity.current as? ComponentActivity
    SideEffect {
        val transparent = Color.Transparent.toArgb()
        activity?.enableEdgeToEdge(
            statusBarStyle = if (isDaylight) {
                SystemBarStyle.light(transparent, transparent)
            } else {
                SystemBarStyle.dark(transparent)
            },
            navigationBarStyle = if (isDaylight) {
                SystemBarStyle.light(LightColors.background.toArgb(), LightColors.background.toArgb())
            } else {
                SystemBarStyle.dark(DarkColors.background.toArgb())
            },
        )
    }

    MaterialTheme(colorScheme = if (isDaylight) LightColors else DarkColors) {
        WeatherScreen(
            state = state,
            viewModel = viewModel,
            requestPermission = requestPermission,
            openSettings = openSettings,
            now = now,
            isDaylight = isDaylight,
        )
    }
}
