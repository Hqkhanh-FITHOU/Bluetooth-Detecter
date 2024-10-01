package com.example.bluetoothdetector


import android.bluetooth.BluetoothDevice
import android.widget.Button

interface OnClickToConnectBluetoothDeviceListener {
    fun clickToConnect(button: Button, device:BluetoothDevice);
}