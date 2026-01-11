package com.metalens.app.wearables

import com.meta.wearable.dat.core.types.DeviceIdentifier
import com.meta.wearable.dat.core.types.RegistrationState

data class WearablesUiState(
    val registrationState: RegistrationState = RegistrationState.Unavailable(),
    val devices: List<DeviceIdentifier> = emptyList(),
    /**
     * Currently active (connected) device selected by the DeviceSelector, if any.
     */
    val activeDevice: DeviceIdentifier? = null,
    /**
     * Best-effort friendly names loaded from Wearables.devicesMetadata.
     * Keyed by deviceId.toString().
     */
    val deviceDisplayNames: Map<String, String> = emptyMap(),
    val hasActiveDevice: Boolean = false,
    val recentError: String? = null,
) {
    val isRegistered: Boolean = registrationState is RegistrationState.Registered

    val connectedDevices: List<DeviceIdentifier> =
        activeDevice?.let { listOf(it) } ?: emptyList()
}

