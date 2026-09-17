package com.simpleweather.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.simpleweather.app.model.weatherCondition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class WeatherIconKind { SUN, MOON, PARTLY_DAY, PARTLY_NIGHT, CLOUD, FOG, RAIN, SNOW, THUNDERSTORM }

fun weatherIconKind(code: Int?, isDay: Boolean): WeatherIconKind = when (code) {
    0 -> if (isDay) WeatherIconKind.SUN else WeatherIconKind.MOON
    1, 2 -> if (isDay) WeatherIconKind.PARTLY_DAY else WeatherIconKind.PARTLY_NIGHT
    3 -> WeatherIconKind.CLOUD
    45, 48 -> WeatherIconKind.FOG
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherIconKind.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherIconKind.SNOW
    95, 96, 99 -> WeatherIconKind.THUNDERSTORM
    else -> WeatherIconKind.CLOUD
}

@Composable
fun WeatherIcon(
    weatherCode: Int?,
    isDay: Boolean,
    modifier: Modifier = Modifier,
) {
    val description = weatherCondition(weatherCode).description
    val cloud = if (isDay) Color(0xFF6F9DB1) else Color(0xFFBED7E5)
    val celestial = if (isDay) Color(0xFFFFBD16) else Color(0xFFFFE59A)
    val rain = if (isDay) Color(0xFF2C91C2) else Color(0xFF72C9F0)
    val snow = if (isDay) Color(0xFF5CA7C5) else Color(0xFFD8F2FF)
    val fog = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)

    Canvas(modifier.semantics { contentDescription = description }) {
        when (weatherIconKind(weatherCode, isDay)) {
            WeatherIconKind.SUN -> drawSketchSun(celestial)
            WeatherIconKind.MOON -> drawSketchMoon(celestial)
            WeatherIconKind.PARTLY_DAY -> drawPartlyCloudy(true, celestial, cloud)
            WeatherIconKind.PARTLY_NIGHT -> drawPartlyCloudy(false, celestial, cloud)
            WeatherIconKind.CLOUD -> drawSketchCloud(cloud)
            WeatherIconKind.FOG -> drawFog(cloud, fog)
            WeatherIconKind.RAIN -> drawRain(cloud, rain)
            WeatherIconKind.SNOW -> drawSnow(cloud, snow)
            WeatherIconKind.THUNDERSTORM -> drawThunderstorm(cloud, celestial, rain)
        }
    }
}

