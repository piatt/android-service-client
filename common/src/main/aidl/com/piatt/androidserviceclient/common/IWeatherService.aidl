package com.piatt.androidserviceclient.common;

import com.piatt.androidserviceclient.common.IWeatherServiceCallback;
import com.piatt.androidserviceclient.common.WeatherServiceState;

/**
 * AIDL interface used for IPC communication
 * between WeatherService and WeatherClient
 */
interface IWeatherService {
    /**
     * Registers the given callback with the WeatherService backend
     * to allow the caller to receive pushed events
     */
    oneway void register(IWeatherServiceCallback callback);

    /**
     * Unregisters from the WeatherService backend the given callback
     * that was used by the caller to receive pushed events
     */
    oneway void unregister(IWeatherServiceCallback callback);

    /**
     * Returns the current WeatherService state
     * as defined by the WeatherService backend
     */
    WeatherServiceState getWeatherServiceState();
}