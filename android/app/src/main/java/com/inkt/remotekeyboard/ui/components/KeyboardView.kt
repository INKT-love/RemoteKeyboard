package com.inkt.remotekeyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inkt.remotekeyboard.viewmodel.KeyboardViewModel

@Composable
fun KeyboardView(
    viewModel: KeyboardViewModel,
    modifier: Modifier = Modifier,
    
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    if (isLandscape) {
        LandscapeKeyboard(viewModel, modifier)
    } else {
        PortraitKeyboard(viewModel, modifier)
    }
}

@Composable
private fun PortraitKeyboard(
    viewModel: KeyboardViewModel,
    modifier: Modifier,
    
) {
    var activeModifiers by remember { mutableStateOf(setOf<String>()) }
    val keyHeight = 48.dp
    val keySpacing = 5.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: QWERTYUIOP + numbers superscript
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keySpacing)
        ) {
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P").forEachIndexed { index, key ->
                KeyButton(
                    label = key,
                    subLabel = if (index < 9) "${index + 1}" else "0",
                    modifier = Modifier.weight(1f).height(keyHeight),
                    
                    onClick = { viewModel.sendKey(key.lowercase(), activeModifiers.toList()) }
                )
            }
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 2: ASDFGHJKL + symbols superscript
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keySpacing)
        ) {
            listOf(
                "A" to "~", "S" to "!", "D" to "@", "F" to "/",
                "G" to "%", "H" to "\"", "J" to "'", "K" to "*", "L" to "?"
            ).forEach { (key, symbol) ->
                KeyButton(
                    label = key,
                    subLabel = symbol,
                    modifier = Modifier.weight(1f).height(keyHeight),
                    
                    onClick = { viewModel.sendKey(key.lowercase(), activeModifiers.toList()) }
                )
            }
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 3: Shift + ZXCVBNM + Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keySpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(
                label = "⇧",
                modifier = Modifier.weight(1.3f).height(keyHeight),
                isModifier = true,
                isActive = activeModifiers.contains("shift"),
                
                onClick = {
                    activeModifiers = if (activeModifiers.contains("shift")) activeModifiers - "shift"
                    else activeModifiers + "shift"
                }
            )
            listOf(
                "Z" to "(", "X" to ")", "C" to "-", "V" to ":",
                "B" to ".", "N" to ",", "M" to "`"
            ).forEach { (key, symbol) ->
                KeyButton(
                    label = key,
                    subLabel = symbol,
                    modifier = Modifier.weight(1f).height(keyHeight),
                    
                    onClick = { viewModel.sendKey(key.lowercase(), activeModifiers.toList()) }
                )
            }
            KeyButton(
                label = "⌫",
                modifier = Modifier.weight(1.3f).height(keyHeight),
                
                onClick = { viewModel.sendKey("backspace") }
            )
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 4: Ctrl, Alt, Fn, Space, Tab, Enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keySpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(
                label = "Ctrl",
                modifier = Modifier.weight(1f).height(keyHeight),
                isModifier = true,
                isActive = activeModifiers.contains("ctrl"),
                
                onClick = {
                    activeModifiers = if (activeModifiers.contains("ctrl")) activeModifiers - "ctrl"
                    else activeModifiers + "ctrl"
                }
            )
            KeyButton(
                label = "Alt",
                modifier = Modifier.weight(1f).height(keyHeight),
                isModifier = true,
                isActive = activeModifiers.contains("alt"),
                
                onClick = {
                    activeModifiers = if (activeModifiers.contains("alt")) activeModifiers - "alt"
                    else activeModifiers + "alt"
                }
            )
            SpaceBar(
                modifier = Modifier.weight(2.5f).height(keyHeight),
                
                onClick = { viewModel.sendKey("space", activeModifiers.toList()) }
            )
            KeyButton(
                label = "Tab",
                modifier = Modifier.weight(1f).height(keyHeight),
                
                onClick = { viewModel.sendKey("tab") }
            )
            KeyButton(
                label = "⏎",
                modifier = Modifier.weight(1f).height(keyHeight),
                
                onClick = { viewModel.sendKey("enter") }
            )
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 5: 候选词 + 中英 +/- Fn Esc
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keySpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(
                label = "<",
                modifier = Modifier.weight(0.8f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("page_up") }
            )
            KeyButton(
                label = ">",
                modifier = Modifier.weight(0.8f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("page_down") }
            )
            KeyButton(
                label = "+",
                modifier = Modifier.weight(0.8f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("=") }
            )
            KeyButton(
                label = "-",
                modifier = Modifier.weight(0.8f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("-") }
            )
            KeyButton(
                label = "中/英",
                modifier = Modifier.weight(1.2f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("space", listOf("ctrl")) }
            )
            KeyButton(
                label = "Fn",
                modifier = Modifier.weight(1f).height(keyHeight - 8.dp),
                
                onClick = { /* fn layer */ }
            )
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 5: Esc, Win, arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(keySpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KeyButton(
                label = "Esc",
                modifier = Modifier.weight(1f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("escape") }
            )
            KeyButton(
                label = "Win",
                modifier = Modifier.weight(1f).height(keyHeight - 8.dp),
                isModifier = true,
                isActive = activeModifiers.contains("super"),
                
                onClick = {
                    activeModifiers = if (activeModifiers.contains("super")) activeModifiers - "super"
                    else activeModifiers + "super"
                }
            )
            Spacer(modifier = Modifier.weight(2f))
            KeyButton(
                label = "◀",
                modifier = Modifier.weight(1f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("left") }
            )
            KeyButton(
                label = "▲",
                modifier = Modifier.weight(1f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("up") }
            )
            KeyButton(
                label = "▼",
                modifier = Modifier.weight(1f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("down") }
            )
            KeyButton(
                label = "▶",
                modifier = Modifier.weight(1f).height(keyHeight - 8.dp),
                
                onClick = { viewModel.sendKey("right") }
            )
        }
    }
}