private fun DrawScope.drawSketchSun(color: Color, scale: Float = 1f, center: Offset = this.center) {
    val radius = size.minDimension * 0.25f * scale
    val stroke = size.minDimension * 0.055f * scale
    drawSketchArc(color, 8f, 344f, center.arcRect(radius), stroke)
    drawSketchArc(color.copy(alpha = 0.82f), 28f, 320f, Offset(center.x + stroke * 0.2f, center.y - stroke * 0.1f).arcRect(radius * 0.82f), stroke * 0.45f)
    drawSketchArc(color.copy(alpha = 0.72f), 190f, 300f, Offset(center.x - stroke * 0.15f, center.y + stroke * 0.12f).arcRect(radius * 1.12f), stroke * 0.3f)
    repeat(12) { index ->
        val angle = index * 2.0 * PI / 12.0 + if (index % 2 == 0) 0.03 else -0.03
        val inner = radius * (1.48f + (index % 3) * 0.04f)
        val outer = radius * (1.82f + (index % 2) * 0.12f)
        drawLine(
            color = color,
            start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
            end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
            strokeWidth = stroke * (0.55f + (index % 3) * 0.08f),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSketchMoon(color: Color, scale: Float = 1f, center: Offset = this.center) {
    val radius = size.minDimension * 0.34f * scale
    val crescent = Path().apply {
        moveTo(center.x - radius * 0.48f, center.y - radius * 0.92f)
        cubicTo(
            center.x + radius * 0.56f, center.y - radius * 0.78f,
            center.x + radius * 1.06f, center.y - radius * 0.22f,
            center.x + radius * 0.94f, center.y + radius * 0.50f,
        )
        cubicTo(
            center.x + radius * 0.84f, center.y + radius * 0.98f,
            center.x + radius * 0.20f, center.y + radius * 1.14f,
            center.x - radius * 0.92f, center.y + radius * 0.66f,
        )
        cubicTo(
            center.x - radius * 0.18f, center.y + radius * 0.60f,
            center.x + radius * 0.14f, center.y + radius * 0.18f,
            center.x + radius * 0.08f, center.y - radius * 0.26f,
        )
        cubicTo(
            center.x + radius * 0.04f, center.y - radius * 0.58f,
            center.x - radius * 0.16f, center.y - radius * 0.82f,
            center.x - radius * 0.48f, center.y - radius * 0.92f,
        )
        close()
    }
    drawPath(
        path = crescent,
        color = color.copy(alpha = 0.14f),
        style = Stroke(size.minDimension * 0.12f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(crescent, color)
}

private fun DrawScope.drawPartlyCloudy(isDay: Boolean, celestial: Color, cloud: Color) {
    val skyCenter = Offset(size.width * 0.38f, size.height * 0.38f)
    if (isDay) drawSketchSun(celestial, 0.62f, skyCenter) else drawSketchMoon(celestial, 0.65f, skyCenter)
    drawSketchCloud(cloud, Offset(0f, size.height * 0.12f))
}

private fun DrawScope.drawSketchCloud(color: Color, offset: Offset = Offset.Zero) {
    val path = Path().apply {
        moveTo(size.width * 0.16f + offset.x, size.height * 0.66f + offset.y)
        cubicTo(size.width * 0.08f, size.height * 0.54f, size.width * 0.15f, size.height * 0.42f, size.width * 0.31f, size.height * 0.43f)
        cubicTo(size.width * 0.36f, size.height * 0.22f, size.width * 0.66f, size.height * 0.20f, size.width * 0.72f, size.height * 0.45f)
        cubicTo(size.width * 0.90f, size.height * 0.43f, size.width * 0.96f, size.height * 0.66f, size.width * 0.81f, size.height * 0.72f)
        cubicTo(size.width * 0.62f, size.height * 0.76f, size.width * 0.34f, size.height * 0.73f, size.width * 0.16f, size.height * 0.66f)
    }
    val stroke = size.minDimension * 0.07f
    drawPath(path, color.copy(alpha = 0.16f))
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, color.copy(alpha = 0.55f), style = Stroke(stroke * 0.28f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawRain(cloud: Color, rain: Color) {
    drawSketchCloud(cloud, Offset(0f, -size.height * 0.12f))
    repeat(3) { index ->
        val x = size.width * (0.3f + index * 0.2f)
        val y = size.height * (0.67f + (index % 2) * 0.04f)
        drawLine(rain, Offset(x, y), Offset(x - size.width * 0.04f, y + size.height * 0.16f), size.minDimension * 0.055f, StrokeCap.Round)
    }
}

private fun DrawScope.drawSnow(cloud: Color, snow: Color) {
    drawSketchCloud(cloud, Offset(0f, -size.height * 0.14f))
    repeat(3) { index ->
        val center = Offset(size.width * (0.3f + index * 0.2f), size.height * (0.76f + (index % 2) * 0.03f))
        repeat(3) { arm ->
            val angle = arm * PI / 3
            val delta = Offset(cos(angle).toFloat() * size.width * 0.055f, sin(angle).toFloat() * size.height * 0.055f)
            drawLine(snow, center - delta, center + delta, size.minDimension * 0.025f, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawFog(cloud: Color, fog: Color) {
    drawSketchCloud(cloud, Offset(0f, -size.height * 0.17f))
    repeat(3) { index ->
        val y = size.height * (0.67f + index * 0.1f)
        val inset = if (index == 1) 0.24f else 0.17f
        drawLine(fog, Offset(size.width * inset, y), Offset(size.width * (1f - inset), y), size.minDimension * 0.045f, StrokeCap.Round)
    }
}

private fun DrawScope.drawThunderstorm(cloud: Color, lightning: Color, rain: Color) {
    drawSketchCloud(cloud, Offset(0f, -size.height * 0.14f))
    val bolt = Path().apply {
        moveTo(size.width * 0.54f, size.height * 0.62f)
        lineTo(size.width * 0.42f, size.height * 0.79f)
        lineTo(size.width * 0.53f, size.height * 0.78f)
        lineTo(size.width * 0.45f, size.height * 0.94f)
        lineTo(size.width * 0.68f, size.height * 0.71f)
        lineTo(size.width * 0.56f, size.height * 0.72f)
        close()
    }
    drawPath(bolt, lightning)
    drawLine(rain, Offset(size.width * 0.28f, size.height * 0.69f), Offset(size.width * 0.23f, size.height * 0.84f), size.minDimension * 0.04f, StrokeCap.Round)
    drawLine(rain, Offset(size.width * 0.77f, size.height * 0.67f), Offset(size.width * 0.72f, size.height * 0.82f), size.minDimension * 0.04f, StrokeCap.Round)
}

private fun Offset.arcRect(radius: Float): Rect = Rect(this - Offset(radius, radius), Size(radius * 2, radius * 2))

private fun DrawScope.drawSketchArc(color: Color, start: Float, sweep: Float, rect: Rect, strokeWidth: Float) {
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = rect.topLeft,
        size = rect.size,
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
}
