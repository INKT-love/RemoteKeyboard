package com.inkt.remotekeyboard.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subLabel: String = "",
    isModifier: Boolean = false,
    isActive: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 50),
        label = "keyScale"
    )

    val backgroundColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        isPressed -> Color(0xFF4A4A4A)
        else -> Color(0xFF3A3A3A)
    }
    val contentColor = when {
        isActive -> MaterialTheme.colorScheme.onPrimary
        isPressed -> Color(0xFFE0E0E0)
        else -> Color(0xFFE0E0E0)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale)
            .shadow(3.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (subLabel.isNotEmpty()) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    lineHeight = 8.sp,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                fontSize = 18.sp,
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
fun SpaceBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 50),
        label = "spaceScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale)
            .shadow(3.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) Color(0xFF4A4A4A) else Color(0xFF3A3A3A))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "空格",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888),
        )
    }
}
