package com.metalens.app.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metalens.app.R
import com.metalens.app.ui.components.FeatureActionCard
import com.metalens.app.wearables.WearablesViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current as ComponentActivity
    val wearablesViewModel: WearablesViewModel = viewModel(activity)
    val uiState by wearablesViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        wearablesViewModel.startMonitoring()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 20.dp, vertical = 24.dp)),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text =
                if (uiState.isRegistered) {
                    stringResource(R.string.glasses_status_connected)
                } else {
                    stringResource(R.string.glasses_status_not_connected)
                },
            style = MaterialTheme.typography.bodyMedium,
        )

        uiState.recentError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        FeatureActionCard(
            title =
                if (uiState.isRegistered) {
                    stringResource(R.string.disconnect_my_glasses)
                } else {
                    stringResource(R.string.connect_my_glasses)
                },
            icon = if (uiState.isRegistered) Icons.Filled.BluetoothDisabled else Icons.Filled.Bluetooth,
            onClick = {
                if (uiState.isRegistered) {
                    wearablesViewModel.startUnregistration()
                } else {
                    wearablesViewModel.startRegistration()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.devices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.glasses_discovered_devices_count, uiState.devices.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            uiState.devices.forEach { deviceId ->
                val id = deviceId.toString()
                val name = uiState.deviceDisplayNames[id] ?: id
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen()
}

