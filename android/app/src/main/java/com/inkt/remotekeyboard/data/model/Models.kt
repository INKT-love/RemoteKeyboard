package com.inkt.remotekeyboard.data.model

data class Device(
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: Int = 8765,
    val platform: String = "", // "windows" or "linux"
    val isRelay: Boolean = false,
    val lastConnected: Long = 0L,
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, MIUIX_LIGHT, MIUIX_DARK, MONET
}

enum class ConnectionMode {
    DIRECT, RELAY
}

data class ConnectionConfig(
    val host: String,
    val port: Int = 8765,
    val mode: ConnectionMode = ConnectionMode.DIRECT,
    val relayHost: String = "",
    val relayPort: Int = 8766,
    val deviceId: String = "",
)

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val platform: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class KeyAction(
    val type: String = "key", // "key", "mouse"
    val action: String = "tap", // "press", "release", "tap"
    val key: String = "",
    val modifiers: List<String> = emptyList(),
    val x: Int = 0,
    val y: Int = 0,
    val button: String = "left",
    val delta: Int = 0,
    val text: String = "",
)

data class ServerStatus(
    val connected: Boolean = false,
    val platform: String = "",
    val latencyMs: Long = 0,
    val lastKey: String = "",
)

data class CustomShortcut(
    val id: String = "",
    val name: String = "",
    val key: String = "",
    val modifiers: List<String> = emptyList(),
)
