package com.nc.whetherapp.entity

data class CityWeatherEntity(
   val date:String,
    val minTemp:String,
   val maxTemp:String,
     val temp:String,
     val image:Int,
     val feelLike:String,
    val weatherTitle:String,
    val city:String
)
