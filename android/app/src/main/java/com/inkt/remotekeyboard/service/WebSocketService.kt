package com.inkt.remotekeyboard.service

import android.util.Log
import com.google.gson.Gson
import com.inkt.remotekeyboard.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketService @Inject constructor() {

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _serverStatus = MutableStateFlow(ServerStatus())
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    // 鼠标移动缓冲
    private var mouseX = 0
    private var mouseY = 0
    private val mouseScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mouseFlushJob: Job? = null

    // 自动重连
    private var lastConfig: ConnectionConfig? = null
    private var reconnectJob: Job? = null
    private var reconnectCount = 0
    private val maxReconnect = 5
    private var userDisconnected = false

    // 延迟测量
    private var pingSentTime = 0L
    private var latencyJob: Job? = null

    init {
        mouseFlushJob = mouseScope.launch {
            while (isActive) {
                delay(8)
                flushMouseMove()
            }
        }
        // 每 2 秒 ping 测延迟
        latencyJob = mouseScope.launch {
            while (isActive) {
                delay(2000)
                if (isConnected()) {
                    pingSentTime = System.currentTimeMillis()
                    send(gson.toJson(mapOf("type" to "ping")))
                }
            }
        }
    }

    fun connect(config: ConnectionConfig) {
        lastConfig = config
        userDisconnected = false
        reconnectCount = 0
        doConnect(config)
    }

    private fun doConnect(config: ConnectionConfig) {
        _connectionState.value = ConnectionState.Connecting

        val url = if (config.mode == ConnectionMode.RELAY) {
            "ws://${config.relayHost}:${config.relayPort}/connect/${config.deviceId}"
        } else {
            "ws://${config.host}:${config.port}"
        }

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectCount = 0
                _connectionState.value = ConnectionState.Connected("")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, Map::class.java)
                    when (msg["type"]) {
                        "status" -> {
                            val platform = msg["platform"] as? String ?: ""
                            _serverStatus.value = ServerStatus(true, platform)
                            _connectionState.value = ConnectionState.Connected(platform)
                        }
                        "pong" -> {
                            val latency = System.currentTimeMillis() - pingSentTime
                            _serverStatus.value = _serverStatus.value.copy(latencyMs = latency)
                        }
                    }
                } catch (_: Exception) {}
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "连接失败: ${t.message}")
                _connectionState.value = ConnectionState.Error(t.message ?: "连接失败")
                tryReconnect()
            }
        })
    }

    private fun tryReconnect() {
        if (userDisconnected) return
        if (reconnectCount >= maxReconnect) {
            Log.d("WebSocket", "重连次数用尽")
            return
        }

        reconnectJob?.cancel()
        reconnectJob = mouseScope.launch {
            val delayMs = minOf(1000L * (reconnectCount + 1), 10000L)
            Log.d("WebSocket", "第 ${reconnectCount + 1} 次重连，等待 ${delayMs}ms")
            delay(delayMs)
            reconnectCount++
            lastConfig?.let { doConnect(it) }
        }
    }

    fun disconnect() {
        userDisconnected = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        mouseX = 0
        mouseY = 0
        lastConfig = null
        reconnectCount = 0
        _connectionState.value = ConnectionState.Disconnected
        _serverStatus.value = ServerStatus()
    }

    fun sendKey(key: String, modifiers: List<String> = emptyList(), action: String = "tap") {
        val event = KeyAction(type = "key", action = action, key = key, modifiers = modifiers)
        send(gson.toJson(event))
        if (action == "tap") {
            val display = if (modifiers.isNotEmpty()) "${modifiers.joinToString("+")}+$key" else key
            _serverStatus.value = _serverStatus.value.copy(lastKey = display)
        }
    }

    fun sendMouseMove(x: Int, y: Int) {
        synchronized(this) {
            mouseX += x
            mouseY += y
        }
    }

    private fun flushMouseMove() {
        val x: Int
        val y: Int
        synchronized(this) {
            x = mouseX
            y = mouseY
            mouseX = 0
            mouseY = 0
        }
        if (x != 0 || y != 0) {
            val event = KeyAction(type = "mouse", action = "move", x = x, y = y)
            send(gson.toJson(event))
        }
    }

    fun sendMouseMoveDirect(x: Int, y: Int) {
        val event = KeyAction(type = "mouse", action = "move", x = x, y = y)
        send(gson.toJson(event))
    }

    fun sendMouseClick(button: String = "left", action: String = "tap") {
        val event = KeyAction(type = "mouse", action = action, button = button)
        send(gson.toJson(event))
    }

    fun sendMouseScroll(delta: Int) {
        val event = KeyAction(type = "mouse", action = "scroll", delta = delta)
        send(gson.toJson(event))
    }

    fun sendText(text: String) {
        val event = KeyAction(type = "key", action = "type", text = text)
        send(gson.toJson(event))
    }

    private fun send(json: String) {
        webSocket?.send(json)
    }

    fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }
}
