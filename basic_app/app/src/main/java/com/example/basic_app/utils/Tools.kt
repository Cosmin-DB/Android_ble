package com.example.basic_app.utils

import java.util.*


const val SELECT_DEVICE_REQUEST_CODE = 50 // arbitrary


const val baseUUID = "6A9D6DB8-7DBE-4AE1-A5BC-B4E55A2D73D"
const val serviceUUID = baseUUID + "0"
const val ledUUID = baseUUID + "1"
const val counterUUID = baseUUID + "2"

data class BluetoothResult(val uuid: UUID, val value: ByteArray?, val status: Int)


//BluetoothNotConnectedException
class BluetoothNotConnectedException : Throwable() {
    override val message: String?
        get() = "Bluetooth device not connected"
}