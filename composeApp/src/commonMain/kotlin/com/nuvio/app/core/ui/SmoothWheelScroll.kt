package com.nuvio.app.core.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nuvio.app.isDesktop
import kotlin.math.abs
import kotlin.math.exp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val WHEEL_STEP = 100.dp
private const val WHEEL_SMOOTHING_SECONDS = 0.07f

/**
 * Mouse-wheel scrolling that glides towards where the wheel has been turned, like a browser,
 * instead of moving a fixed step per notch and stopping. Each frame covers part of the remaining
 * distance, so quick turns (or a free-spinning wheel) blend into one continuous motion.
 *
 * Vertical lists take the vertical wheel; horizontal rows take the horizontal (thumb) wheel and
 * Shift + wheel. Scrolling goes through nested scroll first, so parents such as the Home hero
 * stretch still see what the list cannot take at its ends. Desktop only; elsewhere it does nothing.
 */
fun Modifier.smoothWheelScroll(
    state: ScrollableState,
    orientation: Orientation = Orientation.Vertical,
): Modifier {
    if (!isDesktop) return this
    return composed {
        val scope = rememberCoroutineScope()
        val stepPx = with(LocalDensity.current) { WHEEL_STEP.toPx() }
        val dispatcher = remember { NestedScrollDispatcher() }
        val glide = remember { WheelGlide() }

        this
            .nestedScroll(remember { object : NestedScrollConnection {} }, dispatcher)
            .pointerInput(state, stepPx, orientation) {
                awaitPointerEventScope {
                    while (true) {
                        // Initial pass: taken before the list's own wheel handling sees it.
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type != PointerEventType.Scroll) continue
                        val delta = event.wheelDelta(orientation)
                        if (delta == 0f) continue
                        event.changes.forEach { it.consume() }
                        glide.pending += delta * stepPx
                        if (glide.job?.isActive == true) continue
                        glide.job = scope.launch {
                            state.scroll {
                                var lastFrame = withFrameNanos { it }
                                while (abs(glide.pending) > 0.5f) {
                                    val frame = withFrameNanos { it }
                                    val seconds = (frame - lastFrame) / 1_000_000_000f
                                    lastFrame = frame
                                    val step = glide.pending *
                                        (1f - exp(-seconds / WHEEL_SMOOTHING_SECONDS))
                                    glide.pending -= step
                                    // Nested scroll works in content-movement direction: the
                                    // opposite sign of a scroll offset change.
                                    val preConsumed = -dispatcher.dispatchPreScroll(
                                        orientation.offset(-step),
                                        NestedScrollSource.UserInput,
                                    ).along(orientation)
                                    val listDelta = step - preConsumed
                                    val consumed = scrollBy(listDelta)
                                    val remaining = listDelta - consumed
                                    if (remaining != 0f) {
                                        dispatcher.dispatchPostScroll(
                                            orientation.offset(-consumed),
                                            orientation.offset(-remaining),
                                            NestedScrollSource.UserInput,
                                        )
                                    }
                                }
                                glide.pending = 0f
                            }
                        }
                    }
                }
            }
    }
}

private fun PointerEvent.wheelDelta(orientation: Orientation): Float {
    val shift = keyboardModifiers.isShiftPressed
    return changes.fold(0f) { sum, change ->
        val delta = change.scrollDelta
        sum + when (orientation) {
            Orientation.Vertical -> if (shift) 0f else delta.y
            Orientation.Horizontal -> delta.x + if (shift) delta.y else 0f
        }
    }
}

private fun Orientation.offset(value: Float): Offset =
    if (this == Orientation.Vertical) Offset(0f, value) else Offset(value, 0f)

private fun Offset.along(orientation: Orientation): Float =
    if (orientation == Orientation.Vertical) y else x

private class WheelGlide {
    var pending = 0f
    var job: Job? = null
}
