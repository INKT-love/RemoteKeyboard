package com.inkt.remotekeyboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.inkt.remotekeyboard.viewmodel.SettingsViewModel
import com.inkt.remotekeyboard.data.model.ThemeMode

private val MiuiLightColorScheme = lightColorScheme(
    primary = MiuiLightPrimary,
    onPrimary = MiuiLightOnPrimary,
    primaryContainer = MiuiLightPrimaryContainer,
    onPrimaryContainer = MiuiLightOnPrimaryContainer,
    secondary = MiuiLightSecondary,
    onSecondary = MiuiLightOnSecondary,
    secondaryContainer = MiuiLightSecondaryContainer,
    onSecondaryContainer = MiuiLightOnSecondaryContainer,
    tertiary = MiuiLightTertiary,
    onTertiary = MiuiLightOnTertiary,
    tertiaryContainer = MiuiLightTertiaryContainer,
    onTertiaryContainer = MiuiLightOnTertiaryContainer,
    background = MiuiLightBackground,
    onBackground = MiuiLightOnBackground,
    surface = MiuiLightSurface,
    onSurface = MiuiLightOnSurface,
    surfaceVariant = MiuiLightSurfaceVariant,
    onSurfaceVariant = MiuiLightOnSurfaceVariant,
    error = MiuiLightError,
    onError = MiuiLightOnError,
    outline = MiuiLightOutline,
    surfaceContainerLow = MiuiLightSurfaceContainerLow,
    surfaceContainer = MiuiLightSurfaceContainer,
    surfaceContainerHigh = MiuiLightSurfaceContainerHigh,
)

private val MiuiDarkColorScheme = darkColorScheme(
    primary = MiuiDarkPrimary,
    onPrimary = MiuiDarkOnPrimary,
    primaryContainer = MiuiDarkPrimaryContainer,
    onPrimaryContainer = MiuiDarkOnPrimaryContainer,
    secondary = MiuiDarkSecondary,
    onSecondary = MiuiDarkOnSecondary,
    secondaryContainer = MiuiDarkSecondaryContainer,
    onSecondaryContainer = MiuiDarkOnSecondaryContainer,
    tertiary = MiuiDarkTertiary,
    onTertiary = MiuiDarkOnTertiary,
    tertiaryContainer = MiuiDarkTertiaryContainer,
    onTertiaryContainer = MiuiDarkOnTertiaryContainer,
    background = MiuiDarkBackground,
    onBackground = MiuiDarkOnBackground,
    surface = MiuiDarkSurface,
    onSurface = MiuiDarkOnSurface,
    surfaceVariant = MiuiDarkSurfaceVariant,
    onSurfaceVariant = MiuiDarkOnSurfaceVariant,
    error = MiuiDarkError,
    onError = MiuiDarkOnError,
    outline = MiuiDarkOutline,
    surfaceContainerLow = MiuiDarkSurfaceContainerLow,
    surfaceContainer = MiuiDarkSurfaceContainer,
    surfaceContainerHigh = MiuiDarkSurfaceContainerHigh,
)

@Composable
fun RemoteKeyboardTheme(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT, ThemeMode.MIUIX_LIGHT -> false
        ThemeMode.DARK, ThemeMode.MIUIX_DARK -> true
        ThemeMode.SYSTEM, ThemeMode.MONET -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeMode) {
        ThemeMode.MIUIX_LIGHT -> MiuiLightColorScheme
        ThemeMode.MIUIX_DARK -> MiuiDarkColorScheme
        ThemeMode.MONET -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) MiuiDarkColorScheme else MiuiLightColorScheme
            }
        }
        ThemeMode.LIGHT -> MiuiLightColorScheme
        ThemeMode.DARK -> MiuiDarkColorScheme
        ThemeMode.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) MiuiDarkColorScheme else MiuiLightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MiuiTypography,
        content = content
    )
}
