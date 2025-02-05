package react.bridge

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableMap
import react.bridge.ble.module.BleModule

class MainActivity : AppCompatActivity() {

    private val bleModule by lazy { BleModule(this) }

    private val promise by lazy { object : Promise{
        override fun reject(code: String, message: String?) {
        }

        override fun reject(code: String, throwable: Throwable?) {
        }

        override fun reject(code: String, message: String?, throwable: Throwable?) {
        }

        override fun reject(throwable: Throwable) {
            throwable.printStackTrace()
        }

        override fun reject(throwable: Throwable, userInfo: WritableMap) {
        }

        override fun reject(code: String, userInfo: WritableMap) {
        }

        override fun reject(code: String, throwable: Throwable?, userInfo: WritableMap) {
        }

        override fun reject(code: String, message: String?, userInfo: WritableMap) {
        }

        override fun reject(
            code: String?,
            message: String?,
            throwable: Throwable?,
            userInfo: WritableMap?
        ) {
        }

        override fun reject(message: String) {

        }

        override fun resolve(value: Any?) {
            println(value)
        }

    } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBluetooth()
        bleModule.scanBleListDevice(promise)
    }

    fun requestBluetooth() {
        // check android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestEnableBluetooth.launch(enableBtIntent)
        }
    }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // granted

                bleModule.scanBleListDevice(promise)
            } else {
                // denied
            }
        }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("MyTag", "${it.key} = ${it.value}")
            }
        }
}