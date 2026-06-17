package com.inkt.remotekeyboard.ui.screens

import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inkt.remotekeyboard.data.model.ConnectionState
import com.inkt.remotekeyboard.ui.components.QuickActionsBar
import com.inkt.remotekeyboard.ui.components.KeyboardView
import com.inkt.remotekeyboard.viewmodel.KeyboardViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardScreen(
    onBack: () -> Unit,
    viewModel: KeyboardViewModel = hiltViewModel(),
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val shortcuts by viewModel.shortcuts.collectAsState()
    var showFunctionKeys by remember { mutableStateOf(false) }
    var showTouchpad by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val context = LocalContext.current

    val isConnected = connectionState is ConnectionState.Connected
    LaunchedEffect(isConnected) {
        val activity = context as? android.app.Activity
        if (isConnected) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    androidx.activity.compose.BackHandler {
        viewModel.disconnect()
        onBack()
    }

    Scaffold(
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("键盘", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            ConnectionChip(connectionState, serverStatus.latencyMs, serverStatus.lastKey)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.disconnect()
                            onBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFunctionKeys = !showFunctionKeys }) {
                            Icon(
                                Icons.Default.Functions,
                                contentDescription = "功能键",
                                tint = if (showFunctionKeys) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showTouchpad = !showTouchpad }) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = "触控板",
                                tint = if (showTouchpad) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            viewModel.disconnect()
                            onBack()
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        ConnectionChip(connectionState)
                    }
                    Row {
                        IconButton(onClick = { showFunctionKeys = !showFunctionKeys }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Functions, "F键", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { showTouchpad = !showTouchpad }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.TouchApp, "触控板", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showTouchpad) {
                TouchpadArea(viewModel)
            }

            AnimatedVisibility(visible = showFunctionKeys) {
                FunctionKeyRow(viewModel)
            }

            QuickActionsBar(viewModel, shortcuts)

            KeyboardView(
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TouchpadArea(viewModel: KeyboardViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // 单指手势处理
                    coroutineScope {
                        launch {
                            awaitPointerEventScope {
                                while (true) {
                                    // 等待第一个手指按下
                                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                                    val startTime = System.currentTimeMillis()
                                    var lastPos = firstDown.position
                                    var dragged = false

                                    // 监控是否有移动
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull()
                                        if (change == null || !change.pressed) {
                                            // 手指抬起
                                            if (!dragged && System.currentTimeMillis() - startTime < 300) {
                                                // 短按 = 左键点击
                                                viewModel.sendMouseClick("left")
                                            }
                                            change?.consume()
                                            break
                                        }
                                        val delta = change.position - lastPos
                                        if (delta.getDistance() > 2f) {
                                            dragged = true
                                            viewModel.sendMouseMove(delta.x.toInt(), delta.y.toInt())
                                        }
                                        lastPos = change.position
                                        change.consume()
                                    }
                                }
                            }
                        }
                        // 两指手势处理
                        launch {
                            awaitPointerEventScope {
                                while (true) {
                                    // 等待两指同时按下
                                    val event = awaitPointerEvent()
                                    if (event.changes.size >= 2) {
                                        val pointers = event.changes.take(2)
                                        val startTime = System.currentTimeMillis()
                                        var lastMid = Offset(
                                            (pointers[0].position.x + pointers[1].position.x) / 2,
                                            (pointers[0].position.y + pointers[1].position.y) / 2
                                        )

                                        // 监控两指
                                        while (true) {
                                            val e = awaitPointerEvent()
                                            val active = e.changes.filter { it.pressed }

                                            if (active.size < 2) {
                                                // 两指都抬起
                                                if (System.currentTimeMillis() - startTime < 500) {
                                                    // 两指短按 = 右键
                                                    viewModel.sendMouseClick("right")
                                                }
                                                break
                                            }

                                            if (active.size == 2) {
                                                val mid = Offset(
                                                    (active[0].position.x + active[1].position.x) / 2,
                                                    (active[0].position.y + active[1].position.y) / 2
                                                )
                                                val delta = mid - lastMid
                                                if (delta.getDistance() > 2f) {
                                                    viewModel.sendMouseMove(delta.x.toInt(), delta.y.toInt())
                                                }
                                                lastMid = mid
                                            }
                                            e.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "单指滑动 · 单指点击左键 · 两指点击右键",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun FunctionKeyRow(viewModel: KeyboardViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        (1..12).forEach { n ->
            com.inkt.remotekeyboard.ui.components.KeyButton(
                label = "F$n",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.sendKey("f$n") }
            )
        }
    }
}

@Composable
private fun ConnectionChip(state: ConnectionState, latencyMs: Long = 0, lastKey: String = "") {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when (state) {
            is ConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
            is ConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.errorContainer
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                when (state) {
                    is ConnectionState.Connected -> {
                        if (latencyMs > 0) "● ${latencyMs}ms"
                        else "● 已连接"
                    }
                    is ConnectionState.Connecting -> "● 连接中"
                    is ConnectionState.Error -> "● 断开"
                    is ConnectionState.Disconnected -> "● 断开"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            if (lastKey.isNotEmpty() && state is ConnectionState.Connected) {
                Text(
                    lastKey,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}
