package com.nuvio.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.nuvioCardDepth
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.flow.first

/**
 * Desktop: an entry marked with [desktopDrawerMetadata] opens as a glass panel sliding in from
 * the right over the page below it (which stays visible and in place), instead of as a page of its
 * own. Used for the source picker, so pressing Play on details keeps details on screen.
 */
internal fun desktopDrawerMetadata(): Map<String, Any> = mapOf(DesktopDrawerMetadataKey to true)

/** The page under a drawer marks itself as the blur source; the drawer blurs it. */
internal val desktopDrawerHazeState = HazeState()

/** Lets drawer content take the whole window for a while (the auto-play loading screen). */
internal class DesktopDrawerController {
    var fullscreen by mutableStateOf(false)
}

internal val LocalDesktopDrawer = staticCompositionLocalOf<DesktopDrawerController?> { null }

internal class DesktopDrawerSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        if (entry.metadata[DesktopDrawerMetadataKey] != true || entries.size < 2) return null
        return DesktopDrawerScene(
            entry = entry,
            previousEntries = entries.dropLast(1),
            onDismiss = onBack,
        )
    }
}

private class DesktopDrawerScene<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val onDismiss: () -> Unit,
) : OverlayScene<T> {
    override val key: Any = entry.contentKey
    override val entries: List<NavEntry<T>> = listOf(entry)
    override val overlaidEntries: List<NavEntry<T>> = previousEntries

    // Scenes are recalculated on every back stack change; the open/closed state must outlive them.
    private val visibility = drawerVisibilities.getOrPut(key) {
        MutableTransitionState(false).apply { targetState = true }
    }

    override val content: @Composable () -> Unit = {
        DesktopDrawerHost(visibility = visibility, onDismiss = onDismiss) { entry.Content() }
    }

    override suspend fun onRemove() {
        visibility.targetState = false
        snapshotFlow { visibility.isIdle }.first { it }
        drawerVisibilities.remove(key)
    }

    override fun equals(other: Any?): Boolean =
        other is DesktopDrawerScene<*> && key == other.key && previousEntries == other.previousEntries

    override fun hashCode(): Int = key.hashCode() * 31 + previousEntries.hashCode()
}

@Composable
private fun DesktopDrawerHost(
    visibility: MutableTransitionState<Boolean>,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val controller = remember { DesktopDrawerController() }
    val shape = RoundedCornerShape(28.dp)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val panelWidth = (maxWidth * 0.46f).coerceIn(420.dp, 600.dp)
        AnimatedVisibility(
            visibleState = visibility,
            enter = fadeIn(tween(DrawerFadeMs)),
            exit = fadeOut(tween(DrawerFadeMs)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            )
        }
        AnimatedVisibility(
            visibleState = visibility,
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = slideInHorizontally(tween(DrawerSlideMs, easing = DrawerEasing)) { it / 2 } +
                fadeIn(tween(DrawerFadeMs)),
            exit = slideOutHorizontally(tween(DrawerSlideMs, easing = DrawerEasing)) { it / 2 } +
                fadeOut(tween(DrawerFadeMs)),
        ) {
            Box(
                modifier = if (controller.fullscreen) {
                    Modifier.width(maxWidth).fillMaxHeight()
                } else {
                    Modifier
                        .padding(16.dp)
                        .width(panelWidth)
                        .fillMaxHeight()
                        .shadow(32.dp, shape)
                        .clip(shape)
                        .hazeEffect(desktopDrawerHazeState) {
                            blurRadius = 32.dp
                            noiseFactor = 0f
                        }
                        .background(Color(0xFF1C1C1E).copy(alpha = 0.72f))
                        .nuvioCardDepth(shape, NuvioCardDepthSurface.Controls, fallbackBorderAlpha = 0.14f)
                }
                    // Clicks inside the panel must not reach the scrim behind it.
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                CompositionLocalProvider(LocalDesktopDrawer provides controller) {
                    content()
                }
            }
        }
    }
}

private const val DesktopDrawerMetadataKey = "nuvio.desktopDrawer"
private const val DrawerSlideMs = 480
private const val DrawerFadeMs = 320
private val DrawerEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private val drawerVisibilities = mutableMapOf<Any, MutableTransitionState<Boolean>>()
