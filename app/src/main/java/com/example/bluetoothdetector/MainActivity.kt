package com.example.bluetoothdetector


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
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
    private var bluetoothLeService : BluetoothLeService? = null

    private lateinit var myDeviceItemAdapter: DeviceItemAdapter
    private lateinit var newDeviceItemAdapter: DeviceItemAdapter

    private lateinit var myDevices: MutableList<BluetoothDevice>
    private val newDetectedDevices = mutableListOf<BluetoothDevice>()

    private var scanning = false
    private var connected = false
    private var deviceAddress = ""
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


    private val serviceConnection : ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothLeService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothLeService!!.setContext(this@MainActivity)
            bluetoothLeService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e("BLE Service", "Unable to initialize Bluetooth")
                    finish()
                }
                Log.e("BLE Service", "Initialize Bluetooth")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothLeService = null
        }
    }


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

        newDeviceItemAdapter.setOnClickToConnectBluetoothDeviceListener(object : OnClickToConnectBluetoothDeviceListener{
            override fun clickToConnect(button: Button, device: BluetoothDevice) {
                Log.d("MAIN","item ${device.address} clicked")
                deviceAddress = device.address
                PermissionChecker.checkBluetoothConnectionPermission(this@MainActivity){
                    val bluetoothGatt = device.connectGatt(this@MainActivity, false, bluetoothGattCallback)
                }
            }
        })


        binding.discoverBtn.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                startBluetoothScanner()
            } else {
                requestForEnableBluetooth()
            }
        }

        startBluetoothScanner()
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                Log.d(BluetoothLeService.BLE_SERVICE, "BLE Gatt connected")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                Log.d(BluetoothLeService.BLE_SERVICE, "BLE Gatt disconnected")
            }
        }
    }

    private val scanCallback : ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val device = it.device
                if(device.name != null){
                    newDeviceItemAdapter.addItemToEnd(device)
                    val rssi = it.rssi
                    lateinit var deviceName:String
                    PermissionChecker.checkBluetoothConnectionPermission(this@MainActivity){
                        deviceName = device.name ?: "Unknown Device"
                    }
                    val deviceAddress = device.address
                    Log.d("BLE_SCAN", "Found device: $deviceName with address: $deviceAddress and RSSI: $rssi")
                }
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



    private val gattUpdateReceiver : BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    connected = true
                    updateConnectionState(R.string.connected)
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    connected = false
                    updateConnectionState(R.string.disconnected)
                }
            }
        }
    }



    private fun updateConnectionState(connected: Int) {
        when(connected){
            R.string.connected -> {
                Toast.makeText(this, "connected with device address: $deviceAddress", Toast.LENGTH_SHORT).show()
                Log.d(BluetoothLeService.BLE_SERVICE,"connected with device address: $deviceAddress")
            }
            R.string.disconnected -> {
                Toast.makeText(this, "cannot connected with device address: $deviceAddress", Toast.LENGTH_SHORT).show()
                Log.d(BluetoothLeService.BLE_SERVICE,"cannot connected with device address: $deviceAddress")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val gattServiceIntent = Intent(this@MainActivity, BluetoothLeService::class.java)
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "OnResume")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter(), RECEIVER_EXPORTED)
            Log.d("BLE Service", "BLE Gatt receiver has registered")
        }else{
            registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
            Log.d("BLE Service", "BLE Gatt receiver has registered")
        }
        if (bluetoothLeService != null) {
            val result = bluetoothLeService!!.connect(deviceAddress)
            Log.d("BLE Service", "Connect request result=$result")
        }
    }



    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        }
    }



    override fun onPause() {
        super.onPause()
        unregisterReceiver(gattUpdateReceiver)
        Log.d("BLE Service", "BLE Gatt receiver has unregistered")
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
                myDevices = bluetoothAdapter.bondedDevices.toMutableList()
            }
            myDeviceItemAdapter = DeviceItemAdapter(myDevices)

            binding.myDevicesRecyclerView.adapter = myDeviceItemAdapter
            showMyDevices()
        }
    }



    private fun startBluetoothScanner() {
        if(!scanning){
            PermissionChecker.checkBluetoothConnectionPermission(this){
                bluetoothLeScanner.startScan(scanCallback)
                scanning = true
                Log.d("BLE_SCAN", "Scanning")
                binding.progressBar.visibility = View.VISIBLE
                binding.discoverBtn.text = "Discovering"
                binding.discoverBtn.isEnabled = false
                showDetectedDevices()
                Handler(Looper.getMainLooper()).postDelayed({
                    scanning = false
                    bluetoothLeScanner.stopScan(scanCallback)
                    binding.discoverBtn.text = "Discovery"
                    binding.discoverBtn.isEnabled = true
                    binding.progressBar.visibility = View.INVISIBLE
                    showDetectedDevices()
                    Log.d("BLE_SCAN", "Scan stopped")
                }, 10000)
            }
        }else{
            scanning = false
            bluetoothLeScanner.stopScan(scanCallback)
            binding.progressBar.visibility = View.INVISIBLE
            Log.d("BLE_SCAN", "Scan stopped")
        }

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

