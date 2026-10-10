package com.nuvio.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.nuvio.app.isDesktop
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val TooltipDelayMillis = 450L

private class TooltipRequest(val label: String, val anchor: Rect)

private object NuvioTooltipState {
    var current by mutableStateOf<TooltipRequest?>(null)
}

/**
 * Desktop: shows [label] in the app's own tooltip (a small glass capsule under the element) after
 * the pointer rests on it, for icon-only buttons whose meaning isn't written on them. Drawn by
 * [NuvioTooltipHost]; does nothing on touch platforms.
 */
fun Modifier.nuvioTooltip(label: String?): Modifier = if (!isDesktop || label.isNullOrBlank()) this else composed {
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var request by remember { mutableStateOf<TooltipRequest?>(null) }
    LaunchedEffect(request) {
        val pending = request ?: return@LaunchedEffect
        delay(TooltipDelayMillis)
        if (request === pending) NuvioTooltipState.current = pending
    }
    DisposableEffect(Unit) {
        onDispose { if (NuvioTooltipState.current === request) NuvioTooltipState.current = null }
    }
    this
        .onGloballyPositioned { bounds = it.boundsInWindow() }
        .pointerInput(label) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when (event.type) {
                        PointerEventType.Enter, PointerEventType.Move -> if (request == null) {
                            request = TooltipRequest(label, bounds)
                        }
                        PointerEventType.Exit, PointerEventType.Press -> {
                            if (NuvioTooltipState.current === request) NuvioTooltipState.current = null
                            request = null
                        }
                    }
                }
            }
        }
}

/** Draws the tooltip requested by [nuvioTooltip]. Place once, at the root of the window. */
@Composable
fun NuvioTooltipHost() {
    val request = NuvioTooltipState.current ?: return
    val tokens = MaterialTheme.nuvio
    val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
    val alpha = remember(request) { Animatable(0f) }
    LaunchedEffect(request) { alpha.animateTo(1f, tween(140)) }
    val shape = RoundedCornerShape(percent = 50)
    Popup(
        popupPositionProvider = remember(request) {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    val a = request.anchor
                    val x = (a.center.x - popupContentSize.width / 2f).roundToInt()
                        .coerceIn(gap, (windowSize.width - popupContentSize.width - gap).coerceAtLeast(gap))
                    val below = a.bottom.roundToInt() + gap
                    val y = if (below + popupContentSize.height > windowSize.height) {
                        a.top.roundToInt() - gap - popupContentSize.height
                    } else {
                        below
                    }
                    return IntOffset(x, y)
                }
            }
        },
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        Text(
            text = request.label,
            color = tokens.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .graphicsLayer { this.alpha = alpha.value }
                .background(Color(0xFF1C1C1E).copy(alpha = 0.92f), shape)
                .border(tokens.borders.thin, Color.White.copy(alpha = 0.14f), shape)
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}
