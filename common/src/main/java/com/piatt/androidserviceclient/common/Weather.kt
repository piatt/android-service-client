package com.piatt.androidserviceclient.common

import android.os.Parcel

class Weather(
    val date: Long,
    val location: String,
    val condition: String,
    val temperature: Float
) : IParcelable {
    private constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(date)
        parcel.writeString(location)
        parcel.writeString(condition)
        parcel.writeFloat(temperature)
    }

    override fun toString(): String {
        return "Weather(date=$date, location='$location', condition='$condition', temperature=$temperature)"
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Weather)
    }
}