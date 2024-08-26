package com.example.weatherapp.data.network.api

import com.example.weatherapp.data.model.RemoteLocation
import com.example.weatherapp.data.network.RemoteWeatherData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    companion object {
        const val BASE_URL = "https://api.weatherapi.com/v1/"
        const val API_KEY = "1e65bf62022f477ab4d52825242608"
    }

    @GET("search.json")
    suspend fun searchLocation(
        @Query("key") ket: String = API_KEY,
        @Query("q") query: String
    ): Response<List<RemoteLocation>>

    @GET("forecast.json")
    suspend fun getWeatherData(
        @Query("key") key: String = API_KEY,
        @Query("q") query: String
    ): Response<RemoteWeatherData>
}