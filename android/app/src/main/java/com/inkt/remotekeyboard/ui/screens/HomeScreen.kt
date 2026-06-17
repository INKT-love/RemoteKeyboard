package com.inkt.remotekeyboard.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inkt.remotekeyboard.data.model.*
import com.inkt.remotekeyboard.ui.components.DeviceCard
import com.inkt.remotekeyboard.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToKeyboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val devices by viewModel.devices.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isDiscovering by viewModel.isDiscovering.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDiscoverySheet by remember { mutableStateOf(false) }

    // Auto-navigate to keyboard when connected
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected) {
            onNavigateToKeyboard()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Remote Keyboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Discovery button
                FloatingActionButton(
                    onClick = {
                        showDiscoverySheet = true
                        viewModel.startDiscovery()
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.Search, contentDescription = "扫描设备")
                }
                // Add button
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "手动添加")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Connection status card
            item {
                ConnectionStatusCard(connectionState)
            }

            // Saved devices
            if (devices.isNotEmpty()) {
                item {
                    Text(
                        "已保存的设备",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        connectionState = if (connectionState is ConnectionState.Connected) connectionState else ConnectionState.Disconnected,
                        onConnect = {
                            if (connectionState is ConnectionState.Connected) {
                                viewModel.disconnect()
                            } else {
                                viewModel.connect(device)
                            }
                        },
                        onRemove = { viewModel.removeDevice(device) },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            // Empty state
            if (devices.isEmpty()) {
                item {
                    EmptyState(onAdd = { showAddDialog = true })
                }
            }
        }
    }

    // Add device dialog
    if (showAddDialog) {
        AddDeviceDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { host, port ->
                viewModel.connectManual(host, port)
                showAddDialog = false
            }
        )
    }

    // Discovery sheet
    if (showDiscoverySheet) {
        DiscoverySheet(
            devices = discoveredDevices,
            isDiscovering = isDiscovering,
            onRefresh = { viewModel.startDiscovery() },
            onConnect = { device ->
                viewModel.connect(device)
                showDiscoverySheet = false
            },
            onDismiss = {
                showDiscoverySheet = false
                viewModel.stopDiscovery()
            }
        )
    }
}

@Composable
private fun ConnectionStatusCard(state: ConnectionState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
                is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
                is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                is ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (state) {
                    is ConnectionState.Connected -> Icons.Default.CheckCircle
                    is ConnectionState.Connecting -> Icons.Default.Sync
                    is ConnectionState.Error -> Icons.Default.Error
                    is ConnectionState.Disconnected -> Icons.Default.WifiOff
                },
                contentDescription = null,
                tint = when (state) {
                    is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                    is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
                    is ConnectionState.Error -> MaterialTheme.colorScheme.error
                    is ConnectionState.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    when (state) {
                        is ConnectionState.Connected -> "已连接"
                        is ConnectionState.Connecting -> "连接中..."
                        is ConnectionState.Error -> "连接失败"
                        is ConnectionState.Disconnected -> "未连接"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                when (state) {
                    is ConnectionState.Connected -> {
                        Text(
                            "平台: ${state.platform.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is ConnectionState.Error -> {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Keyboard,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "开始使用",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 使用步骤
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                StepItem("1", "电脑启动服务", "运行 RemoteKeyboard-Server.exe")
                Spacer(modifier = Modifier.height(12.dp))
                StepItem("2", "手机连接", "点击右下角 + 输入电脑 IP")
                Spacer(modifier = Modifier.height(12.dp))
                StepItem("3", "开始打字", "在任意输入框使用远程键盘")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加设备")
        }
    }
}

@Composable
private fun StepItem(step: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    step,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddDeviceDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8765") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加设备") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("IP 地址") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("端口") },
                    placeholder = { Text("8765") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(host, port.toIntOrNull() ?: 8765) },
                enabled = host.isNotBlank()
            ) {
                Text("连接")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverySheet(
    devices: List<Device>,
    isDiscovering: Boolean,
    onRefresh: () -> Unit,
    onConnect: (Device) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "发现的设备",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRefresh) {
                    if (isDiscovering) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDiscovering) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("正在扫描局域网设备...")
                        }
                    } else {
                        Text(
                            "未发现设备，请确保电脑端服务已启动",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                devices.forEach { device ->
                    Card(
                        onClick = { onConnect(device) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Computer, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "${device.host}:${device.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
