package com.piatt.androidserviceclient.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class WeatherManager {
    private val weatherClient = OkHttpClient()
        .newBuilder()
        .addInterceptor { chain ->
            val newUrl = chain.request().url()
                .newBuilder()
                .addQueryParameter("units", "imperial")
                .addQueryParameter("appid", BuildConfig.WEATHER_API_KEY)
                .build()

            val newRequest = chain.request()
                .newBuilder()
                .url(newUrl)
                .build()

            chain.proceed(newRequest)
        }
        .build()

    val weatherApi: WeatherApi = Retrofit.Builder()
        .client(weatherClient)
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(WeatherApi::class.java)
}

interface WeatherApi{
    @GET("weather")
    suspend fun getCurrentWeather(@Query("q") city: String): String

    @GET("forecast/daily")
    suspend fun getForecastWeather(@Query("q") city: String): String
}