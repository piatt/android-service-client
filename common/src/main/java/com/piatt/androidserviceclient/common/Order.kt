package com.piatt.androidserviceclient.common

import android.os.Parcel
import java.util.*

class Order private constructor(
    val id: UUID,
    val date: Date,
    val price: String,
    val description: String
) : IParcelable {
    constructor(price: String, description: String) : this(
        id = UUID.randomUUID(),
        date = Date(),
        price = price,
        description = description
    )

    private constructor(parcel: Parcel) : this(
        parcel.readUUID()!!,
        parcel.readDate(),
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeUUID(id)
        parcel.writeDate(date)
        parcel.writeString(price)
        parcel.writeString(description)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Order)
    }
}