package com.jdw.skillstestapp.data.network

import com.jdw.skillstestapp.BuildConfig
import com.jdw.skillstestapp.data.model.weather.WeatherData
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Singleton

@Singleton
interface WeatherApi {
    @GET(value = "data/2.5/weather")
    suspend fun getWeather(
        @Query(value = "lat") lat: String,
        @Query(value = "lon") lon: String,
        @Query(value = "appid") appid: String = BuildConfig.OPEN_WEATHER_API_KEY,
        @Query(value = "units") units: String = "metric",
    ): WeatherData
}