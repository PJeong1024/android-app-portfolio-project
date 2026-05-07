package com.jdw.skillstestapp.screens.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.jdw.skillstestapp.data.model.weather.WeatherData
import com.jdw.skillstestapp.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherApiViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val weatherRepository: WeatherRepository,
) : ViewModel() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val _weatherData = MutableStateFlow(WeatherData())
    val weatherData = _weatherData.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    init {
        getWeather()
    }

    fun getWeather() {
        getDeviceLocation { lat, lon ->
            viewModelScope.launch {
                _loading.value = true
                _weatherData.value = weatherRepository.getWeather(lat, lon)
                _loading.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(onLocationReceived: (Double, Double) -> Unit) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                onLocationReceived(location.latitude, location.longitude)
            }
        }.addOnFailureListener {
            onLocationReceived(0.0, 0.0)
        }
    }
}
