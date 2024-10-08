package com.example.bluetoothdetector

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionChecker {

    companion object {
        const val REQUEST_BLUETOOTH_PERMISSIONS:Int = 124

        fun checkBluetoothConnectionPermission(context: Context, onPermissionGranted: () -> Unit) {
            val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                 arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
            }else {
                arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if(permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED  }){
                onPermissionGranted()
            }else{
                ActivityCompat.requestPermissions(context as Activity, permissions, REQUEST_BLUETOOTH_PERMISSIONS)
            }
        }
    }



}