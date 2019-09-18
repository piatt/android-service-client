package com.piatt.androidserviceclient.common;

/**
 * AIDL callback interface used by the
 * IWeatherService AIDL interface to allow
 * WeatherService to notify WeatherClient
 */
oneway interface IWeatherServiceCallback {
    /**
     * Notifies receivers of when the latest weather update was made,
     * allowing them to take appropriate steps i.e. using the API
     */
    void onWeatherUpdate(in long timestamp);
}