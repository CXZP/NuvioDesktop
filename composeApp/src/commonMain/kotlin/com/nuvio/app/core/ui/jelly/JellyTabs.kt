package com.nuvio.app.core.ui.jelly

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.FloatingNavigationItem
import com.nuvio.app.core.ui.accentBrush
import com.nuvio.app.core.ui.gradientMask
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.themePalette
import com.nuvio.app.core.ui.visualNavIndex
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Density

@Composable
internal fun JellyTabRow(
    items: List<FloatingNavigationItem>,
    labelFraction: Float,
    motion: JellyMotion,
    active: Boolean,
    compactSize: Boolean,
    modifier: Modifier,
    horizontalLabels: Boolean = false,
    iconSize: Dp = if (compactSize) 24.dp else 28.dp,
) {
    val tokens = MaterialTheme.nuvio
    val palette = MaterialTheme.themePalette
    val color = if (horizontalLabels && active) tokens.colors.textPrimary else if (active) tokens.colors.accent else tokens.colors.textMuted
    val iconModifier = Modifier.size(if (horizontalLabels) iconSize - 10.dp else iconSize)
        .then(if (active && !horizontalLabels) Modifier.gradientMask(palette.accentBrush()) else Modifier)
    val iconTint = if (active) Color.White else color
    JellyTabCells(
        modifier = modifier.padding(vertical = 4.dp).clearAndSetSemantics {},
    ) {
        items.forEach { item ->
            Box(
                Modifier.fillMaxHeight().graphicsLayer {
                    val scale = if (active) motion.frame.contentScale else 1f
                    scaleX = scale
                    scaleY = scale
                },
                contentAlignment = Alignment.Center,
            ) {
                JellyTabContent(item.label, labelFraction, compactSize, horizontalLabels, iconSize, color, active) {
                    when {
                        item.icon != null -> Icon(item.icon, null, iconModifier, tint = iconTint)
                        item.drawable != null -> Icon(painterResource(item.drawable), null, iconModifier, tint = iconTint)
                    }
                }
            }
        }
    }
}

