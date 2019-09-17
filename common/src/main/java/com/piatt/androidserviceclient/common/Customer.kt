package com.piatt.androidserviceclient.common

import android.os.Parcel
import java.util.*

class Customer private constructor(
    val id: UUID,
    val name: String,
    val phoneNumber: String,
    val emailAddress: String,
    val orders: List<Order>
) : IParcelable {
    constructor(name: String, phoneNumber: String, emailAddress: String) : this(
        id = UUID.randomUUID(),
        name = name,
        phoneNumber = phoneNumber,
        emailAddress = emailAddress,
        orders = mutableListOf()
    )

    private constructor(parcel: Parcel) : this(
        parcel.readUUID()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readList<Order>()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeUUID(id)
        parcel.writeString(name)
        parcel.writeString(phoneNumber)
        parcel.writeString(emailAddress)
        parcel.writeList(orders)
    }

    companion object {
        @JvmField val CREATOR = parcelableCreator(::Customer)
    }
}