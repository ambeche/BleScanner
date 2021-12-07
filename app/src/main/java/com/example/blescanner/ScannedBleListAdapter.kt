package com.example.blescanner

import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi

class ScannedBleListAdapter(private val context: Context) : BaseAdapter() {
    private var bleList = ArrayList<ScanResult>()
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return bleList.size
    }

    override fun getItem(pos: Int): Any? {
        return bleList[pos]
    }

    override fun getItemId(pos: Int): Long {
        return pos.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(pos: Int, view: View?, parent: ViewGroup?): View {
        val rowView = if (view === null)
            inflater.inflate(R.layout.ble_device_item, parent,false)
        else view

        val tvName = rowView.findViewById<TextView>(R.id.tvName)
        val tvMacAddress = rowView.findViewById<TextView>(R.id.tvAddress)
        val tvSignalStrength =  rowView.findViewById<TextView>(R.id.tvRssi)
        val bleItem = bleList[pos]

        if (!bleItem.isConnectable) {
            updateTextColor(tvMacAddress, tvName, tvSignalStrength)
            rowView.isEnabled = false
        }
        tvName.text = bleItem.device?.name ?: ""
        tvMacAddress.text = bleItem.device?.address
        tvSignalStrength.text = "${bleList[pos].rssi} dBm"

        return rowView
    }

    override fun isEnabled(position: Int): Boolean {
        return true
    }

    fun setAdapter (scanResult:ArrayList<ScanResult>) {
        bleList = scanResult
        notifyDataSetChanged()
    }

    private fun updateTextColor (vararg v: TextView) {
        val tv = listOf(*v)
        tv.forEach { it.setTextColor(context.getColor(R.color.colorGrey)) }
    }
}