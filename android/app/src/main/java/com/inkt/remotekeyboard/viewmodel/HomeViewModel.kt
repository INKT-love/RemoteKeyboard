package com.inkt.remotekeyboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkt.remotekeyboard.data.model.*
import com.inkt.remotekeyboard.data.repository.DeviceRepository
import com.inkt.remotekeyboard.data.repository.SettingsRepository
import com.inkt.remotekeyboard.service.DiscoveryService
import com.inkt.remotekeyboard.service.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val webSocketService: WebSocketService,
    private val discoveryService: DiscoveryService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val devices = deviceRepository.devices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionState = webSocketService.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionState.Disconnected)

    private val _discoveredDevices = MutableStateFlow<List<Device>>(emptyList())
    val discoveredDevices: StateFlow<List<Device>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private var autoConnectAttempted = false

    init {
        // 自动连接
        viewModelScope.launch {
            settingsRepository.autoConnect.combine(deviceRepository.devices) { enabled, devices ->
                Pair(enabled, devices)
            }.collect { (enabled, devices) ->
                if (enabled && !autoConnectAttempted && devices.isNotEmpty()) {
                    autoConnectAttempted = true
                    val lastDevice = devices.maxByOrNull { it.lastConnected }
                    if (lastDevice != null && webSocketService.connectionState.value is ConnectionState.Disconnected) {
                        connect(lastDevice)
                    }
                }
            }
        }
    }

    fun connect(device: Device) {
        val config = ConnectionConfig(
            host = device.host,
            port = device.port,
            mode = if (device.isRelay) ConnectionMode.RELAY else ConnectionMode.DIRECT
        )
        webSocketService.connect(config)

        viewModelScope.launch {
            deviceRepository.saveDevice(device.copy(lastConnected = System.currentTimeMillis()))
        }
    }

    fun connectManual(host: String, port: Int) {
        val device = Device(
            id = "$host:$port",
            name = host,
            host = host,
            port = port
        )
        connect(device)
    }

    fun disconnect() {
        webSocketService.disconnect()
    }

    fun removeDevice(device: Device) {
        viewModelScope.launch {
            deviceRepository.removeDevice(device)
        }
    }

    fun startDiscovery() {
        _isDiscovering.value = true
        _discoveredDevices.value = emptyList()
        discoveryService.startDiscovery { device ->
            val current = _discoveredDevices.value.toMutableList()
            if (current.none { it.host == device.host }) {
                current.add(device)
                _discoveredDevices.value = current
            }
        }
    }

    fun stopDiscovery() {
        discoveryService.stopDiscovery()
        _isDiscovering.value = false
    }

    override fun onCleared() {
        super.onCleared()
        discoveryService.stopDiscovery()
    }
}
