package com.example.bluetoothdetector


import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class BluetoothLeService (
    private var context: Context? = null
): Service() {

    companion object {
        const val BLE_SERVICE : String = "BLE Service"
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2
    }
    private var connectionState = STATE_DISCONNECTED
    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setContext(context: Context){
        this.context = context
    }

    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
             Log.e(BLE_SERVICE, "Unable to obtain a BluetoothAdapter.")
             return false
        }
        return true
    }

    inner class LocalBinder : Binder(){
        fun getService() : BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                // connect to the GATT server on the device
                context?.let {
                    PermissionChecker.checkBluetoothConnectionPermission(it){
                        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                    }
                }
            } catch (exception: IllegalArgumentException) {
                Log.w(BLE_SERVICE, "Device not found with provided address.")
                return false
            }
        } ?: run {
            Log.w(BLE_SERVICE, "BluetoothAdapter not initialized")
            return false
        }
        return true
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                connectionState = STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                connectionState = STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        close()
        return super.onUnbind(intent)
    }

    private fun close() {
        bluetoothGatt?.let { gatt ->
            context?.let {
                PermissionChecker.checkBluetoothConnectionPermission(it){
                    gatt.close()
                }
            }
            gatt.close()
            bluetoothGatt = null
        }
    }

}