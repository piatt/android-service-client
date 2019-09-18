package com.piatt.androidserviceclient.common

import android.content.Context
import android.os.IBinder
import android.util.Log
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Singleton

@Module
class TestWeatherClientModule {
    @Provides
    @Singleton
    fun provideWeatherClient(context: Context) = WeatherClient(
        context = context,
        weatherServiceClient = object : TestServiceClient<IWeatherService>(
            serviceName = IWeatherService::class.java.simpleName
        ) {
            override fun getServiceInterface(service: IBinder?) = object : IWeatherService.Stub() {
                private lateinit var callback: IWeatherServiceCallback

                override fun register(callback: IWeatherServiceCallback) {
                    Log.d(serviceName, "Registered callback with WeatherService")
                    callback.onWeatherUpdate(Date().time)
                    this.callback = callback
                }

                override fun unregister(callback: IWeatherServiceCallback) {
                    Log.d(serviceName, "Unregistered callback with WeatherService")
                }

                override fun updateWeather() {
                    Log.d(serviceName, "Triggering weather data refresh...")
                    GlobalScope.launch {
                        delay(3000L)
                        callback.onWeatherUpdate(Date().time)
                    }
                }

                override fun getCurrentWeatherForCity(city: String): String {
                    return "Current Test Weather for $city"
                }

                override fun getForecastWeatherForCity(city: String): String {
                    return "Forecast Test Weather for $city"
                }
            }
        }
    )
}