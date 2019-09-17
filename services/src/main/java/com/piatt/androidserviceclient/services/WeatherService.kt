package com.piatt.androidserviceclient.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.util.Log
import android.app.NotificationManager
import android.os.Build
import com.piatt.androidserviceclient.common.ExceptionHandler
import com.piatt.androidserviceclient.common.IWeatherService
import com.piatt.androidserviceclient.common.IWeatherServiceCallback
import com.piatt.androidserviceclient.common.WeatherServiceState

class WeatherService : Service() {
    /**
     * Dictates whether or not the service has been started,
     * and is used to determine whether binding clients should also start the service
     */
    private var serviceStarted = false

    /**
     * Generic handler used to queue runnable operations synchronously
     */
    private val handler = Handler()

    /**
     * Maintains a list of registered callbacks
     * linking WeatherClient instances to WeatherService
     *
     * @see [https://developer.android.com/reference/android/os/RemoteCallbackList]
     */
    private val callbacks = RemoteCallbackList<IWeatherServiceCallback>()

    /**
     * @see IWeatherService
     */
    private val mBinder = object : IWeatherService.Stub() {
        /**
         * @see IWeatherService.register
         */
        override fun register(callback: IWeatherServiceCallback) {
            callbacks.register(callback)
            Log.d(TAG, "Registered callback with WeatherService")
            callback.onWeatherServiceStateChanged(weatherServiceState)
        }

        /**
         * @see IWeatherService.unregister
         */
        override fun unregister(callback: IWeatherServiceCallback) {
            callbacks.unregister(callback)
            Log.d(TAG, "Unregistered callback with WeatherService")
        }

        /**
         * @see IWeatherService.getWeatherServiceState
         */
        override fun getWeatherServiceState() = if (serviceStarted) WeatherServiceState.STARTED else WeatherServiceState.STOPPED
    }

    companion object {
        private const val TAG = "WeatherService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "WeatherServiceChannel"
        private const val ACTION_RESTART = "com.piatt.androidserviceclient.services.RESTART"
        private const val ACTION_FORCE_CRASH = "com.piatt.androidserviceclient.services.FORCE_CRASH"

        /**
         * Public static helper method used to start the service from the outside
         */
        @JvmStatic fun launch(context: Context) {
            Log.d(TAG, "Launching WeatherService...")
            context.startService(Intent(context, WeatherService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()

        /**
         * Instantiates a handler that intercepts any uncaught exceptions
         * and allows the WeatherService to properly trigger a stop
         * to the WeatherService backend before passing the exception along
         * for default handling by the runtime
         */
        ExceptionHandler {
            Log.d(TAG, "ExceptionHandler invoked")
            tearDownService()
        }

        Log.d(TAG, "WeatherService created")
    }

    override fun onBind(intent: Intent): IWeatherService.Stub {
        Log.d(TAG, "onBind() $intent")

        if (!serviceStarted) {
            Log.d(TAG, "Binding to un-started service which needs to be launched")
            launch(this)
        }
        return mBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand() $intent flags=$flags startId=$startId")

        when {
            intent == null || intent.component != null || flags == START_FLAG_RETRY -> {
                updateServiceState()
                //TODO: Start WeatherService backend
            }
            intent.action != null -> when (intent.action) {
                ACTION_RESTART -> { /*TODO: Restart WeatherService backend */ }
                ACTION_FORCE_CRASH -> throw RuntimeException("CRASH TEST")
                else -> Log.d(TAG, "Unknown start command action: ${intent.action}")
            }
            else -> stopSelf()
        }
        return START_STICKY
    }

    /**
     * Triggers a stop to the WeatherService backend
     * and removes all remote callback references
     * before destroying the service
     */
    override fun onDestroy() {
        tearDownService()
        Log.d(TAG, "WeatherService destroyed")
        super.onDestroy()
    }

    private fun updateServiceState() {
        if (serviceStarted) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(TAG)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        serviceStarted = true
        Log.d(TAG, "Promoted WeatherService to foreground service")
    }

    /**
     * Executes the code block that was passed by the consumer
     * for each callback in the callback list
     */
    private fun notifyCallbacks(codeBlock: (cb: IWeatherServiceCallback) -> Unit) {
        handler.post {
            for (i in 0 until callbacks.beginBroadcast()) {
                try {
                    codeBlock(callbacks.getBroadcastItem(i))
                } catch (e: RemoteException) {}
            }
            callbacks.finishBroadcast()
        }
    }

    private fun tearDownService() {
        Log.d(TAG, "Tearing down WeatherService...")
        //TODO: Stop WeatherService backend
        callbacks.kill()
        serviceStarted = false
    }
}