package com.nuvio.app.core.ui

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nuvio.app.isDesktop
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.DialogProperties

enum class DialogButtonStyle {
    Primary,
    Secondary,
    Destructive,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    BasicAlertDialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = tokens.components.dialogMaxWidth)
                .then(if (isDesktop) Modifier.nuvioCardDepth(tokens.shapes.dialog, NuvioCardDepthSurface.Controls) else Modifier),
            shape = tokens.shapes.dialog,
            color = tokens.colors.surfaceDialog,
            contentColor = tokens.colors.textPrimary,
            border = BorderStroke(tokens.borders.thin, SurfaceEdge),
        ) {
            Column(
                modifier = Modifier.padding(tokens.spacing.dialogPadding),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.listGap),
            ) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.colors.textPrimary,
                    )
                }
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textSecondary,
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun DialogButtons(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = NuvioTokens.Space.s8),
    ) { measurables, constraints ->
        val gap = NuvioTokens.Space.s10.roundToPx()
        val width = constraints.maxWidth
        // Desktop: buttons sized to their labels and lined up on the right, like a macOS dialog,
        // not bars across the whole dialog.
        if (isDesktop) {
            val minWidth = 96.dp.roundToPx()
            val placeables = measurables.map {
                val w = it.maxIntrinsicWidth(constraints.maxHeight).coerceIn(minWidth, width)
                it.measure(Constraints.fixedWidth(w))
            }
            val total = placeables.sumOf { it.width } + totalGapFor(placeables.size, gap)
            if (total <= width) {
                return@Layout layout(width, placeables.maxOfOrNull { it.height } ?: 0) {
                    var x = width - total
                    placeables.forEach { placeable ->
                        placeable.placeRelative(x, 0)
                        x += placeable.width + gap
                    }
                }
            }
        }
        val totalGap = gap * (measurables.size - 1).coerceAtLeast(0)
        val itemWidth = (width - totalGap) / measurables.size.coerceAtLeast(1)
        if (measurables.all { it.maxIntrinsicWidth(constraints.maxHeight) <= itemWidth }) {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(itemWidth)) }
            layout(width, placeables.maxOfOrNull { it.height } ?: 0) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(index * (itemWidth + gap), 0)
                }
            }
        } else {
            val placeables = measurables.map { it.measure(Constraints.fixedWidth(width)) }.asReversed()
            layout(width, placeables.sumOf { it.height } + totalGap) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + gap
                }
            }
        }
    }
}

@Composable
fun DialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DialogButtonStyle = DialogButtonStyle.Secondary,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val tokens = MaterialTheme.nuvio
    val containerColor = when (style) {
        DialogButtonStyle.Primary -> tokens.colors.accent
        DialogButtonStyle.Secondary -> tokens.colors.textPrimary.copy(alpha = tokens.opacity.hover)
        DialogButtonStyle.Destructive -> tokens.colors.danger
    }
    val contentColor = if (style == DialogButtonStyle.Secondary) tokens.colors.textPrimary else tokens.colors.onAccent
    // Desktop: glass capsules like NuvioPrimaryButton; the primary one a little brighter.
    val desktopContainer = when (style) {
        DialogButtonStyle.Primary -> Color.White.copy(alpha = 0.18f)
        DialogButtonStyle.Secondary -> Color.White.copy(alpha = 0.08f)
        DialogButtonStyle.Destructive -> tokens.colors.danger.copy(alpha = 0.85f)
    }
    Button(
        onClick = onClick,
        modifier = if (isDesktop) {
            modifier
                .heightIn(min = 44.dp)
                .nuvioCardDepth(RoundedCornerShape(percent = 50), NuvioCardDepthSurface.Controls)
        } else {
            modifier.heightIn(min = NuvioTokens.Space.s48)
        },
        enabled = enabled && !loading,
        shape = if (isDesktop) RoundedCornerShape(percent = 50) else tokens.shapes.button,
        colors = if (isDesktop) {
            ButtonDefaults.buttonColors(
                containerColor = desktopContainer,
                contentColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContentColor = Color.White.copy(alpha = 0.32f),
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = containerColor.alpha * tokens.opacity.disabled),
                disabledContentColor = contentColor.copy(alpha = tokens.opacity.disabled),
            )
        },
        contentPadding = PaddingValues(horizontal = if (isDesktop) 22.dp else NuvioTokens.Space.s16),
    ) {
        if (loading) {
            NuvioLoadingIndicator(
                modifier = Modifier.size(tokens.icons.sm),
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(NuvioTokens.Space.s8))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun DialogOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    role: Role = Role.RadioButton,
    leading: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.nuvio
    // Desktop: the rows of a list, not a stack of filled boxes. Only the selected row is filled,
    // and the row under the pointer gets a light highlight.
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(tokens.shapes.compactCard)
            .background(
                when {
                    selected -> tokens.colors.accent.copy(alpha = tokens.opacity.selected)
                    !isDesktop -> tokens.colors.textPrimary.copy(alpha = tokens.opacity.subtle)
                    hovered -> tokens.colors.textPrimary.copy(alpha = 0.06f)
                    else -> Color.Transparent
                },
            )
            .hoverable(interaction)
            .selectable(selected = selected, enabled = enabled, role = role, onClick = onClick)
            .padding(horizontal = NuvioTokens.Space.s14, vertical = NuvioTokens.Space.s12),
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s2),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else null,
                color = if (enabled) tokens.colors.textPrimary else tokens.colors.textDisabled,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = tokens.colors.accent,
                modifier = Modifier.size(tokens.icons.md),
            )
        }
    }
}

private fun totalGapFor(count: Int, gap: Int): Int = gap * (count - 1).coerceAtLeast(0)
