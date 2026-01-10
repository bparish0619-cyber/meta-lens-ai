package com.metalens.app.wearables

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.AutoDeviceSelector
import com.meta.wearable.dat.core.selectors.DeviceSelector
import com.meta.wearable.dat.core.types.DeviceCompatibility
import com.meta.wearable.dat.core.types.DeviceIdentifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WearablesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WearablesUiState())
    val uiState: StateFlow<WearablesUiState> = _uiState.asStateFlow()

    val deviceSelector: DeviceSelector = AutoDeviceSelector()
    private var deviceSelectorJob: Job? = null
    private var monitoringStarted = false
    private val deviceMetadataJobs = mutableMapOf<DeviceIdentifier, Job>()

    fun startMonitoring() {
        if (monitoringStarted) return
        monitoringStarted = true

        deviceSelectorJob =
            viewModelScope.launch {
                deviceSelector.activeDevice(Wearables.devices).collect { device ->
                    _uiState.update { it.copy(hasActiveDevice = device != null) }
                }
            }

        viewModelScope.launch {
            Wearables.registrationState.collect { state ->
                _uiState.update { it.copy(registrationState = state) }
            }
        }

        viewModelScope.launch {
            Wearables.devices.collect { devices ->
                _uiState.update { it.copy(devices = devices.toList()) }
                monitorDeviceMetadata(devices)
            }
        }
    }

    private fun monitorDeviceMetadata(devices: Set<DeviceIdentifier>) {
        // cancel removed
        val removed = deviceMetadataJobs.keys - devices
        removed.forEach { id ->
            deviceMetadataJobs[id]?.cancel()
            deviceMetadataJobs.remove(id)
            _uiState.update { state ->
                state.copy(deviceDisplayNames = state.deviceDisplayNames - id.toString())
            }
        }

        // start for new
        val newDevices = devices - deviceMetadataJobs.keys
        newDevices.forEach { deviceId ->
            val job =
                viewModelScope.launch {
                    Wearables.devicesMetadata[deviceId]?.collect { metadata ->
                        val idKey = deviceId.toString()
                        val name = metadata.name.ifEmpty { idKey }
                        _uiState.update { state ->
                            state.copy(deviceDisplayNames = state.deviceDisplayNames + (idKey to name))
                        }

                        if (metadata.compatibility == DeviceCompatibility.DEVICE_UPDATE_REQUIRED) {
                            setRecentError("Device '$name' requires an update to work with this app")
                        }
                    }
                }
            deviceMetadataJobs[deviceId] = job
        }
    }

    fun startRegistration() {
        Wearables.startRegistration(getApplication())
    }

    fun startUnregistration() {
        Wearables.startUnregistration(getApplication())
    }

    fun setRecentError(error: String?) {
        _uiState.update { it.copy(recentError = error) }
    }

    override fun onCleared() {
        super.onCleared()
        deviceSelectorJob?.cancel()
        deviceMetadataJobs.values.forEach { it.cancel() }
        deviceMetadataJobs.clear()
    }
}

