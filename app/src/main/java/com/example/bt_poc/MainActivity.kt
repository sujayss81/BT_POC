package com.example.bt_poc

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.bt_poc.ui.theme.BT_POCTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.InputStream
import java.lang.reflect.Method
import java.util.*


class MainActivity : ComponentActivity() {

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var rfidReaderAddress: String? by remember {
                mutableStateOf(null)
            }

            val enableBluetoothContract = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode == Activity.RESULT_OK) {
                    Log.d("bluetoothLauncher", "Success")
                } else {
                    Log.w("bluetoothLauncher", "Failed")
                }
            }

            val btPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) listOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
            ) else
                listOf(
                    android.Manifest.permission.BLUETOOTH,
                    android.Manifest.permission.BLUETOOTH_ADMIN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                )

            val requestBluetoothContract = rememberMultiplePermissionsState(btPermissions)

            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            var inputStream: InputStream? = null

            LaunchedEffect(key1 = true, block = {
                if (requestBluetoothContract.allPermissionsGranted) {
                    val bluetoothManager =
                        getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
                    val mAdapter = bluetoothManager.adapter

                    if (mAdapter?.isEnabled == true) {
                        // Bluetooth is on print the receipt
                        Toast.makeText(
                            context,
                            "Bluetooth success",
                            Toast.LENGTH_SHORT
                        ).show()

                        rfidReaderAddress = mAdapter.bondedDevices.firstOrNull {
                            it.name == "RFID Reader"
                        }?.address

                        if(rfidReaderAddress != null) {
                            val mDevice = mAdapter.getRemoteDevice(rfidReaderAddress)
                            val btSocket = mDevice.createRfcommSocketToServiceRecord(
                                UUID.fromString("81a6e88d-bf69-4523-90c5-019762599330")
                            )
                            val clazz: Class<*> = btSocket.remoteDevice.javaClass
                            val paramTypes = arrayOf<Class<*>>(Integer.TYPE)

                            val m: Method = clazz.getMethod("createRfcommSocket", *paramTypes)
                            val params = arrayOf<Any>(Integer.valueOf(1))

                            val fallbackSocket =
                                m.invoke(btSocket.remoteDevice, params) as BluetoothSocket
                            fallbackSocket.connect()
                            coroutineScope.launch {
                                try {
                                    btSocket.connect()
                                    inputStream = btSocket.inputStream
                                    Toast.makeText(
                                        context,
                                        "Success:Connection to RFID",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (ex: Exception) {
                                    Log.e("$$", "Exception: $ex")
                                    Toast.makeText(
                                        context,
                                        "Failure:Connection to RFID lost",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } else {
                        // Bluetooth is off, ask user to turn it on
                        enableBluetoothContract.launch(enableBluetoothIntent)
                    }
                } else {
                    requestBluetoothContract.launchMultiplePermissionRequest()
                }
            })

            BT_POCTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    TextButton(
                        onClick = {}
                    ) {
                        if (inputStream == null)
                            Text(text = "check bt")
                        else
                            Text(text = "reader connected: $rfidReaderAddress")
                    }
                }
            }
        }
    }
}

data class BluetoothDevice(
    val name: String,
    val address: String
)

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BT_POCTheme {
        Greeting("Android")
    }
}