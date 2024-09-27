package com.example.bluetoothdetector.adapter

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdetector.PermissionChecker
import com.example.bluetoothdetector.R

class DeviceItemAdapter(
    private val list: Set<BluetoothDevice>
) : RecyclerView.Adapter<DeviceItemAdapter.DeviceItemHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeviceItemAdapter.DeviceItemHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item_layout, parent, false)
        return DeviceItemHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceItemAdapter.DeviceItemHolder, position: Int) {
        val device = list.elementAt(position)

        PermissionChecker.checkBluetoothConnectionPermission(holder.itemView.context){
            holder.deviceName.text = device.name
        }
        holder.physicalAddress.text = device.address
    }

    override fun getItemCount(): Int {
        if(list.isEmpty()){
            return 0
        }
        return list.size
    }

    class DeviceItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName = itemView.findViewById<TextView>(R.id.diviceName)
        val physicalAddress = itemView.findViewById<TextView>(R.id.physicalAddress)
        val diviceItemLayout = itemView.findViewById<ViewGroup>(R.id.diviceItemLayout)
    }




}