@Composable
internal fun JellyTabTargets(
    items: List<FloatingNavigationItem>,
    labelFraction: Float,
    motion: JellyMotion,
    compactSize: Boolean,
    modifier: Modifier,
    horizontalLabels: Boolean = false,
    iconSize: Dp = if (compactSize) 24.dp else 28.dp,
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    JellyTabCells(modifier.selectableGroup()) {
        items.forEachIndexed { index, item ->
            val visualIndex = visualNavIndex(index, items.size, isRtl)
            val onClick = {
                motion.select(visualIndex)
                item.onClick()
            }
            Box(
                modifier = Modifier.fillMaxHeight()
                    .selectable(
                        selected = item.selected,
                        role = Role.Tab,
                        interactionSource = null,
                        indication = null,
                        onClick = onClick,
                    )
                    .clearAndSetSemantics {
                        role = Role.Tab
                        selected = item.selected
                        contentDescription = item.label
                        onClick { onClick(); true }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (item.content != null) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            val frame = motion.frame
                            val coverage = (1f - abs(frame.position - visualIndex)).coerceIn(0f, 1f)
                            val scale = 1f + (frame.contentScale - 1f) * coverage
                            scaleX = scale
                            scaleY = scale
                        },
                    ) {
                        JellyTabContent(item.label, labelFraction, compactSize, horizontalLabels, iconSize, Color.Transparent, false) {
                            item.content(onClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JellyTabContent(
    label: String,
    labelFraction: Float,
    compactSize: Boolean,
    horizontal: Boolean,
    iconSize: Dp,
    color: Color,
    active: Boolean,
    icon: @Composable () -> Unit,
) {
    val labelStyle = if (horizontal) {
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
    } else {
        TextStyle(
            fontSize = if (compactSize) 12.sp else 13.sp,
            lineHeight = if (compactSize) 14.sp else 16.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
    val iconContent: @Composable () -> Unit = {
        Box(
            Modifier.size(iconSize).graphicsLayer {
                if (!horizontal) translationY = 2.dp.toPx() * labelFraction
            },
            contentAlignment = Alignment.Center,
        ) { icon() }
    }
    val labelContent: @Composable () -> Unit = {
        Text(
            text = label,
            color = color,
            style = labelStyle,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alpha(labelFraction)
                .then(if (horizontal) Modifier else Modifier.fillMaxWidth().padding(horizontal = 4.dp)),
        )
    }
    if (horizontal) {
        // Fills the tab and places the icon and label at exact (fractional) positions. The label
        // is measured at its own width, not at the space the tab has right now: while the bar
        // widened, that space grew every frame, the label switched between ellipsized lengths, and
        // with whole-pixel centring on top the icons jumped back and forth.
        Layout(
            modifier = Modifier.padding(horizontal = 8.dp).clipToBounds(),
            content = {
                iconContent()
                labelContent()
            },
        ) { measurables, constraints ->
            val iconPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
            val gap = 6.dp.toPx()
            val labelPlaceable = measurables[1].measure(
                Constraints(maxWidth = JellyLabelMaxWidth.roundToPx(), maxHeight = constraints.maxHeight),
            )
            val revealedLabel = (labelPlaceable.width + gap) * labelFraction
            val contentWidth = iconPlaceable.width + revealedLabel
            val width = constraints.maxWidth
            val height = constraints.constrainHeight(maxOf(iconPlaceable.height, labelPlaceable.height))
            val iconX = (width - contentWidth) / 2f
            layout(width, height) {
                placeFractional(iconPlaceable, iconX, (height - iconPlaceable.height) / 2, width, layoutDirection)
                placeFractional(
                    labelPlaceable,
                    iconX + iconPlaceable.width + gap,
                    (height - labelPlaceable.height) / 2,
                    width,
                    layoutDirection,
                    revealWidth = (revealedLabel - gap).coerceAtLeast(0f),
                )
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            iconContent()
            Box(Modifier.height((if (compactSize) 14.dp else 16.dp) * labelFraction).fillMaxWidth().clipToBounds()) {
                labelContent()
            }
        }
    }
}

private val JellyLabelMaxWidth = 120.dp

// Tabs at their exact share of the bar. Whole-pixel tab widths and positions rounded differently
// from one frame to the next while the bar changed width, which made the icons shake.
@Composable
private fun JellyTabCells(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val inset = 4.dp.toPx()
        val tabWidth = ((constraints.maxWidth - 2 * inset) / measurables.size.coerceAtLeast(1)).coerceAtLeast(0f)
        val cellWidth = ceil(tabWidth).toInt()
        val height = constraints.maxHeight
        val placeables = measurables.map { it.measure(Constraints.fixed(cellWidth, height)) }
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                placeFractional(placeable, inset + index * tabWidth, 0, constraints.maxWidth, layoutDirection)
            }
        }
    }
}

// Places at a fractional x: whole pixels through layout, the rest as a layer offset, mirrored for
// right-to-left like placeRelative. [revealWidth] clips the placeable to that much of its leading side.
private fun Placeable.PlacementScope.placeFractional(
    placeable: Placeable,
    x: Float,
    y: Int,
    parentWidth: Int,
    direction: LayoutDirection,
    revealWidth: Float? = null,
) {
    val left = if (direction == LayoutDirection.Rtl) parentWidth - x - placeable.width else x
    val whole = floor(left)
    placeable.placeWithLayer(whole.toInt(), y) {
        translationX = left - whole
        if (revealWidth != null) {
            clip = true
            shape = LeadingRevealShape(revealWidth)
        }
    }
}

private class LeadingRevealShape(private val width: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val visible = width.coerceIn(0f, size.width)
        return Outline.Rectangle(
            if (layoutDirection == LayoutDirection.Rtl) {
                Rect(size.width - visible, 0f, size.width, size.height)
            } else {
                Rect(0f, 0f, visible, size.height)
            },
        )
    }
}
