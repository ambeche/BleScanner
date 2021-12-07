package com.example.blescanner

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.handler.BleWrapper
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BleWrapper.BleCallback {
    private lateinit var bleAdapter: BluetoothAdapter
    private var bleScanning = false
    private lateinit var bLeScanner: BluetoothLeScanner
    private lateinit var bleHandler: Handler
    lateinit var listAdapter: ScannedBleListAdapter
    var scanResult = ArrayList<ScanResult>()
    lateinit var bleWrapper: BleWrapper

    companion object {
        const val SCAN_PERIOD: Long = 3000
        const val REQUEST_CODE_ENABLE = 0
        const val REQUEST_CODE_FINE_LOCATION = 1
        const val CHANNEL_ID = "chart"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()

        val bltManager =  getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bleAdapter = bltManager.adapter
        checkPermissionAndBleStatus()
        btnScan.setOnClickListener{ startBleScan() }
        btnChart.setOnClickListener{
            startActivity( Intent(this, LineChartActivity::class.java))
        }

        listAdapter = ScannedBleListAdapter(this)
        lvBleDevices.adapter = listAdapter
        lvBleDevices.onItemClickListener = OnItemClickListener()
    }

    private fun checkPermissionAndBleStatus(): Boolean {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            Log.d("DBG", "No fine location access")
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_FINE_LOCATION
            )
            return true
        } else if (!bleAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE)
        }
        return true
    }

    private fun startBleScan() {
        Log.d("DBG", "Scan start")
        bLeScanner = bleAdapter.bluetoothLeScanner
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        val filter: List<ScanFilter>? = null
        val bleScanCallback = BleScanCallback()
        if (!bleScanning) {
            bleHandler = Handler(Looper.getMainLooper())
            bleHandler.postDelayed({
                bleScanning = false
                bLeScanner.stopScan(bleScanCallback) }, SCAN_PERIOD)
            bleScanning = true
            bLeScanner.startScan(filter, settings, bleScanCallback)
        }else {
            bleScanning = false
            bLeScanner.stopScan(bleScanCallback)
        }


    }

    inner class BleScanCallback  : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            setBleResults(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            if (results != null) {
                for (result in results) { setBleResults(result)}
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.d ("Scan", errorCode.toString())
        }

        private fun setBleResults(result: ScanResult) {
            val deviceName = result.device
            scanResult.add(result)
            listAdapter.setAdapter(scanResult)
            Log.d("result", deviceName.address)
        }
    }

    inner class OnItemClickListener: AdapterView.OnItemClickListener {
        override fun onItemClick(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
            bleWrapper = BleWrapper(this@MainActivity, scanResult[pos].device.address)
            bleWrapper.also {
                it.addListener(this@MainActivity)
                it.connect(false)
            }
        }

    }

    override fun onDeviceReady(gatt: BluetoothGatt) {
        val hrtUUID = bleWrapper.HEART_RATE_SERVICE_UUID
        for (service in gatt.services) {
            Log.d("services", "${service.uuid}")
            if (service.uuid == hrtUUID) {
                Log.d("HRT_UUID", "is a match")
                for (characteristic in service.characteristics) {
                    Log.d("xtics", "${characteristic.uuid}")
                    bleWrapper.getNotifications(gatt, service.uuid, characteristic.uuid)
                }
                Snackbar.make(
                    myCoordinatorLayout, getString(R.string.snackbar_paired, gatt.device.name),
                    Snackbar.LENGTH_LONG
                ).also {
                    it.setBackgroundTint(getColor(R.color.colorPrimaryDark))
                    it.show()
                }
            }
            // waits 3s after connection is established and then sends push notification
            Handler(Looper.getMainLooper())
                .postDelayed({createNotification()}, 5000)
        }
    }

    override fun onDeviceDisconnected() {
        Toast.makeText(this,
            getString(R.string.toast_disconnect), Toast.LENGTH_SHORT).show()
    }

    override fun onNotify(characteristic: BluetoothGattCharacteristic) {
        val format = BluetoothGattCharacteristic.FORMAT_UINT16
        val hrt = "${characteristic.getIntValue(format, 1) } bpm"
        tvHRT.text = hrt
        Log.d("value", hrt)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = getString(R.string.channel_description)
            }
            ( getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager ).apply {
                createNotificationChannel(channel)
            }
        }
    }

    private fun createNotification()  {
        val notice = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            val intent = Intent(this@MainActivity, LineChartActivity::class.java)
                .apply {  Intent.FLAG_ACTIVITY_CLEAR_TASK  }
            setSmallIcon(R.drawable.ic_baseline_notification_important_24)
            setContentText(getString(R.string.notice_title))
            setContentText(getString(R.string.notice_text))
            priority = NotificationCompat.PRIORITY_DEFAULT
            setContentIntent(
                PendingIntent.getActivity(
                    this@MainActivity, 0, intent, 0 )
            )
            setAutoCancel(true)
        }.build()
        NotificationManagerCompat.from(this).notify(1,notice)
    }
}