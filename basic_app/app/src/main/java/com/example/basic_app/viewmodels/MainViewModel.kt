package com.example.basic_app.viewmodels

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basic_app.callbacks.BleCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    private val TAG = "MainViewModel"

    private val _connected = MutableLiveData<Boolean>()
    val insoleConnected: LiveData<Boolean> = _connected

    private val _counter = MutableLiveData<Short>()
    val counter: LiveData<Short> = _counter

    val gattCallBack = BleCallback( _connected, _counter)
    var bleDevice: BluetoothDevice? = null


    fun setLed(state: Byte) {
        viewModelScope.launch(Dispatchers.IO) {
            val result=gattCallBack.updateLed(state)
            Log.d(TAG, "setLed result: $result")
            }
        }
    }