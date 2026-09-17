package com.simpleweather.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionTest {
    @Test fun `maps representative WMO codes to scenes`() {
        assertEquals(WeatherScene.CLEAR, weatherCondition(0).scene)
        assertEquals(WeatherScene.CLOUDY, weatherCondition(3).scene)
        assertEquals(WeatherScene.FOG, weatherCondition(45).scene)
        assertEquals(WeatherScene.RAIN, weatherCondition(65).scene)
        assertEquals(WeatherScene.SNOW, weatherCondition(75).scene)
        assertEquals(WeatherScene.THUNDERSTORM, weatherCondition(99).scene)
    }

    @Test fun `unknown code has safe fallback`() {
        assertEquals("天氣資料不明", weatherCondition(999).description)
    }
}
