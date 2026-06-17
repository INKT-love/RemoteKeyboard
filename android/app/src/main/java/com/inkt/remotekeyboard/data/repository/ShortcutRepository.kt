package com.inkt.remotekeyboard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inkt.remotekeyboard.data.model.CustomShortcut
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.shortcutDataStore: DataStore<Preferences> by preferencesDataStore(name = "shortcuts")

@Singleton
class ShortcutRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val shortcutsKey = stringPreferencesKey("custom_shortcuts")

    val shortcuts: Flow<List<CustomShortcut>> = context.shortcutDataStore.data.map { prefs ->
        val json = prefs[shortcutsKey] ?: "[]"
        val type = object : TypeToken<List<CustomShortcut>>() {}.type
        gson.fromJson(json, type) ?: getDefaultShortcuts()
    }

    suspend fun addShortcut(shortcut: CustomShortcut) {
        context.shortcutDataStore.edit { prefs ->
            val current = getCurrentShortcuts(prefs)
            val updated = current + shortcut
            prefs[shortcutsKey] = gson.toJson(updated)
        }
    }

    suspend fun removeShortcut(id: String) {
        context.shortcutDataStore.edit { prefs ->
            val current = getCurrentShortcuts(prefs)
            val updated = current.filter { it.id != id }
            prefs[shortcutsKey] = gson.toJson(updated)
        }
    }

    suspend fun updateShortcut(shortcut: CustomShortcut) {
        context.shortcutDataStore.edit { prefs ->
            val current = getCurrentShortcuts(prefs)
            val updated = current.map { if (it.id == shortcut.id) shortcut else it }
            prefs[shortcutsKey] = gson.toJson(updated)
        }
    }

    suspend fun resetToDefault() {
        context.shortcutDataStore.edit { prefs ->
            prefs[shortcutsKey] = gson.toJson(getDefaultShortcuts())
        }
    }

    private fun getCurrentShortcuts(prefs: Preferences): List<CustomShortcut> {
        val json = prefs[shortcutsKey] ?: return getDefaultShortcuts()
        val type = object : TypeToken<List<CustomShortcut>>() {}.type
        return gson.fromJson(json, type) ?: getDefaultShortcuts()
    }

    private fun getDefaultShortcuts(): List<CustomShortcut> = listOf(
        CustomShortcut("1", "复制", "c", listOf("ctrl")),
        CustomShortcut("2", "粘贴", "v", listOf("ctrl")),
        CustomShortcut("3", "撤销", "z", listOf("ctrl")),
        CustomShortcut("4", "重做", "z", listOf("ctrl", "shift")),
        CustomShortcut("5", "全选", "a", listOf("ctrl")),
        CustomShortcut("6", "保存", "s", listOf("ctrl")),
        CustomShortcut("7", "查找", "f", listOf("ctrl")),
        CustomShortcut("8", "关闭", "w", listOf("ctrl")),
        CustomShortcut("9", "切换窗口", "tab", listOf("alt")),
        CustomShortcut("10", "锁定", "l", listOf("win")),
    )
}
