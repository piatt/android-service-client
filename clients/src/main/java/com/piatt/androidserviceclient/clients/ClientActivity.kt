package com.piatt.androidserviceclient.clients

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.piatt.androidserviceclient.common.ServiceClient
import com.piatt.androidserviceclient.common.WeatherClient
import com.piatt.androidserviceclient.common.WeatherClient.WeatherClientCallback
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_client.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ClientActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    @Inject
    lateinit var weatherClient: WeatherClient

    private val callback = object : WeatherClientCallback {
        private val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

        override fun onWeatherUpdate(timestamp: Long) {
            runOnUiThread {
                service_state_view.text = String.format(getString(R.string.service_state), sdf.format(Date(timestamp)))
            }
        }

        override fun onClientStateChanged(state: ServiceClient.State) {
            runOnUiThread {
                client_state_view.text = String.format(getString(R.string.client_state), state.name)
                root_view.setBackgroundColor(getColor(if (state == ServiceClient.State.CONNECTED) R.color.connected else R.color.disconnected))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        weatherClient.register(callback)
        force_crash_button.setOnClickListener { throw RuntimeException("CLIENT APP CRASH") }
        current_weather_button.setOnClickListener {
            scope.launch {
                weather_view.text = weatherClient.getCurrentWeatherForCity(search_view.text.trim().toString()).toString()
            }
        }
        forecast_weather_button.setOnClickListener {
            scope.launch {
                weather_view.text = weatherClient.getForecastWeatherForCity(search_view.text.trim().toString()).toString()
            }
        }
        refresh_weather_button.setOnClickListener {
            weatherClient.updateWeather()
        }
    }

    override fun onPause() {
        if (isFinishing) {
            weatherClient.unregister(callback)
            scope.cancel()
        }
        super.onPause()
    }
}