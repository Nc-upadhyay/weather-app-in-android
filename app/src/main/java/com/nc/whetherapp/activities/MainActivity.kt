package com.nc.whetherapp.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.nc.whetherapp.R
import com.nc.whetherapp.Utilities.ApiUtilities
import com.nc.whetherapp.databinding.ActivityMainBinding
import com.nc.whetherapp.entity.CityWeatherEntity
import com.nc.whetherapp.models.CityWeather
import com.nc.whetherapp.models.WeatherModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build


class MainActivity : AppCompatActivity() {

    val tag = "main_Activity"
    lateinit var binding: ActivityMainBinding
    private lateinit var currentLo: Location
    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 101
    private val apikey = "7482bdd210e5cc9a88643db87725b81a"
    private var isDataStore: Boolean = false
    lateinit var sp: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        var view = binding.root
        setContentView(view)
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        sp = getSharedPreferences("cityData", MODE_PRIVATE)

        // checking internet Connection
        if (checkForInternet(this)) {
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
            try {
                if (sp.getBoolean("isDataSave", false)) {
                    getDataFromSharedPre()

                }
            } catch (e: java.lang.Exception) {
                Log.d(tag, "exception ${e.printStackTrace()}")
            }
            hideProgressBar()
        }
        binding.citySearch.setOnEditorActionListener { textView, i, keyEvent ->

            if (i == EditorInfo.IME_ACTION_SEARCH) {
                Log.d(tag, "============== city is ${binding.citySearch.text}")
                getCityWeather(binding.citySearch.text.toString())

                val view2 = this.currentFocus

                if (view2 != null) {
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

                    imm.hideSoftInputFromWindow(view2.windowToken, 0)
                    binding.citySearch.clearFocus()
                }
                return@setOnEditorActionListener true
            } else {
                return@setOnEditorActionListener false
            }
            // binding=DataBindingUtil.setContentView(this,R.layout.activity_main)


        }
        binding.currentLocation.setOnClickListener {
            getCurrentLocation()
        }


    }

    private fun getCurrentLocation() {
        if (checkPermission()) {
            if (isLocationEnabled()) {
                //final location
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }

                fusedLocationProvider.lastLocation.addOnSuccessListener { it ->
                    if (it != null) {
                        Toast.makeText(this, "------ $it", Toast.LENGTH_LONG).show()
                        Log.d(tag, "========== current location is $it")
                        currentLo = it
                        showProgressBar()
                        fetchCurretnLocationWeather(it.latitude.toString(), it.longitude.toString())
                    } else {
                        Toast.makeText(this, "Location Not Fetch $it", Toast.LENGTH_LONG).show()
                        getCityWeather("india")
                    }
                }

            } else {
                // if location not enable and opne location setting

                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            //if permission not enabled
            requestPermission()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ), LOCATION_REQUEST_CODE
        )
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Granted", Toast.LENGTH_LONG).show()
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getCityWeather(city: String) {
        showProgressBar()
        ApiUtilities.getApi()?.getCityWeatherDate(city.trim(), apikey)?.enqueue(object :
            Callback<WeatherModel> {
            override fun onResponse(call: Call<WeatherModel>, response: Response<WeatherModel>) {
                if (response.isSuccessful) {
                    Log.d(tag, " data is ${response.body()}")
                    binding.progrssBar.visibility = View.GONE
                    if (response.body() != null) {
                        setData(response.body());
                        sp.edit().clear().commit()

                        val currentDate = SimpleDateFormat("dd/MM/yyyy hh:mm").format(Date())
                        setDataIntoSharePre(
                            CityWeatherEntity(
                                currentDate,
                                k2c(response.body()!!.main.temp_min).toString(),
                                k2c(response.body()!!.main.temp_max).toString(),
                                k2c(response.body()!!.main.temp).toString(),
                                response.body()!!.weather[0].id,
                                k2c(response.body()!!.main.feels_like).toString(),
                                response.body()!!.weather[0].main,
                                city
                            )
                        )
                        isDataStore = true;

                    }
                } else {
                    Log.d(tag, " ${response.body()}")
                    Toast.makeText(this@MainActivity, "City Not Found", Toast.LENGTH_LONG)
                        .show()
                }
                hideProgressBar()
            }

            override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                Toast.makeText(this@MainActivity, "", Toast.LENGTH_LONG).show()
                Log.d(tag, "============ Exception ${t.printStackTrace()}");
                hideProgressBar()
            }
        })

    }

    private fun setDataIntoSharePre(city: CityWeatherEntity) {

        val myEdit = sp.edit()
        myEdit.putString("temp", city.temp).apply()
        myEdit.putString("date", city.date).commit()
        myEdit.putString("minTemp", city.minTemp).commit()
        myEdit.putString("maxTemp", city.maxTemp).commit()
        myEdit.putInt("image", city.image).commit()
        myEdit.putString("title", city.weatherTitle).commit()
        myEdit.putString("feels", city.feelLike).commit()
        myEdit.putString("cityName", city.city).commit()
        myEdit.putBoolean("isDataSave", true).commit()

        getDataFromSharedPre()

    }

    private fun getDataFromSharedPre() {
        binding.apply {
            dateTime.text = sp.getString("date", "")
            maxTemp.text = "Max " + sp.getString("maxTemp", "") + "°"
            minTemp.text = "Min " + sp.getString("minTemp", "") + "°"
            temp.text = "" + sp.getString("temp", "") + " °C"
            citySearch.setText(sp.getString("cityName", ""))
            feelsLike.text = "${sp.getString("feels", "")}°"
            weatherTitle.text = sp.getString("title", "")
            citySearch.setText(sp.getString("cityName", ""))
            updateUi(sp.getInt("image", 500))
        }
    }

    private fun setData(response: WeatherModel?) {
        binding.apply {
            val currentDate = SimpleDateFormat("dd/MM/yyyy hh:mm").format(Date())
            dateTime.text = currentDate.toString()
            maxTemp.text = "Max " + k2c(response?.main?.temp!!) + "°"
            minTemp.text = "Min " + k2c(response?.main?.temp_min!!) + "°"
            temp.text = "" + k2c(response?.main?.temp!!) + " °C"
            citySearch.setText(response.name)
            feelsLike.text = "${k2c(response?.main?.feels_like!!)}°"
            weatherTitle.text = response.weather[0].main

        }
        updateUi(response!!.weather[0].id)
        setCityData()

    }

    private fun setCityData() {
        showProgressBar()
        var j: Int = 0;
        val city =
            arrayListOf<String>("New York", "Singapore", "Mumbai", "Delhi", "Sydney", "Melbourne")
        for (i in city) {
            ApiUtilities.getApi()?.getCityWeatherDate(i.trim(), apikey)?.enqueue(object :
                Callback<WeatherModel> {
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {
                        val data =
                            CityWeather(
                                i,
                                response!!.body()!!.weather[0].id,
                                k2c(response!!.body()!!.main.temp).toString()
                            )
                        if (j == 0) {
                            setData1(data, binding.singImg, binding.singTitle, binding.singTemp)
                        } else if (j == 1) {
                            setData1(data, binding.mumImg, binding.mumTitle, binding.mumTemp)
                        } else if (j == 2) {
                            setData1(data, binding.delImg, binding.delTitle, binding.delTemp)
                        } else if (j == 3) {
                            setData1(data, binding.nwkImg, binding.nwkCity, binding.nwkTemp)
                        } else if (j == 4) {
                            setData1(data, binding.melImg, binding.melTitle, binding.melTemp)
                        } else {
                            setData1(data, binding.syImg, binding.syTitle, binding.syTemp)
                        }
                        j++
                    }
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "", Toast.LENGTH_LONG).show()
                    Log.d(tag, "============ Exception ${t.printStackTrace()}");
                    hideProgressBar()
                }
            })
        }
        hideProgressBar()

    }

    private fun setData1(
        data: CityWeather,
        singImg: ImageView,
        singTitle: TextView,
        singTemp: TextView
    ) {
        setImage(data.img, singImg)
        singTitle.text = data.title
        singTemp.text = data.temp
    }

    private fun setImage(id: Int, img: ImageView) {
        binding.apply {
            binding.apply {
                when (id) {
                    in 200..232 -> {
                        img.setImageResource(R.drawable.ic_storm_weather)
                    }
                    in 300..321 -> {
                        img.setImageResource(R.drawable.ic_few_clouds)
                    }
                    in 500..532 -> {
                        img.setImageResource(R.drawable.ic_rainy_weather)
                    }
                    in 600..632 -> {
                        img.setImageResource(R.drawable.ic_snow_weather)
                    }
                    in 700..782 -> {
                        img.setImageResource(R.drawable.ic_broken_clouds)
                    }
                    800 -> {
                        img.setImageResource(R.drawable.ic_clear_day)
                    }
                    in 800..804 -> {
                        img.setImageResource(R.drawable.ic_cloudy_weather)
                    }
                    else -> {
                        img.setImageResource(R.drawable.ic_unknown)
                    }
                }
            }
        }
    }

    private fun updateUi(id: Int) {
        binding.apply {
            when (id) {
                in 200..232 -> {
                    weatherImg.setImageResource(R.drawable.ic_storm_weather)
                    mainActivity.background =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.thunderstrom_bg)


                }
                in 300..321 -> {
                    weatherImg.setImageResource(R.drawable.ic_few_clouds)
                    mainActivity.background =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.drizzle_bg)


                }
                in 500..532 -> {
                    weatherImg.setImageResource(R.drawable.ic_rainy_weather)
                    mainActivity.background =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.rain_bg)
                }
                in 600..632 -> {
                    weatherImg.setImageResource(R.drawable.ic_snow_weather)
                    mainActivity.background =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.snow_bg)
                }
                in 700..782 -> {
                    weatherImg.setImageResource(R.drawable.ic_broken_clouds)
                    mainActivity.background =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.atmosphere_bg)
                }
                800 -> {
                    weatherImg.setImageResource(R.drawable.ic_clear_day)
                    mainActivity.background =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.clear_bg)
                }
                in 800..804 -> {
                    weatherImg.setImageResource(R.drawable.ic_cloudy_weather)
                    mainActivity.background =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.clouds_bg)
                }
                else -> {
                    weatherImg.setImageResource(R.drawable.ic_unknown)
                    mainActivity.background =
                        ContextCompat.getDrawable(this@MainActivity, R.drawable.unknown_bg)
                }
            }
        }
    }


    private fun k2c(tempMax: Double): Double {
        var intTemp = tempMax
        intTemp = intTemp.minus(270)
        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    fun showProgressBar() {
        binding.progrssBar.visibility = View.VISIBLE
    }

    fun hideProgressBar() {
        binding.progrssBar.visibility = View.INVISIBLE
    }

    private fun fetchCurretnLocationWeather(latitude: String, longitude: String) {
        ApiUtilities.getApi()?.getCurrentWeatherDate(latitude, longitude, apikey)
            ?.enqueue(object : Callback<WeatherModel> {
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {
                        setData(response.body())
                    } else {
                        Toast.makeText(this@MainActivity, "Location Not Found.", Toast.LENGTH_LONG)
                            .show()
                        if (isDataStore) {
                            getDataFromSharedPre()
                        }
                    }
                    hideProgressBar()
                }

                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    Log.d(
                        tag, "==========================enable to feathc data ${
                            t.printStackTrace()
                        }"
                    )
                    hideProgressBar()
                }
            })
    }

    private fun checkForInternet(context: Context): Boolean {

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false

            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}
