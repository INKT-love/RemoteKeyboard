package com.inkt.remotekeyboard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inkt.remotekeyboard.data.model.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "devices")

@Singleton
class DeviceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val devicesKey = stringPreferencesKey("saved_devices")

    val devices: Flow<List<Device>> = context.deviceDataStore.data.map { prefs ->
        val json = prefs[devicesKey] ?: "[]"
        val type = object : TypeToken<List<Device>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }

    suspend fun saveDevice(device: Device) {
        context.deviceDataStore.edit { prefs ->
            val current = getCurrentDevices(prefs)
            val updated = current.filter { it.host != device.host || it.port != device.port } + device
            prefs[devicesKey] = gson.toJson(updated)
        }
    }

    suspend fun removeDevice(device: Device) {
        context.deviceDataStore.edit { prefs ->
            val current = getCurrentDevices(prefs)
            val updated = current.filter { it.id != device.id }
            prefs[devicesKey] = gson.toJson(updated)
        }
    }

    private fun getCurrentDevices(prefs: Preferences): List<Device> {
        val json = prefs[devicesKey] ?: "[]"
        val type = object : TypeToken<List<Device>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
