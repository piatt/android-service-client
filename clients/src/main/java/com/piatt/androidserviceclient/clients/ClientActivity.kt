package com.piatt.androidserviceclient.clients

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.piatt.androidserviceclient.common.WeatherClient
import com.piatt.androidserviceclient.common.WeatherClient.WeatherClientCallback
import com.piatt.androidserviceclient.common.WeatherServiceState
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_client.*
import javax.inject.Inject

class ClientActivity : AppCompatActivity() {
    @Inject
    lateinit var weatherClient: WeatherClient

    private val callback = object : WeatherClientCallback {
        override fun onWeatherServiceStateChanged(state: WeatherServiceState) {
            runOnUiThread { callback_state_view.text = state.name }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        weatherClient.register(callback)
        state_button.setOnClickListener {
            button_state_view.text = weatherClient.isWeatherServiceRunning().toString()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        if (isFinishing) {
            weatherClient.unregister(callback)
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}