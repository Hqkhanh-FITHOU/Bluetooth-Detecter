package com.example.bluetoothdetector

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.bluetoothdetector.adapter.DeviceItemAdapter
import com.example.bluetoothdetector.databinding.ActivityMainBinding
import java.io.IOException
import java.util.UUID


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var myDeviceItemAdapter: DeviceItemAdapter
    private lateinit var newDeviceItemAdapter: DeviceItemAdapter

    private lateinit var myDevices: MutableSet<BluetoothDevice>
    private val newDetectedDevices = mutableSetOf<BluetoothDevice>()

    companion object {
        const val MY_UUID = "56a294f6-ebea-4ce9-b814-024341c2328a"
    }


    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                checkBluetoothState()
                Toast.makeText(this, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
                startBluetoothDiscovery()
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
//            startBluetoothDiscovery()
//        } else {
//            Toast.makeText(this, "Needs permission to scan bluetooth", Toast.LENGTH_SHORT).show()
//        }
//    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.getAdapter()

        newDeviceItemAdapter = DeviceItemAdapter(newDetectedDevices)
        clickConnectDevice(newDeviceItemAdapter)
        binding.newDetectedDevicesRecyclerView.adapter = newDeviceItemAdapter

        Toast.makeText(this, "Bluetooth is available", Toast.LENGTH_SHORT).show()

        requestForEnableBluetooth()

        binding.discoverBtn.setOnClickListener {
            if (bluetoothAdapter.isEnabled) {
                startBluetoothDiscovery()
            } else {
                requestForEnableBluetooth()
            }
        }


    }

    private fun clickConnectDevice(adapter: DeviceItemAdapter) {
        Toast.makeText(this@MainActivity, "connecting...", Toast.LENGTH_SHORT)
            .show()
        adapter.setOnClickToConnectBluetoothDeviceListener(object :
            OnClickToConnectBluetoothDeviceListener {
            override fun clickToConnect(button: Button, device: BluetoothDevice) {
                if (button.text == "connect") {
                    try {
                        val connectThread = ConnectThread(device, button)
                        connectThread.start()

                    } catch (e: IOException) {
                        Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Already connected", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })
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
            clickConnectDevice(newDeviceItemAdapter)
            binding.myDevicesRecyclerView.adapter = myDeviceItemAdapter
            showMyDevices()
        }
    }


    private fun startBluetoothDiscovery() {

        PermissionChecker.checkBluetoothConnectionPermission(this) {
            if (bluetoothAdapter.isDiscovering) {
                Toast.makeText(this, "Discovery is already running", Toast.LENGTH_SHORT).show()
            } else {
                PermissionChecker.checkBluetoothConnectionPermission(this) {
                    bluetoothAdapter.startDiscovery()
                    Toast.makeText(this, "Discovery is running", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
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


    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action.toString()
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    PermissionChecker.checkBluetoothConnectionPermission(this@MainActivity){
                        if (device?.name != null){
                            newDetectedDevices.add(device)
                        }
                    }
                    updateDetectedRecyclerView()
                }
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun updateDetectedRecyclerView() {
        newDeviceItemAdapter.notifyDataSetChanged()
        showDetectedDevices()
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
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

    private fun updateUIOnConnectionSuccess(device: BluetoothDevice, button: Button) {
        // Example: Update the UI to show connection success
        PermissionChecker.checkBluetoothConnectionPermission(this) {
            button.text = "Connected"
        }
    }


    @SuppressLint("MissingPermission")
    private inner class ConnectThread(
        private val bluetoothDevice: BluetoothDevice,
        private var button: Button
    ) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
        }


        public override fun run() {
            super.run()
            bluetoothAdapter.cancelDiscovery()
            mmSocket?.let { socket ->
                try {
                    socket.connect()
                    runOnUiThread {
                        Log.d("CONNECT_THREAD", "Connected to ${bluetoothDevice.name}}")
                        updateUIOnConnectionSuccess(bluetoothDevice, button)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Connection failed", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("CONNECT_THREAD", "Could not close the client socket", e)
            }
        }
    }

}

