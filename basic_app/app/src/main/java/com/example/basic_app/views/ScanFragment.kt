package com.example.basic_app.views

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.basic_app.viewmodels.MainViewModel
import com.example.basic_app.databinding.FragmentScanBinding
import java.util.regex.Pattern

class ScanFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentScanBinding? = null
    private val TAG = "ScanFragment"
    private val deviceFilter: BluetoothLeDeviceFilter = BluetoothLeDeviceFilter.Builder()
        .setNamePattern(Pattern.compile("ESP32_*"))
        .build()

    private val pairingRequest: AssociationRequest = AssociationRequest.Builder()
        .addDeviceFilter(deviceFilter)
        .setSingleDevice(false)
        .build()

    @SuppressLint("MissingPermission")//TODO: check permission
    private val getBLEDevice= registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data= result.data
            if(data != null && data.hasExtra(CompanionDeviceManager.EXTRA_DEVICE))
            {
                var scanResult: ScanResult? = data.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                var device: BluetoothDevice? = scanResult?.device
                mainViewModel.bleDevice = device
                device?.connectGatt(requireContext(), true, mainViewModel.gattCallBack)
            }
        }
    }

    private val callbackScan: CompanionDeviceManager.Callback = object : CompanionDeviceManager.Callback() {
        override fun onDeviceFound(chooserLauncher: IntentSender) {
            Log.v(TAG,"onDeviceFound")
            getBLEDevice.launch(IntentSenderRequest.Builder(chooserLauncher).build())
        }
        override fun onFailure(error: CharSequence?) {
            Log.v(TAG,"onFailure")
            // Handle the failure.
        }
    }

    private val requestBLEPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            if (isGranted) {
                Log.v(TAG,"BLE permission granted")
            } else {
                Log.v(TAG," no BLE permission granted")
                // TODO: Close APP
            }
        }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentScanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.mainViewModel = mainViewModel
        binding.lifecycleOwner = this

        //Print memory direction of the insoleConnected
        Log.d(TAG, "insoleConnected: ${mainViewModel.insoleConnected}")

        binding.scanBle.setOnClickListener{ onScanBLE() }

        Log.v(TAG,"onScanBLE")
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG,"onScanBLE: no permission")
            requestBLEPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            Log.v(TAG,"onScanBLE: has permission")
        }

        return root
    }

    private fun onScanBLE() {
        val deviceManager: CompanionDeviceManager = requireContext().getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
        deviceManager.associate(pairingRequest,callbackScan, null)
        Log.v(TAG,"associate done ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}