package react.bridge.ble.module

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class BleModule(
    private val reactContext: Context
) : ReactContextBaseJavaModule() {

    private val bluetoothAdapter: BluetoothAdapter?
        get() = (reactContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val bluetoothGattCallback by lazy {
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState){
                    BluetoothProfile.STATE_CONNECTED -> connectDevicesPromise?.resolve(true)
                    BluetoothProfile.STATE_DISCONNECTED -> connectDevicesPromise?.reject(Exception("Failed to connect to Bluetooth Device"))
                }
            }
        }
    }

    private val listDeviceBle by lazy { mutableListOf<ScanResult>() }

    private var connectDevicesPromise: Promise? = null

    // Device scan callback.
    private val bleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            listDeviceBle.add(result)
        }
    }

    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner
    private var scanning = false

    // Stops scanning after 10 seconds.
    private val scanPeriod: Long = 10000

    /**
     * This method to fetch the scan list of BLE Devices and will return List of Devices.
     * @param promise Promise. To return callback for list of BLE Devices.
     */
    @ReactMethod
    fun scanBleListDevice(promise: Promise) {
        runCatching {
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                scanning = true
                bleScanner?.startScan(bleScanCallback)
                CoroutineScope(Dispatchers.IO).launch {
                    delay(scanPeriod)
                    scanning = false
                    bleScanner?.stopScan(bleScanCallback)
                    promise.resolve(listDevicesToWriteableMap())
                }
            } else {
                scanning = false
                bleScanner?.stopScan(bleScanCallback)
            }
        }.onFailure { promise.reject(Exception("Please enable bluetooth permission first")) }
    }


    /**
     * This method to connect BLE Devices
     * @param address String. Identify for which bluetooth le device will be connected.
     * @param promise Promise. To return callback for list of BLE Devices.
     */
    @ReactMethod
    fun connect(address: String, promise: Promise) {
        val deviceBle = listDeviceBle.firstOrNull { it.device.address == address }
        deviceBle?.let {
            deviceBle.device.connectGatt(reactContext, false, bluetoothGattCallback)
        } ?: promise.reject(Exception("Device not found"))
    }

    /**
     * This method to disconnect BLE Devices
     * @param address String. Identify for which bluetooth le device will be connected.
     * @param promise Promise. To return callback for list of BLE Devices.
     */
    @ReactMethod
    fun disconnect(address: String, promise: Promise) {
        val deviceBle = bluetoothAdapter?.bondedDevices?.find { it.address == address }
        deviceBle?.let {
            runCatching { deviceBle.removeBond() }
                .onSuccess { promise.resolve(true) }
                .onFailure { promise.reject(it) }
        } ?: promise.reject(Exception("Device not found"))
    }

    @Throws(Exception::class)
    fun BluetoothDevice.removeBond() {
        javaClass.getMethod("removeBond").invoke(this)
    }

    private fun listDevicesToWriteableMap() : WritableArray {
        val deviceList = Arguments.createArray()
        listDeviceBle
            .forEach {
                val params = Arguments.createMap()
                params.apply {
                    it.device.also{ device ->
                        putString("name", device.name)
                        putString("address", device.address)
                        device.bluetoothClass?.let {
                            putInt("class", it.deviceClass)
                        }
                    }

                }
                deviceList.pushMap(params)
            }
        return deviceList
    }

    companion object{
        const val NAME = "BleModule"
    }

    override fun getName(): String = NAME
}