package com.piatt.androidserviceclient.common

import android.content.Context
import android.os.IBinder
import android.util.Log
import dagger.Module
import dagger.Provides
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
                override fun register(callback: IWeatherServiceCallback) {
                    Log.d(serviceName, "Registered callback with WeatherService")
                    callback.onWeatherServiceStateChanged(weatherServiceState)
                }

                override fun unregister(callback: IWeatherServiceCallback) {
                    Log.d(serviceName, "Unregistered callback with WeatherService")
                }

                override fun getWeatherServiceState() = WeatherServiceState.STARTED
            }
        }
    )
}