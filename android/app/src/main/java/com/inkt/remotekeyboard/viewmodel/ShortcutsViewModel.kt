package com.inkt.remotekeyboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inkt.remotekeyboard.data.model.CustomShortcut
import com.inkt.remotekeyboard.data.repository.ShortcutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    private val shortcutRepository: ShortcutRepository
) : ViewModel() {

    val shortcuts: StateFlow<List<CustomShortcut>> = shortcutRepository.shortcuts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addShortcut(name: String, key: String, modifiers: List<String>) {
        val shortcut = CustomShortcut(
            id = UUID.randomUUID().toString(),
            name = name,
            key = key,
            modifiers = modifiers
        )
        viewModelScope.launch {
            shortcutRepository.addShortcut(shortcut)
        }
    }

    fun updateShortcut(shortcut: CustomShortcut) {
        viewModelScope.launch {
            shortcutRepository.updateShortcut(shortcut)
        }
    }

    fun removeShortcut(id: String) {
        viewModelScope.launch {
            shortcutRepository.removeShortcut(id)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            shortcutRepository.resetToDefault()
        }
    }
}
