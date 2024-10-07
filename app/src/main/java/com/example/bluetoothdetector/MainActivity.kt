package com.example.bluetoothdetector


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothdetector.adapter.DeviceItemAdapter
import com.example.bluetoothdetector.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private lateinit var myDeviceItemAdapter: DeviceItemAdapter
    private lateinit var newDeviceItemAdapter: DeviceItemAdapter

    private lateinit var myDevices: MutableSet<BluetoothDevice>
    private val newDetectedDevices = mutableSetOf<BluetoothDevice>()

    private var scanning = false
    //private val handlerThread = HandlerThread("BackgroundThread")



    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                checkBluetoothState()
                Toast.makeText(this, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
            } else {
                checkBluetoothState()
                Toast.makeText(this, "Bluetooth has been disabled", Toast.LENGTH_SHORT).show()
            }
        }


//    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->
//        val bluetoothScanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
//        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
//
//        if (bluetoothScanGranted && locationGranted) {
//            startBluetoothScanner()
//        } else {
//            Toast.makeText(this, "Needs permission to scan bluetooth", Toast.LENGTH_SHORT).show()
//        }
//    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        newDeviceItemAdapter = DeviceItemAdapter(newDetectedDevices)

        binding.newDetectedDevicesRecyclerView.adapter = newDeviceItemAdapter


        requestForEnableBluetooth()
        startBluetoothScanner()

        binding.discoverBtn.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                startBluetoothScanner()
            } else {
                requestForEnableBluetooth()
            }
        }

    }

    private val scanCallback : ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val device = it.device
                newDetectedDevices.add(device)
                updateDetectedRecyclerView()
                val rssi = it.rssi
                lateinit var deviceName:String
                PermissionChecker.checkBluetoothConnectionPermission(this@MainActivity){
                    deviceName = device.name ?: "Unknown Device"
                }
                val deviceAddress = device.address
                Log.d("BLE_SCAN", "Found device: $deviceName with address: $deviceAddress and RSSI: $rssi")
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { result ->
                val device = result.device

                lateinit var deviceName:String
                PermissionChecker.checkBluetoothConnectionPermission(this@MainActivity){
                    deviceName = device.name ?: "Unknown Device"
                }
                val deviceAddress = device.address
                Log.d("BLE_SCAN", "Batch found device: $deviceName with address: $deviceAddress")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e("BLE_SCAN", "Scan failed with error: $errorCode")
        }
    }



    private fun requestForEnableBluetooth() {
        PermissionChecker.checkBluetoothConnectionPermission(this) {
            try {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            } catch (e: Exception) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
            }

        }
        checkBluetoothState()
    }


    private fun showMyDevices() {
        if (myDevices.isEmpty()) {
            binding.myDevicesRecyclerView.visibility = View.GONE
            binding.text4.visibility = View.VISIBLE

        } else {
            binding.myDevicesRecyclerView.visibility = View.VISIBLE
            binding.text4.visibility = View.GONE
        }
    }


    private fun checkBluetoothState() {
        if (!bluetoothAdapter.isEnabled) {
            binding.text1.text = "Bluetooth is disabled"

        } else {
            binding.text1.text = "Bluetooth is enabled"

            PermissionChecker.checkBluetoothConnectionPermission(this) {
                myDevices = bluetoothAdapter.bondedDevices
            }
            myDeviceItemAdapter = DeviceItemAdapter(myDevices)

            binding.myDevicesRecyclerView.adapter = myDeviceItemAdapter
            showMyDevices()
        }
    }


    private fun startBluetoothScanner() {
        if(!scanning){
            PermissionChecker.checkBluetoothConnectionPermission(this){
                Handler(Looper.getMainLooper()).postDelayed({
                    scanning = false
                    bluetoothLeScanner.stopScan(scanCallback)
                    binding.progressBar.visibility = View.INVISIBLE
                    Log.d("BLE_SCAN", "Scan stopped")
                }, 10000)
                bluetoothLeScanner.startScan(scanCallback)
                scanning = true
                Log.d("BLE_SCAN", "Scanning")
                binding.progressBar.visibility = View.VISIBLE
            }
        }else{
            scanning = false
            bluetoothLeScanner.stopScan(scanCallback)
            binding.progressBar.visibility = View.INVISIBLE
            Log.d("BLE_SCAN", "Scan stopped")
        }
        showDetectedDevices()
        showMyDevices()
    }


    private fun showDetectedDevices() {
        if (newDetectedDevices.isEmpty()) {
            binding.newDetectedDevicesRecyclerView.visibility = View.GONE
            binding.text5.visibility = View.VISIBLE
        } else {
            binding.newDetectedDevicesRecyclerView.visibility = View.VISIBLE
            binding.text5.visibility = View.GONE
        }
    }




    private fun updateDetectedRecyclerView() {
        newDeviceItemAdapter.notifyItemInserted(newDetectedDevices.size-1)
        showDetectedDevices()
    }


    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionChecker.REQUEST_BLUETOOTH_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    binding.text1.text = "Bluetooth is denied"
                    binding.progressBar.visibility = View.GONE
                } else {
                    checkBluetoothState()
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }


}

