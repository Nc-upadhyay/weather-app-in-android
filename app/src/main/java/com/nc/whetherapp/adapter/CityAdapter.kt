package com.nc.whetherapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.nc.whetherapp.R
import com.nc.whetherapp.models.CityWeather

class CityAdapter(val cityList: ArrayList<CityWeather>): RecyclerView.Adapter<CityAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      var view=LayoutInflater.from(parent.context).inflate(R.layout.more_city_show_design,parent,false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cityWeather=cityList[position]
        holder.city.text=cityWeather.title
        holder.temp.text=cityWeather.temp
        updateUi(cityWeather.img,holder)
    }

    override fun getItemCount(): Int {
        return cityList.size
    }

    class  ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val city:TextView=view.findViewById(R.id.city_title)
        val temp:TextView=view.findViewById(R.id.city_temp)
        val image: ImageView =view.findViewById(R.id.city_weather_img)
    }
    private fun updateUi(id: Int, holder: ViewHolder) {
        holder.apply {
            when (id) {
                in 200..232 -> {
                    image.setImageResource(R.drawable.ic_storm_weather)


                }
                in 300..321 -> {
                    image.setImageResource(R.drawable.ic_few_clouds)



                }
                in 500..532 -> {
                    image.setImageResource(R.drawable.ic_rainy_weather)
                }
                in 600..632 -> {
                    image.setImageResource(R.drawable.ic_snow_weather)
                }
                in 700..782 -> {
                    image.setImageResource(R.drawable.ic_broken_clouds)
                }
                800 -> {
                    image.setImageResource(R.drawable.ic_clear_day)
                }
                in 800..804 -> {
                    image.setImageResource(R.drawable.ic_cloudy_weather)
                }
                else -> {
                    image.setImageResource(R.drawable.ic_unknown)
                }
            }
        }
    }

}

