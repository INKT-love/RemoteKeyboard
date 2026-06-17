package com.inkt.remotekeyboard.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inkt.remotekeyboard.data.model.ConnectionState
import com.inkt.remotekeyboard.data.model.CustomShortcut
import com.inkt.remotekeyboard.data.model.ServerStatus
import com.inkt.remotekeyboard.data.repository.ShortcutRepository
import com.inkt.remotekeyboard.service.WebSocketService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class KeyboardViewModel @Inject constructor(
    application: Application,
    private val webSocketService: WebSocketService,
    private val shortcutRepository: ShortcutRepository
) : AndroidViewModel(application) {

    val connectionState: StateFlow<ConnectionState> = webSocketService.connectionState
    val serverStatus: StateFlow<ServerStatus> = webSocketService.serverStatus

    val shortcuts: StateFlow<List<CustomShortcut>> = shortcutRepository.shortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultShortcuts = listOf(
        CustomShortcut("1", "复制", "c", listOf("ctrl")),
        CustomShortcut("2", "粘贴", "v", listOf("ctrl")),
        CustomShortcut("3", "撤销", "z", listOf("ctrl")),
        CustomShortcut("4", "重做", "z", listOf("ctrl", "shift")),
        CustomShortcut("5", "全选", "a", listOf("ctrl")),
        CustomShortcut("6", "保存", "s", listOf("ctrl")),
        CustomShortcut("7", "查找", "f", listOf("ctrl")),
        CustomShortcut("8", "关闭", "w", listOf("ctrl")),
        CustomShortcut("9", "切换", "tab", listOf("alt")),
        CustomShortcut("10", "锁定", "l", listOf("win")),
    )

    fun sendKey(key: String, modifiers: List<String> = emptyList()) {
        webSocketService.sendKey(key, modifiers)
    }

    fun sendShortcut(shortcut: CustomShortcut) {
        webSocketService.sendKey(shortcut.key, shortcut.modifiers)
    }

    fun sendKeyDown(key: String) {
        webSocketService.sendKey(key, action = "press")
    }

    fun sendKeyUp(key: String) {
        webSocketService.sendKey(key, action = "release")
    }

    fun sendModifierDown(modifier: String) {
        webSocketService.sendKey(modifier, action = "press")
    }

    fun sendModifierUp(modifier: String) {
        webSocketService.sendKey(modifier, action = "release")
    }

    fun sendText(text: String) {
        webSocketService.sendText(text)
    }

    fun pasteClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString()
            if (!text.isNullOrEmpty()) {
                webSocketService.sendText(text)
                Toast.makeText(context, "已粘贴 ${text.length} 个字符", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendMouseMove(x: Int, y: Int) {
        webSocketService.sendMouseMoveDirect(x, y)
    }

    fun sendMouseClick(button: String = "left") {
        webSocketService.sendMouseClick(button)
    }

    fun sendMouseScroll(delta: Int) {
        webSocketService.sendMouseScroll(delta)
    }

    fun switchInputMethod() {
        val imm = getApplication<Application>().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    fun disconnect() {
        webSocketService.disconnect()
    }

    fun isConnected(): Boolean = webSocketService.isConnected()
}
