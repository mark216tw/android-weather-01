package com.simpleweather.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simpleweather.app.data.FavoriteResult
import com.simpleweather.app.data.LocationProvider
import com.simpleweather.app.data.LocationResult
import com.simpleweather.app.data.WeatherRepository
import com.simpleweather.app.model.Place
import com.simpleweather.app.model.WeatherBundle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeatherUiState(
    val weather: WeatherBundle? = null,
    val favorites: List<Place> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLocating: Boolean = false,
    val needsPermission: Boolean = false,
    val showLocationRationale: Boolean = false,
    val locationUnavailable: Boolean = false,
    val locationIsStale: Boolean = false,
    val message: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Place> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val showPlaceSheet: Boolean = false,
) {
    val isCacheStale: Boolean
        get() = weather?.let { System.currentTimeMillis() - it.fetchedAtEpochMillis > 2 * 60 * 60 * 1000L } == true
}

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private companion object {
        const val AUTOMATIC_REFRESH_AGE_MILLIS = 15 * 60 * 1_000L
    }

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()
    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        viewModelScope.launch { runCatching { repository.cleanOldCache() } }
        viewModelScope.launch {
            repository.favorites.collect { favorites -> _state.update { it.copy(favorites = favorites) } }
        }
        viewModelScope.launch {
            val selected = repository.selectedPlace()
            if (selected == null) {
                val seen = repository.hasSeenRationale()
                _state.update {
                    it.copy(isLoading = false, needsPermission = true, showLocationRationale = !seen)
                }
            } else if (selected.isCurrentLocation) {
                locate()
            } else {
                load(selected)
            }
        }
    }

    fun acceptLocationRationale() {
        viewModelScope.launch { repository.markRationaleSeen() }
        _state.update { it.copy(showLocationRationale = false, needsPermission = true) }
    }

    fun dismissLocationRationale() {
        viewModelScope.launch { repository.markRationaleSeen() }
        _state.update { it.copy(showLocationRationale = false, needsPermission = false, isLoading = false) }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) locate()
        else _state.update {
            it.copy(needsPermission = false, isLoading = false, locationUnavailable = true, message = "未授予定位權限，仍可搜尋城市")
        }
    }

    fun locate() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLocating = true, locationUnavailable = false, needsPermission = false) }
            when (val result = locationProvider.resolveCurrentPlace()) {
                LocationResult.PermissionRequired -> _state.update { it.copy(isLocating = false, isLoading = false, needsPermission = true) }
                LocationResult.ProviderDisabled -> _state.update {
                    it.copy(isLocating = false, isLoading = false, locationUnavailable = true, message = "定位服務未開啟")
                }
                LocationResult.Unavailable -> _state.update {
                    it.copy(isLocating = false, isLoading = false, locationUnavailable = true, message = "無法取得目前位置")
                }
                is LocationResult.Success -> {
                    _state.update { it.copy(isLocating = false, locationIsStale = result.isStale) }
                    repository.select(result.place)
                    load(result.place, staleLocation = result.isStale)
                }
            }
        }
    }

    fun refresh() {
        val place = _state.value.weather?.place
        if (place == null || place.isCurrentLocation) locate() else load(place, refreshing = true)
    }

    fun refreshIfStale() {
        val current = _state.value
        val weather = current.weather ?: return
        if (current.isLoading || current.isRefreshing || current.isLocating) return
        val age = (System.currentTimeMillis() - weather.fetchedAtEpochMillis).coerceAtLeast(0L)
        if (age >= AUTOMATIC_REFRESH_AGE_MILLIS) {
            load(
                place = weather.place,
                refreshing = true,
                staleLocation = current.locationIsStale,
            )
        }
    }

    fun selectPlace(place: Place) {
        _state.update { it.copy(showPlaceSheet = false, searchQuery = "", searchResults = emptyList()) }
        viewModelScope.launch { repository.select(place) }
        load(place)
    }

    private fun load(place: Place, refreshing: Boolean = false, staleLocation: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val cached = repository.cached(place)
            _state.update {
                it.copy(
                    weather = cached ?: it.weather?.takeIf { old -> old.place.cacheKey == place.cacheKey },
                    isLoading = cached == null,
                    isRefreshing = refreshing || cached != null,
                    locationUnavailable = false,
                    locationIsStale = staleLocation,
                    message = if (staleLocation) "使用 24 小時內的最近位置" else null,
                )
            }
            runCatching { repository.refresh(place) }
                .onSuccess { bundle ->
                    repository.select(bundle.place)
                    _state.update { it.copy(weather = bundle, isLoading = false, isRefreshing = false) }
                }
                .onFailure {
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            message = if (current.weather != null) "更新失敗，正在顯示上次資料" else "無法載入天氣，請檢查網路後重試",
                        )
                    }
                }
        }
    }

    fun openPlaceSheet() = _state.update { it.copy(showPlaceSheet = true) }
    fun closePlaceSheet() = _state.update { it.copy(showPlaceSheet = false) }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query, searchError = null) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _state.update { it.copy(isSearching = true) }
            runCatching { repository.search(query) }
                .onSuccess { results ->
                    _state.update { it.copy(searchResults = results, isSearching = false, searchError = if (results.isEmpty()) "找不到符合的城市" else null) }
                }
                .onFailure { _state.update { it.copy(isSearching = false, searchError = "城市搜尋失敗，請稍後再試") } }
        }
    }

    fun toggleFavorite(place: Place) {
        viewModelScope.launch {
            val existing = _state.value.favorites.any { it.id == place.id }
            if (existing) {
                repository.removeFavorite(place)
                _state.update { it.copy(message = "已移除收藏") }
            } else {
                val message = when (repository.addFavorite(place)) {
                    FavoriteResult.Added -> "已加入收藏"
                    FavoriteResult.AlreadyExists -> "此城市已在收藏中"
                    FavoriteResult.LimitReached -> "收藏已達 20 個上限"
                    FavoriteResult.NotAllowed -> "目前位置無法直接收藏"
                }
                _state.update { it.copy(message = message) }
            }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    class Factory(
        private val repository: WeatherRepository,
        private val locationProvider: LocationProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WeatherViewModel(repository, locationProvider) as T
    }
}
