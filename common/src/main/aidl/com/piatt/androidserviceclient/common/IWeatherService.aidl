package com.piatt.androidserviceclient.common;

import com.piatt.androidserviceclient.common.IWeatherServiceCallback;

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

    /*
     * Triggers the backend to refresh latest weather data
     */
    oneway void updateWeather();

    /**
     * Returns the JSON representation of the current weather
     * as defined by the WeatherService backend for the given city
     *
     * @param city: string representation of a city name
     */
    String getCurrentWeatherForCity(in String city);

    /**
     * Returns the JSON representation of a list of weather forecasts
     * as defined by the WeatherService backend for the given city
     *
     * @param city: string representation of a city name
     */
    String getForecastWeatherForCity(in String city);
}