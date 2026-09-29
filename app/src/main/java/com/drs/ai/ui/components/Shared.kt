package com.drs.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Brand gradient — indigo → violet → cyan ("Nova Indigo" v1.2). */
fun brandGradient(): Brush = Brush.linearGradient(
    colors = listOf(Color(0xFF3A45A8), Color(0xFF5B4BD0), Color(0xFF7C4DFF), Color(0xFF22A7D3))
)

/** Diagonal variant for hero cards. */
fun heroGradient(): Brush = Brush.linearGradient(
    colors = listOf(Color(0xFF2E3799), Color(0xFF5B4BD0), Color(0xFF8B5CF6), Color(0xFF22C3E6)),
    start = androidx.compose.ui.geometry.Offset.Zero,
    end = androidx.compose.ui.geometry.Offset.Infinite
)

/** Soft halo gradient for subtle backgrounds. */
fun haloGradient(base: Color): Brush = Brush.radialGradient(
    colors = listOf(base.copy(alpha = 0.18f), Color.Transparent),
    radius = 900f
)

/**
 * Animated scale-on-press wrapper. Gives every tappable card/button a modern,
 * tactile squeeze instead of a flat ripple-only response.
 */
@Composable
fun Modifier.pressScale(
    pressedScale: Float = 0.97f,
    interactionSource: MutableInteractionSource
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "pressScale"
    )
    return this.scale(scale)
}

/** Pulsing status dot — used for "generating", live states. */
@Composable
fun PulsingDot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(
        modifier
            .size(size)
            .alpha(alpha)
            .background(color, CircleShape)
    )
}

/** Small status pill with a leading status dot: LOCAL / OFFLINE / NO MODEL. */
@Composable
fun StatusBadge(text: String, positive: Boolean, modifier: Modifier = Modifier) {
    val container = if (positive)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (positive)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant
    val dot = if (positive)
        MaterialTheme.colorScheme.secondary
    else
        MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier,
        color = container,
        contentColor = fg,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(dot, CircleShape)
            )
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Section header with consistent tone and spacing. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

/** Key/value row used in profile + diagnostics cards. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

@Composable
fun RowSpaced(horizontal: Int = 8, content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(horizontal.dp), content = content)
}
