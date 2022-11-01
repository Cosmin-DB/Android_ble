package com.example.basic_app.callbacks

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.basic_app.utils.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class BleCallback(
        private val connected: MutableLiveData<Boolean>,
        private val counter: MutableLiveData<Short>
) : BluetoothGattCallback() {
    private val TAG = "BleCallback"
    private lateinit var mLedCharacteristic: BluetoothGattCharacteristic
    private lateinit var mCounterCharacteristic: BluetoothGattCharacteristic
    private lateinit var mGatt: BluetoothGatt
    private var mClose = false
    private val writeCharacteristicChannel = Channel<BluetoothResult>()
    private val readCharacteristicChannel = Channel<BluetoothResult>() //TODO: read specific characteristic
    private val gattSemaphore: Semaphore = Semaphore(1)

    @SuppressLint("MissingPermission") //TODO: check how to check permission appropriately in this and other case
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        mGatt = gatt
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> {
                Log.d(TAG, "Connected to GATT server.")
                //mConnected = true
                connected.postValue(true)

                var result = mGatt.discoverServices()
                Log.d(TAG, "Attempting to start service discovery:$result")
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                //mConnected = false
                connected.postValue(false)
                Log.d(TAG, "Disconnected from GATT server.")
                if (mClose) { //if the user specify that want to disconnect, do not try to reconnect.
                    Log.d(TAG, "mClose=$mClose end of BleGattCallback")
                    mGatt.close()
                } else {
                    // TODO: try to reconnect
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)

        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onServicesDiscovered: $status")
            val gattInsoleService = gatt.getService(UUID.fromString(serviceUUID))
            mLedCharacteristic = gattInsoleService.getCharacteristic(UUID.fromString(ledUUID))
            mCounterCharacteristic = gattInsoleService.getCharacteristic(UUID.fromString(counterUUID))
            var result=gatt.setCharacteristicNotification(mCounterCharacteristic, true)
            Log.d(TAG, "setCharacteristicNotification result: $result")
        } else {
            Log.d(TAG, "onServicesDiscovered, NO GATT SUCCESS: $status")
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)
        Log.d(TAG, "onCharacteristicChanged: ${characteristic.uuid}")
        when (characteristic.uuid.toString().uppercase()) {
            counterUUID -> {
                val value = characteristic.value
                val buffer = ByteBuffer.wrap(value)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                val counterValue = buffer.short
                counter.postValue(counterValue)
                //counter.postValue(ByteBuffer.wrap(characteristic.value).order(ByteOrder.LITTLE_ENDIAN).int)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
        if (connected.value == false) {
            throw BluetoothNotConnectedException()
        }
        gattSemaphore.acquire() // All function that have gatt operation must acquire the semaphore
        val test=mGatt.writeCharacteristic(characteristic)
        Log.d(TAG, "writeCharacteristic: $test")
        // Wait 3s for the result of the write operation and return the result
        val result: Boolean = withTimeoutOrNull(10000) {
            var bluetoothResult: BluetoothResult = writeCharacteristicChannel.receive()
            while (bluetoothResult.uuid != characteristic.uuid) {
                //TODO: something is wrong, the uuid is not the one expected. This is not supposed to happen
                bluetoothResult = writeCharacteristicChannel.receive()
            }
            true
        }?: run {   false  }
        gattSemaphore.release()
        return result
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        writeCharacteristicChannel.offer(BluetoothResult(characteristic.uuid, characteristic.value, status))
    }

    suspend fun updateLed(state: Byte): Boolean {
        /*val buffer=ByteBuffer.allocate(Short.SIZE_BYTES) // Si no es un byte usar un buffer, ej para un short 2 de bytes
        buffer.putShort(state)*/
        mLedCharacteristic.setValue(byteArrayOf(state))
        return writeCharacteristic(mLedCharacteristic)
    }
}