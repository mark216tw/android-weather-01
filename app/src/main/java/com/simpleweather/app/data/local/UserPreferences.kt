package com.simpleweather.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.simpleweather.app.model.Place
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("weather_preferences")

class UserPreferences(
    private val context: Context,
    private val json: Json,
) {
    suspend fun hasSeenLocationRationale(): Boolean =
        context.dataStore.data.first()[RATIONALE_SEEN] ?: false

    suspend fun markLocationRationaleSeen() {
        context.dataStore.edit { it[RATIONALE_SEEN] = true }
    }

    suspend fun selectedPlace(): Place? = context.dataStore.data.first()[SELECTED_PLACE]
        ?.let { runCatching { json.decodeFromString<Place>(it) }.getOrNull() }

    suspend fun setSelectedPlace(place: Place) {
        context.dataStore.edit { it[SELECTED_PLACE] = json.encodeToString(place) }
    }

    private companion object {
        val RATIONALE_SEEN = booleanPreferencesKey("location_rationale_seen")
        val SELECTED_PLACE = stringPreferencesKey("selected_place")
    }
}
