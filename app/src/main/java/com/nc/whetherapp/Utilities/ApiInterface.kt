package com.nc.whetherapp.Utilities

import com.nc.whetherapp.models.WeatherModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {

    @GET("weather")
    fun getCurrentWeatherDate(
        @Query("lat") lat:String,
        @Query("lon") lon:String,
        @Query("apiKey") appid:String,
    ):Call<WeatherModel>

    @GET("weather")
    fun getCityWeatherDate(
        @Query("q") q:String,
        @Query("APPID") appid:String
    ):Call<WeatherModel>
}