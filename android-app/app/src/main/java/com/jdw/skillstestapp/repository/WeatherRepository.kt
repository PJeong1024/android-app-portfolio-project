package com.jdw.skillstestapp.repository

import com.jdw.skillstestapp.data.model.weather.WeatherData
import com.jdw.skillstestapp.data.network.WeatherApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {
    suspend fun getWeather(lat: Double, lon: Double): WeatherData =
        weatherApi.getWeather(lat.toString(), lon.toString())
}
