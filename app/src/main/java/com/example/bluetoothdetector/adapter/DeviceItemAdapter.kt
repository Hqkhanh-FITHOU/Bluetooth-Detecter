package com.example.bluetoothdetector.adapter

import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothdetector.BluetoothLeService
import com.example.bluetoothdetector.OnClickToConnectBluetoothDeviceListener
import com.example.bluetoothdetector.PermissionChecker
import com.example.bluetoothdetector.R

class DeviceItemAdapter(
    private val list: MutableList<BluetoothDevice>,
    private var onClickToConnectBluetoothDeviceListener: OnClickToConnectBluetoothDeviceListener? = null
) : RecyclerView.Adapter<DeviceItemAdapter.DeviceItemHolder>() {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DeviceItemAdapter.DeviceItemHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item_layout, parent, false)
        return DeviceItemHolder(view)
    }

    fun setOnClickToConnectBluetoothDeviceListener(listener: OnClickToConnectBluetoothDeviceListener) {
        this.onClickToConnectBluetoothDeviceListener = listener
    }

    override fun onBindViewHolder(holder: DeviceItemAdapter.DeviceItemHolder, position: Int) {
        val device = list.elementAt(position)

        PermissionChecker.checkBluetoothConnectionPermission(holder.itemView.context){
            holder.deviceName.text = if (device.name == null)  "Unknown Device" else device.name
        }
        holder.physicalAddress.text = device.address
        holder.buttonConnect.setOnClickListener {
            onClickToConnectBluetoothDeviceListener?.clickToConnect(holder.buttonConnect, device)
        }
    }

    fun addItemToEnd(device: BluetoothDevice){
        if(device !in list){
            list.add(device)
            notifyItemInserted(list.size - 1)
        }
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
        val buttonConnect = itemView.findViewById<Button>(R.id.btn_connect)
    }




}