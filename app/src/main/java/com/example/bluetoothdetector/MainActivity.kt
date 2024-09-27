package com.example.bluetoothdetector

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.CompoundButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bluetoothdetector.adapter.DeviceItemAdapter
import com.example.bluetoothdetector.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var mydeviceItemAdapter: DeviceItemAdapter;
    private lateinit var newdeviceItemAdapter: DeviceItemAdapter;

    private lateinit var myDevices: MutableSet<BluetoothDevice>
    private val newDetectedDevices = mutableSetOf<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.getAdapter()

        newdeviceItemAdapter = DeviceItemAdapter(newDetectedDevices)
        binding.newDetectedDevicesRecyclerView.adapter = newdeviceItemAdapter

        if(bluetoothAdapter == null){
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show()
        }else {
            Toast.makeText(this, "Bluetooth is available", Toast.LENGTH_SHORT).show()

            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            PermissionChecker.checkBluetoothConnectionPermission(this) {
                startActivityForResult(enableBtIntent, 123)
            }
            checkBluetoothState()
        }



        binding.discoverBtn.setOnClickListener {
            startBluetoothDiscovery()
        }

    }

    private fun showMyDevices(){
        if(myDevices.isEmpty()){
            binding.myDevicesRecyclerView.visibility = View.GONE
            binding.text4.visibility = View.VISIBLE

        }else {
            binding.myDevicesRecyclerView.visibility = View.VISIBLE
            binding.text4.visibility = View.GONE
        }
    }



    private fun checkBluetoothState() {
        if (!bluetoothAdapter.isEnabled) {
            binding.text1.text = "Bluetooth is disabled"

        }else {
            binding.text1.text = "Bluetooth is enabled"

            PermissionChecker.checkBluetoothConnectionPermission(this) {
                myDevices = bluetoothAdapter.bondedDevices
            }
            mydeviceItemAdapter = DeviceItemAdapter(myDevices)
            binding.myDevicesRecyclerView.adapter = mydeviceItemAdapter
            showMyDevices()
        }
    }


    private fun startBluetoothDiscovery() {
        PermissionChecker.checkBluetoothConnectionPermission(this){
            if(bluetoothAdapter.isDiscovering){
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        showDetectedDevices()
    }


    private fun showDetectedDevices(){
        if(newDetectedDevices.isEmpty()){
            binding.newDetectedDevicesRecyclerView.visibility = View.GONE
            binding.text5.visibility = View.VISIBLE
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show()
        }else {
            binding.newDetectedDevicesRecyclerView.visibility = View.VISIBLE
            binding.text5.visibility = View.GONE
            Toast.makeText(this, "${newDetectedDevices.size} devices found", Toast.LENGTH_SHORT).show()
        }
    }

    private val receiver = object : BroadcastReceiver() {

        @SuppressLint("NewApi")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action.toString()
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice = intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)!!
                    device.let {
                        PermissionChecker.checkBluetoothConnectionPermission(this@MainActivity) {
                            Toast.makeText(
                                this@MainActivity,
                                "Device found: ${device.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        newDetectedDevices.add(device)
                        updateRecyclerView()
                    }
                }
            }
        }
    }

    private fun updateRecyclerView() {
        newdeviceItemAdapter.notifyDataSetChanged()
        showDetectedDevices()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            123 -> {
                checkBluetoothState()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            124 -> {
                if(grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    binding.text1.text = "Bluetooth is denied"
                    binding.progressBar.visibility = View.GONE
                }else {
                    checkBluetoothState()
                    binding.progressBar.visibility = View.VISIBLE
                }
            }

        }
    }

}