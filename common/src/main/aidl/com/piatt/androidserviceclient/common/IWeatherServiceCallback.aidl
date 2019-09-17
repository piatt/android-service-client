package com.piatt.androidserviceclient.common;

import com.piatt.androidserviceclient.common.WeatherServiceState;

/**
 * AIDL callback interface used by the
 * IWeatherService AIDL interface to allow
 * WeatherService to notify WeatherClient
 */
oneway interface IWeatherServiceCallback {
    /**
     * Notifies receivers that WeatherService backend has been initialized or terminated,
     * allowing them to take appropriate steps i.e. using the API
     */
    void onWeatherServiceStateChanged(in WeatherServiceState state);
}