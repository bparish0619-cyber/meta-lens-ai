package com.metalens.app

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.INTERNET
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.meta.wearable.dat.core.Wearables
import com.metalens.app.ui.navigation.MetaLensApp
import com.metalens.app.wearables.WearablesViewModel

class MainActivity : ComponentActivity() {
    companion object {
        val PERMISSIONS: Array<String> = arrayOf(BLUETOOTH, BLUETOOTH_CONNECT, INTERNET)
    }

    private val wearablesViewModel: WearablesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions {
            // Must be called before using any Wearables APIs
            Wearables.initialize(this)
            wearablesViewModel.startMonitoring()
        }

        setContent {
            MaterialTheme {
                MetaLensApp()
            }
        }
    }

    private fun checkPermissions(onPermissionsGranted: () -> Unit) {
        registerForActivityResult(RequestMultiplePermissions()) { permissionsResult ->
            val granted = permissionsResult.entries.all { it.value }
            if (granted) {
                onPermissionsGranted()
            } else {
                wearablesViewModel.setRecentError(
                    "Allow All Permissions (Bluetooth, Bluetooth Connect, Internet)"
                )
            }
        }.launch(PERMISSIONS)
    }
}

