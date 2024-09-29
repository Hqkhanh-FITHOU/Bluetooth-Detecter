package com.example.bluetoothdetector

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.bluetoothdetector.adapter.DeviceItemAdapter
import com.example.bluetoothdetector.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var myDeviceItemAdapter: DeviceItemAdapter
    private lateinit var newDeviceItemAdapter: DeviceItemAdapter

    private lateinit var myDevices: MutableSet<BluetoothDevice>
    private val newDetectedDevices = mutableSetOf<BluetoothDevice>()





    private val enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if(result.resultCode == RESULT_OK){
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
        binding.newDetectedDevicesRecyclerView.adapter = newDeviceItemAdapter

        Toast.makeText(this, "Bluetooth is available", Toast.LENGTH_SHORT).show()

        requestForEnableBluetooth()

        binding.discoverBtn.setOnClickListener {
            if(bluetoothAdapter.isEnabled){
                startBluetoothDiscovery()
            }else {
                requestForEnableBluetooth()
            }
        }

    }





    private fun requestForEnableBluetooth(){
        PermissionChecker.checkBluetoothConnectionPermission(this){
            try{
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            }catch (e: Exception){
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





    private fun startBluetoothDiscovery() {

        PermissionChecker.checkBluetoothConnectionPermission(this) {
            if (bluetoothAdapter.isDiscovering) {
                Toast.makeText(this, "Discovery is already running", Toast.LENGTH_SHORT).show()
            }else{
                bluetoothAdapter.startDiscovery()
                Toast.makeText(this, "Discovery is running", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show()
        } else {
            binding.newDetectedDevicesRecyclerView.visibility = View.VISIBLE
            binding.text5.visibility = View.GONE
            Toast.makeText(this, "${newDetectedDevices.size} devices found", Toast.LENGTH_SHORT)
                .show()
        }
    }




    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action.toString()
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    PermissionChecker.checkBluetoothConnectionPermission(this@MainActivity) {
                        Toast.makeText(
                            this@MainActivity,
                            "Device found: ${device!!.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    newDetectedDevices.add(device!!)
                    updateDetectedRecyclerView()
                }
            }
        }
    }





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
            124 -> {
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