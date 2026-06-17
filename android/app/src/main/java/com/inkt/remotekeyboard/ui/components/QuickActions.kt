package com.inkt.remotekeyboard.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkt.remotekeyboard.data.model.CustomShortcut
import com.inkt.remotekeyboard.viewmodel.KeyboardViewModel

@Composable
fun QuickActionsBar(
    viewModel: KeyboardViewModel,
    shortcuts: List<CustomShortcut> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val displayShortcuts = if (shortcuts.isEmpty()) viewModel.defaultShortcuts else shortcuts

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF2A2A2A)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 剪贴板粘贴按钮
            QuickAction(
                name = "📋 粘贴",
                onClick = { viewModel.pasteClipboard() }
            )
            displayShortcuts.forEach { shortcut ->
                QuickAction(
                    name = shortcut.name,
                    onClick = { viewModel.sendShortcut(shortcut) }
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    name: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF3A3A3A),
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.labelSmall, color = Color(0xFFAAAAAA), fontSize = 11.sp)
        }
    }
}