@Composable
private fun LandscapeKeyboard(
    viewModel: KeyboardViewModel,
    modifier: Modifier,
    
) {
    var activeModifiers by remember { mutableStateOf(setOf<String>()) }
    val keyHeight = 42.dp
    val keySpacing = 3.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Esc + Numbers + Backspace
        Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
            KeyButton(label = "Esc", modifier = Modifier.weight(1f).height(keyHeight),
                 onClick = { viewModel.sendKey("escape") })
            listOf("`", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=").forEach { key ->
                KeyButton(label = key, modifier = Modifier.weight(1f).height(keyHeight),
                     onClick = { viewModel.sendKey(key, activeModifiers.toList()) })
            }
            KeyButton(label = "⌫", modifier = Modifier.weight(1.3f).height(keyHeight),
                 onClick = { viewModel.sendKey("backspace") })
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 2: Tab + QWERTY + [
        Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
            KeyButton(label = "Tab", modifier = Modifier.weight(1.2f).height(keyHeight),
                 onClick = { viewModel.sendKey("tab") })
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P").forEach { key ->
                KeyButton(label = key, modifier = Modifier.weight(1f).height(keyHeight),
                     onClick = { viewModel.sendKey(key.lowercase(), activeModifiers.toList()) })
            }
            KeyButton(label = "[", modifier = Modifier.weight(1f).height(keyHeight),
                 onClick = { viewModel.sendKey("[") })
            KeyButton(label = "]", modifier = Modifier.weight(1f).height(keyHeight),
                 onClick = { viewModel.sendKey("]") })
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 3: Caps + ASDF + Enter
        Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
            KeyButton(label = "⇪", modifier = Modifier.weight(1.3f).height(keyHeight),
                 onClick = { viewModel.sendKey("caps_lock") })
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L").forEach { key ->
                KeyButton(label = key, modifier = Modifier.weight(1f).height(keyHeight),
                     onClick = { viewModel.sendKey(key.lowercase(), activeModifiers.toList()) })
            }
            KeyButton(label = ";", modifier = Modifier.weight(1f).height(keyHeight),
                 onClick = { viewModel.sendKey(";") })
            KeyButton(label = "'", modifier = Modifier.weight(1f).height(keyHeight),
                 onClick = { viewModel.sendKey("'") })
            KeyButton(label = "⏎", modifier = Modifier.weight(1.5f).height(keyHeight),
                 onClick = { viewModel.sendKey("enter") })
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 4: Shift + ZXCVBNM + Shift
        Row(horizontalArrangement = Arrangement.spacedBy(keySpacing)) {
            KeyButton(label = "⇧", modifier = Modifier.weight(1.8f).height(keyHeight), isModifier = true,
                isActive = activeModifiers.contains("shift"), 
                onClick = { activeModifiers = if (activeModifiers.contains("shift")) activeModifiers - "shift" else activeModifiers + "shift" })
            listOf("Z", "X", "C", "V", "B", "N", "M").forEach { key ->
                KeyButton(label = key, modifier = Modifier.weight(1f).height(keyHeight),
                     onClick = { viewModel.sendKey(key.lowercase(), activeModifiers.toList()) })
            }
            KeyButton(label = ",", modifier = Modifier.weight(1f).height(keyHeight),
                 onClick = { viewModel.sendKey(",") })
            KeyButton(label = ".", modifier = Modifier.weight(1f).height(keyHeight),
                 onClick = { viewModel.sendKey(".") })
            KeyButton(label = "/", modifier = Modifier.weight(1f).height(keyHeight),
                 onClick = { viewModel.sendKey("/") })
            KeyButton(label = "⇧", modifier = Modifier.weight(1.8f).height(keyHeight), isModifier = true,
                isActive = activeModifiers.contains("shift"), 
                onClick = { activeModifiers = if (activeModifiers.contains("shift")) activeModifiers - "shift" else activeModifiers + "shift" })
        }

        Spacer(modifier = Modifier.height(keySpacing))

        // Row 5: Ctrl Win Alt Space + - 中英 arrows
        Row(horizontalArrangement = Arrangement.spacedBy(keySpacing), verticalAlignment = Alignment.CenterVertically) {
            KeyButton(label = "Ctrl", modifier = Modifier.weight(1.2f).height(keyHeight), isModifier = true,
                isActive = activeModifiers.contains("ctrl"), 
                onClick = { activeModifiers = if (activeModifiers.contains("ctrl")) activeModifiers - "ctrl" else activeModifiers + "ctrl" })
            KeyButton(label = "Win", modifier = Modifier.weight(1f).height(keyHeight), isModifier = true,
                isActive = activeModifiers.contains("super"), 
                onClick = { activeModifiers = if (activeModifiers.contains("super")) activeModifiers - "super" else activeModifiers + "super" })
            KeyButton(label = "Alt", modifier = Modifier.weight(1f).height(keyHeight), isModifier = true,
                isActive = activeModifiers.contains("alt"), 
                onClick = { activeModifiers = if (activeModifiers.contains("alt")) activeModifiers - "alt" else activeModifiers + "alt" })
            SpaceBar(modifier = Modifier.weight(3f).height(keyHeight), 
                onClick = { viewModel.sendKey("space", activeModifiers.toList()) })
            KeyButton(label = "+", modifier = Modifier.weight(0.8f).height(keyHeight),
                 onClick = { viewModel.sendKey("=") })
            KeyButton(label = "-", modifier = Modifier.weight(0.8f).height(keyHeight),
                 onClick = { viewModel.sendKey("-") })
            KeyButton(label = "中/英", modifier = Modifier.weight(1.2f).height(keyHeight),
                 onClick = { viewModel.sendKey("space", listOf("ctrl")) })
            Spacer(modifier = Modifier.weight(0.2f))
            KeyButton(label = "◀", modifier = Modifier.weight(0.8f).height(keyHeight),
                 onClick = { viewModel.sendKey("left") })
            Column(modifier = Modifier.weight(1f).height(keyHeight), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KeyButton(label = "▲", modifier = Modifier.fillMaxWidth().weight(1f),
                     onClick = { viewModel.sendKey("up") })
                KeyButton(label = "▼", modifier = Modifier.fillMaxWidth().weight(1f),
                     onClick = { viewModel.sendKey("down") })
            }
            KeyButton(label = "▶", modifier = Modifier.weight(0.8f).height(keyHeight),
                 onClick = { viewModel.sendKey("right") })
        }
    }
}
