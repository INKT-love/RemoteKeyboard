package com.inkt.remotekeyboard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inkt.remotekeyboard.data.model.CustomShortcut
import com.inkt.remotekeyboard.viewmodel.ShortcutsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutsScreen(
    onBack: () -> Unit,
    viewModel: ShortcutsViewModel = hiltViewModel(),
) {
    val shortcuts by viewModel.shortcuts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingShortcut by remember { mutableStateOf<CustomShortcut?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("快捷键管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    "快捷操作栏显示的按钮",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(shortcuts, key = { it.id }) { shortcut ->
                ShortcutItem(
                    shortcut = shortcut,
                    onEdit = { editingShortcut = shortcut },
                    onDelete = { viewModel.removeShortcut(shortcut.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        ShortcutDialog(
            title = "添加快捷键",
            shortcut = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, key, modifiers ->
                viewModel.addShortcut(name, key, modifiers)
                showAddDialog = false
            }
        )
    }

    editingShortcut?.let { shortcut ->
        ShortcutDialog(
            title = "编辑快捷键",
            shortcut = shortcut,
            onDismiss = { editingShortcut = null },
            onSave = { name, key, modifiers ->
                viewModel.updateShortcut(shortcut.copy(name = name, key = key, modifiers = modifiers))
                editingShortcut = null
            }
        )
    }
}

@Composable
private fun ShortcutItem(
    shortcut: CustomShortcut,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    shortcut.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    buildString {
                        if (shortcut.modifiers.isNotEmpty()) {
                            append(shortcut.modifiers.joinToString(" + "))
                            append(" + ")
                        }
                        append(shortcut.key)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutDialog(
    title: String,
    shortcut: CustomShortcut?,
    onDismiss: () -> Unit,
    onSave: (name: String, key: String, modifiers: List<String>) -> Unit,
) {
    var name by remember { mutableStateOf(shortcut?.name ?: "") }
    var key by remember { mutableStateOf(shortcut?.key ?: "") }
    var modifiers by remember { mutableStateOf(shortcut?.modifiers ?: emptyList()) }

    val modifierOptions = listOf("ctrl", "alt", "shift", "win")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    placeholder = { Text("保存") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("按键") },
                    placeholder = { Text("s") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("修饰键", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modifierOptions.forEach { mod ->
                        FilterChip(
                            selected = modifiers.contains(mod),
                            onClick = {
                                modifiers = if (modifiers.contains(mod)) modifiers - mod
                                else modifiers + mod
                            },
                            label = { Text(mod) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, key, modifiers) },
                enabled = name.isNotBlank() && key.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
