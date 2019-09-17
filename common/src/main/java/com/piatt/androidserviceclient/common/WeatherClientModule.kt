package com.piatt.androidserviceclient.common

import android.content.Context
import android.content.Intent
import android.os.IBinder
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class WeatherClientModule {
    private val SERVICE_PACKAGE = "com.piatt.androidserviceclient.services"
    private val SERVICE_CLASS = "$SERVICE_PACKAGE.WeatherService"

    @Provides
    @Singleton
    fun provideWeatherClient(context: Context) = WeatherClient(
        context = context,
        weatherServiceClient = object : AndroidServiceClient<IWeatherService>(
            context = context,
            intent = Intent().setClassName(SERVICE_PACKAGE, SERVICE_CLASS),
            flags = Context.BIND_AUTO_CREATE,
            serviceName = IWeatherService::class.java.simpleName
        ) {
            override fun getServiceInterface(service: IBinder?) = IWeatherService.Stub.asInterface(service)
        }
    )
}