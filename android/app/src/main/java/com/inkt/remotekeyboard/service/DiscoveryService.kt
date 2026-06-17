package com.inkt.remotekeyboard.service

import android.util.Log
import com.inkt.remotekeyboard.data.model.Device
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscoveryService @Inject constructor() {

    private val serverPort = 8765
    private val discoveryPort = 8767
    private val discoveryMagic = "REMOTE_KEYBOARD_DISCOVER"
    private var discoveryJob: Job? = null

    fun startDiscovery(onDeviceFound: (Device) -> Unit): Job {
        discoveryJob?.cancel()
        discoveryJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取本机 IP 段
                val localIp = getLocalIpAddress()
                if (localIp == null) {
                    Log.e("Discovery", "无法获取本机 IP")
                    return@launch
                }

                val subnet = localIp.substringBeforeLast(".")
                Log.d("Discovery", "本机 IP: $localIp, 扫描网段: $subnet.0/24")

                // 先尝试 UDP 广播发现
                tryUdpDiscovery(onDeviceFound)

                // 同时扫描常见 IP（最后 20 个）
                val jobs = mutableListOf<Job>()
                for (i in 1..254) {
                    val ip = "$subnet.$i"
                    if (ip == localIp) continue

                    val job = launch {
                        tryConnect(ip, serverPort, 500)?.let { device ->
                            withContext(Dispatchers.Main) {
                                onDeviceFound(device)
                            }
                        }
                    }
                    jobs.add(job)

                    // 限制并发数
                    if (jobs.size >= 50) {
                        jobs.forEach { it.join() }
                        jobs.clear()
                    }
                }
                jobs.forEach { it.join() }

            } catch (e: Exception) {
                Log.e("Discovery", "发现服务异常: ${e.message}", e)
            }
        }
        return discoveryJob!!
    }

    private suspend fun tryUdpDiscovery(onDeviceFound: (Device) -> Unit) {
        try {
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 3000

            val data = discoveryMagic.toByteArray()
            val packet = DatagramPacket(
                data, data.size,
                InetAddress.getByName("255.255.255.255"), discoveryPort
            )
            socket.send(packet)
            Log.d("Discovery", "UDP 广播已发送")

            val buffer = ByteArray(1024)
            try {
                val respPacket = DatagramPacket(buffer, buffer.size)
                socket.receive(respPacket)
                val message = String(respPacket.data, 0, respPacket.length)
                Log.d("Discovery", "UDP 收到响应: $message")

                if (message.startsWith("REMOTE_KEYBOARD_SERVER:")) {
                    val parts = message.split(":")
                    if (parts.size >= 4) {
                        val device = Device(
                            id = respPacket.address.hostAddress ?: "",
                            name = parts[2],
                            host = respPacket.address.hostAddress ?: "",
                            port = parts[3].toIntOrNull() ?: serverPort,
                            platform = parts.getOrElse(4) { "" }
                        )
                        withContext(Dispatchers.Main) {
                            onDeviceFound(device)
                        }
                    }
                }
            } catch (_: Exception) {
                Log.d("Discovery", "UDP 未收到响应")
            }
            socket.close()
        } catch (e: Exception) {
            Log.e("Discovery", "UDP 发现异常: ${e.message}")
        }
    }

    private fun tryConnect(ip: String, port: Int, timeoutMs: Int): Device? {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            socket.close()
            Device(
                id = ip,
                name = ip,
                host = ip,
                port = port,
                platform = ""
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }
}
