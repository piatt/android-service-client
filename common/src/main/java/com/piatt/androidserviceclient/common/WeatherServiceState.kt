package com.piatt.androidserviceclient.common

import android.os.Parcel

/**
 * Defines the various states that WeatherService can have,
 * either due to normal work flows or due to unexpected terminations or exceptions.
 * Consumers can utilize these states for debugging purposes, as well as to react accordingly
 *
 * @property STARTED: WeatherService has notified WeatherClient that WeatherService backend has or is started
 * @property STOPPED: WeatherService has notified WeatherClient that WeatherService backend has or is stopped
 */
enum class WeatherServiceState : IParcelable {
    STARTED, STOPPED;

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeEnum(this)
    }

    companion object {
        @JvmField val CREATOR = enumCreator<WeatherServiceState>()
    }
}