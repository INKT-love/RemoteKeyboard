package com.inkt.remotekeyboard.di

import android.content.Context
import com.inkt.remotekeyboard.data.repository.DeviceRepository
import com.inkt.remotekeyboard.data.repository.SettingsRepository
import com.inkt.remotekeyboard.service.DiscoveryService
import com.inkt.remotekeyboard.service.WebSocketService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWebSocketService(): WebSocketService = WebSocketService()

    @Provides
    @Singleton
    fun provideDiscoveryService(): DiscoveryService = DiscoveryService()

    @Provides
    @Singleton
    fun provideDeviceRepository(@ApplicationContext context: Context): DeviceRepository =
        DeviceRepository(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)
}
