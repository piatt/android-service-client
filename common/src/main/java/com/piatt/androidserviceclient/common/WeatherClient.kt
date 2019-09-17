package com.piatt.androidserviceclient.common

import android.content.Context
import android.util.Log
import com.piatt.androidserviceclient.common.ServiceClient.State
import com.piatt.androidserviceclient.common.ServiceClient.StateCallback
import kotlinx.coroutines.runBlocking
import kotlin.properties.Delegates

/**
 *
 */
class WeatherClient(
    private val context: Context,
    private val weatherServiceClient: ServiceClient<IWeatherService>
) {
    private val TAG = javaClass.simpleName
    private val callbacks = mutableSetOf<WeatherClientCallback>()

    /**
     * Current state of WeatherService backend
     *
     * When a new assignment is made to the state variable,
     * the observable triggers the state change callback with the new state
     * and triggers a potential update to ContentSections
     *
     * @see WeatherServiceState
     */
    private var weatherServiceState: WeatherServiceState by Delegates.observable(WeatherServiceState.STOPPED) {
            _, _, newState -> run {
            Log.d(TAG, "WeatherServiceState changed to $newState")
            notifyCallbacks { it.onWeatherServiceStateChanged(newState) }
            //TODO: Do anything else that a state change should trigger
        }
    }

    /**
     * @see IWeatherServiceCallback
     */
    private val weatherServiceCallback = object : IWeatherServiceCallback.Stub() {
        override fun onWeatherServiceStateChanged(state: WeatherServiceState) {
            weatherServiceState = state
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
                when (state) {
                    State.DISCONNECTED,
                    State.DISCONNECTED_BY_SERVICE,
                    State.DISCONNECTED_BY_CLIENT,
                    State.CONNECTING -> weatherServiceState = WeatherServiceState.STOPPED
                    State.CONNECTED -> registerServiceCallback()
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
        callback.onWeatherServiceStateChanged(weatherServiceState)
    }

    /**
     * Client domains must call this method when cleaning up
     */
    fun unregister(callback: WeatherClientCallback) {
        callbacks.remove(callback)
        Log.d(TAG, "Unregistered callback ${callback.hashCode()} with WeatherClient")
    }

    /**
     * @see IWeatherService.getWeatherServiceState
     */
    fun isWeatherServiceRunning(): Boolean = runBlocking {
        val state = weatherServiceClient.execute(defaultValue = WeatherServiceState.STOPPED) {
            it.getWeatherServiceState()
        }!!
        when (state) {
            WeatherServiceState.STARTED -> true
            WeatherServiceState.STOPPED -> false
        }
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
        override fun asBinder() = null
    }
}