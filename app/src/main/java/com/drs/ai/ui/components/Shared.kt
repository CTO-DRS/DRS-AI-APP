package com.drs.ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

// ═══════════════════════════════════════════════════════════════════════════
// v1.2.1 — shared "modern shell" components used by every screen
// ═══════════════════════════════════════════════════════════════════════════

/** Fade + slide-up entrance used across screens for a soft landing (v1.4: with motion). */
@Composable
fun Modifier.entrance(key: Any = Unit, delayMs: Int = 0): Modifier {
    var shown by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(420, delayMillis = delayMs, easing = FastOutSlowInEasing),
        label = "entranceProgress"
    )
    return this.graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 18f
    }
}

/** Modern screen header: bold title + optional supporting line. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

/** Rounded banner with the brand gradient — gives each screen an identity. */
@Composable
fun GradientBanner(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Box(
        modifier
            .fillMaxWidth()
            .background(brush = heroGradient(), shape = RoundedCornerShape(24.dp))
    ) {
        Row(
            Modifier.padding(18.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

/** Unified card shell for sections: flat surface, generous padding, optional header row. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            content()
        }
    }
}

/** Icon inside a rounded tinted container — the v1.2 signature look. */
@Composable
fun IconBadge(
    icon: ImageVector,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 42.dp,
    corner: Dp = 13.dp
) {
    Box(
        Modifier
            .size(size)
            .background(container, RoundedCornerShape(corner)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(size * 0.55f))
    }
}

/** Switch row with label + supporting caption, wrapped for RTL. */
@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, caption: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/** Labeled slider with live value on the trailing edge. */
@Composable
fun SliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                activeTrackColor = MaterialTheme.colorScheme.primary,
                thumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/** Friendly empty state with icon, message and optional action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(76.dp)
                .background(haloGradient(MaterialTheme.colorScheme.primary), CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp)
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
        )
        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

/** Primary action button — rounded, confident, full-width friendly. */
@Composable
fun PrimaryAction(
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary action button — outlined, softer. */
@Composable
fun SoftAction(
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Horizontally scrollable chip row — keeps long filter lists usable. */
@Composable
fun ChipRow(
    items: List<Pair<String, String>>, // id to label
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((id, label) in items) {
            androidx.compose.material3.FilterChip(
                selected = selectedId == id,
                onClick = { onSelect(id) },
                label = { Text(label) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// v1.4 — motion & delight kit: animated gradients, shimmer, typing indicators
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Slowly breathing Nova gradient — the brand gradient drifts over time so hero
 * cards feel alive instead of static. GPU-friendly (single draw, no blur).
 */
@Composable
fun Modifier.novaBreath(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)): Modifier {
    val transition = rememberInfiniteTransition(label = "novaBreath")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6500, easing = androidx.compose.animation.core.LinearEasing)),
        label = "novaT"
    )
    val start = androidx.compose.ui.geometry.Offset(100f * t, 400f * (1f - t))
    val end = androidx.compose.ui.geometry.Offset(100f + 700f * (1f - t), 300f + 500f * t)
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2E3799),
            Color(0xFF5B4BD0),
            Color(0xFF8B5CF6),
            Color(0xFF22C3E6)
        ),
        start = start,
        end = end
    )
    return this.background(brush = brush, shape = shape)
}

/** Shimmer sweep for loading placeholders — a soft light band crossing the surface. */
@Composable
fun Modifier.shimmer(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val t by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1400, easing = androidx.compose.animation.core.LinearEasing)),
        label = "shimmerT"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val hi = MaterialTheme.colorScheme.surfaceContainerHighest
    return this
        .clip(shape)
        .background(base)
        .background(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, hi.copy(alpha = 0.85f), Color.Transparent),
                start = androidx.compose.ui.geometry.Offset(t * 600f, 0f),
                end = androidx.compose.ui.geometry.Offset(t * 600f + 300f, 120f)
            )
        )
}

/** Three-dot "assistant is thinking" indicator — bounces softly in sequence. */
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier.padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val y by transition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    tween(360, delayMillis = i * 130, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "dot$i"
            )
            Box(
                Modifier
                    .offset(y = y.dp)
                    .size(7.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), CircleShape)
            )
        }
    }
}

/**
 * Message text with a pulsing streaming caret — the caret char gets its own
 * animated span so it breathes while tokens keep arriving.
 */
@Composable
fun streamingText(text: String): androidx.compose.ui.text.AnnotatedString {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "caretAlpha"
    )
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        if (text.isNotEmpty()) {
            pushStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                    fontWeight = FontWeight.Bold
                )
            )
            append("▍")
            pop()
        }
    }
}

/** Animated count-up number for dashboard stats — counts from 0 to [value] once. */
@Composable
fun CountUpText(
    value: Int,
    modifier: Modifier = Modifier,
    valueStyle: androidx.compose.ui.text.TextStyle? = null,
    format: (Int) -> String = { it.toString() }
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(value) { started = true }
    val animated by animateFloatAsState(
        targetValue = if (started) value.toFloat() else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "countUp"
    )
    Text(
        format(animated.toInt()),
        modifier,
        style = valueStyle ?: MaterialTheme.typography.titleLarge
    )
}

/** Compact stat chip: value on top, label below — used in the usage stats card. */
@Composable
fun StatChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
