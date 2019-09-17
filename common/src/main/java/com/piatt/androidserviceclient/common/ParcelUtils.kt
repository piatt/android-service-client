package com.piatt.androidserviceclient.common

import android.os.Parcel
import android.os.Parcelable
import java.util.Date
import java.util.UUID

/**
 * Centralized listing of Parcelable related helper extension functions and interfaces,
 * used to construct and deconstruct parameters, as well as lists and arrays of parameters,
 * primarily for the purpose of transmission over IPC
 *
 * @see [https://medium.com/@BladeCoder/reducing-parcelable-boilerplate-code-using-kotlin-741c3124a49a]
 * @author Benjamin Piatt
 */

/**
 * Abstracts the Android Parcelable interface
 * to provide default implementation for certain methods,
 * while allowing them to use and/or override the included methods
 */
interface IParcelable : Parcelable {
    override fun describeContents() = 0

    /**
     * Any implementors that have members to parcel that
     * are not already covered in a parent class or interface
     * must override this method, write their unique members to the parcel,
     * then must reference the parent method via a super call
     */
    override fun writeToParcel(parcel: Parcel, flags: Int)
}

inline fun <reified T> parcelableCreator(crossinline create: (Parcel) -> T) =
    object : Parcelable.Creator<T> {
        override fun createFromParcel(source: Parcel) = create(source)
        override fun newArray(size: Int) = arrayOfNulls<T>(size)
    }

inline fun <reified T : Enum<T>> enumCreator() =
    object : Parcelable.Creator<T> {
        override fun createFromParcel(source: Parcel) = source.readEnum<T>()
        override fun newArray(size: Int) = arrayOfNulls<T>(size)
    }

fun Parcel.readNullableInt() = readNullable { readInt() }

fun Parcel.writeNullableInt(value: Int?) = writeNullable(value) { writeInt(it) }

fun Parcel.readNullableLong() = readNullable { readLong() }

fun Parcel.writeNullableLong(value: Long?) = writeNullable(value) { writeLong(it) }

fun Parcel.readNullableFloat() = readNullable { readFloat() }

fun Parcel.writeNullableFloat(value: Float?) = writeNullable(value) { writeFloat(it) }

inline fun <reified T : Enum<T>> Parcel.readEnum(): T? = readNullable { enumValueOf(readString()!!) as T }

fun <T : Enum<T>> Parcel.writeEnum(value: T?) = writeNullable(value) { writeString(value!!.name) }

fun Parcel.readUUID() = readNullable { UUID(readLong(), readLong()) }

fun Parcel.writeUUID(value: UUID?) = writeNullable(value) {
    writeLong(value!!.mostSignificantBits)
    writeLong(value!!.leastSignificantBits)
}

fun Parcel.readDate() = Date(readLong())

fun Parcel.writeDate(value: Date) = writeLong(value.time)

inline fun <reified T : Parcelable> Parcel.readParcelable(): T? {
    return readParcelable(T::class.java.classLoader) as? T
}

inline fun <reified T> Parcel.readList(): List<T> {
    val list = mutableListOf<T>()
    readList(list as List<*>, T::class.java.classLoader)
    return list.toList()
}

inline fun <reified T> Parcel.readArray(): Array<out T> {
    val size = readInt()
    val arrayList = arrayListOf<T>()
    for (i in 0 until size) {
        arrayList.add(readValue(T::class.java.classLoader) as T)
    }
    return arrayList.toTypedArray()
}

fun <T> Parcel.readNullable(reader: () -> T) = if (readInt() != 0) reader() else null

fun <T> Parcel.writeNullable(value: T?, writer: (T) -> Unit) {
    if (value != null) {
        writeInt(1)
        writer(value)
    } else {
        writeInt(0)
    }
}