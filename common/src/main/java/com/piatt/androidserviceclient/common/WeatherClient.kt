package com.piatt.androidserviceclient.common

import android.content.Context
import android.util.Log
import com.piatt.androidserviceclient.common.ServiceClient.State
import com.piatt.androidserviceclient.common.ServiceClient.StateCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.properties.Delegates

class WeatherClient(
    private val context: Context,
    private val weatherServiceClient: ServiceClient<IWeatherService>
) {
    private val TAG = javaClass.simpleName
    private val callbacks = mutableSetOf<WeatherClientCallback>()

    /**
     * @see IWeatherServiceCallback
     */
    private val weatherServiceCallback = object : IWeatherServiceCallback.Stub() {
        override fun onWeatherUpdate(timestamp: Long) {
            notifyCallbacks { it.onWeatherUpdate(timestamp) }
        }
    }

    /**
     * Instantiates a handler that intercepts any uncaught exceptions
     * and allows the WeatherClient to properly unregister and unbind
     * from the WeatherService before passing the exception along
     * for default handling by the runtime
     */
    init {
        ExceptionHandler {
            Log.d(TAG,"ExceptionHandler invoked")
            unregisterServiceCallback()
            weatherServiceClient.disconnectService()
        }

        weatherServiceClient.stateCallback = object : StateCallback {
            override fun onStateChanged(state: State) {
                notifyCallbacks { it.onClientStateChanged(state) }
                if (state == State.CONNECTED) {
                    registerServiceCallback()
                }
            }
        }
        weatherServiceClient.connectService()
    }

    /**
     * Client domains must call this method during setup
     * to register themselves in order to receive callbacks
     */
    fun register(callback: WeatherClientCallback) {
        callbacks.add(callback)
        Log.d(TAG, "Registered callback ${callback.hashCode()} with WeatherClient")
        //TODO: return current client state and weather update timestamp
    }

    /**
     * Client domains must call this method when cleaning up
     */
    fun unregister(callback: WeatherClientCallback) {
        callbacks.remove(callback)
        Log.d(TAG, "Unregistered callback ${callback.hashCode()} with WeatherClient")
    }

    /**
     * @see IWeatherService.updateWeather
     */
    fun updateWeather() {
        weatherServiceClient.execute("updateWeather()") {
            it.updateWeather()
        }
    }

    /**
     * @see IWeatherService.getCurrentWeatherForCity
     */
    suspend fun getCurrentWeatherForCity(city: String): String {
        return weatherServiceClient.execute("No data found!", "getCurrentWeatherForCity($city)") {
            it.getCurrentWeatherForCity(city)
        }!!
    }

    /**
     * @see IWeatherService.getForecastWeatherForCity
     */
    suspend fun getForecastWeatherForCity(city: String): String {
        return weatherServiceClient.execute("No data found!", "getForecastWeatherForCity($city)") {
            it.getForecastWeatherForCity(city)
        }!!
    }

    /**
     * @see IWeatherService.register
     */
    private fun registerServiceCallback() {
        Log.d(TAG, "Registering callback with WeatherService...")
        weatherServiceClient.execute {
            it.register(weatherServiceCallback)
        }
    }

    /**
     * @see IWeatherService.unregister
     */
    private fun unregisterServiceCallback() {
        Log.d(TAG, "Unregistering callback with WeatherService...")
        weatherServiceClient.execute {
            it.unregister(weatherServiceCallback)
        }
    }

    /**
     * Executes the code block that was passed by the consumer
     * for each callback in the callback list
     */
    private fun notifyCallbacks(codeBlock: (cb: WeatherClientCallback) -> Unit) {
        callbacks.forEach { codeBlock(it) }
    }

    /**
     * Callback interface that clients must use
     * to be notified of changes from within WeatherClient.
     * Note: More methods may eventually be added to this interface
     *
     * @see IWeatherServiceCallback
     */
    interface WeatherClientCallback : IWeatherServiceCallback {
        fun onClientStateChanged(state: State)

        override fun asBinder() = null
    }
}