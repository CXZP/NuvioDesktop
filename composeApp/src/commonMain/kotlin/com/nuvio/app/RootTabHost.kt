package com.nuvio.app

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.compose.rememberLifecycleOwner
import com.nuvio.app.core.ui.LocalScreenActive

@Composable
internal fun RootTabHost(
    selectedTab: AppScreenTab,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    profileId: Int? = null,
    content: @Composable (AppScreenTab) -> Unit,
) {
    val parentLifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val hostActive = active && parentLifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val focusManager = LocalFocusManager.current
    DisposableEffect(selectedTab, hostActive, profileId) {
        onDispose { focusManager.clearFocus(force = true) }
    }

    key(profileId) {
        val tabStateHolder = rememberSaveableStateHolder()
        val visitedTabs = remember { mutableSetOf<AppScreenTab>() }
        val displayedTabs = remember(selectedTab) { (visitedTabs + selectedTab).toList() }
        SideEffect { visitedTabs += selectedTab }
        val tabTransition = updateTransition(selectedTab, label = "root_tab")
        val tabVisibility = displayedTabs.map { tab ->
            key(tab) {
                tabTransition.animateFloat(
                    transitionSpec = {
                        if (isDesktop) tween(RootTabSwitchMillis, easing = LinearEasing) else snap()
                    },
                    label = "root_tab_visibility",
                ) { shown -> if (shown == tab) 1f else 0f }
            }
        }

        Layout(
            modifier = modifier.fillMaxSize(),
            content = {
                displayedTabs.forEach { tab ->
                    key(tab) {
                        RootTabPane(
                            tab = tab,
                            active = hostActive && tab == selectedTab,
                            stateHolder = tabStateHolder,
                            content = content,
                        )
                    }
                }
            },
        ) { measurables, constraints ->
            val selectedIndex = displayedTabs.indexOf(selectedTab)
            // Desktop: the tab being left stays in place while it fades out, under the new one.
            val leavingIndex = displayedTabs.indices.firstOrNull { index ->
                index != selectedIndex && tabVisibility[index].value > 0f
            }
            val placeable = measurables[selectedIndex].measure(constraints)
            val leaving = leavingIndex?.let { measurables[it].measure(constraints) }
            val enterOffsetPx = RootTabEnterOffset.toPx()
            layout(placeable.width, placeable.height) {
                if (leaving != null && leavingIndex != null) {
                    leaving.placeRelativeWithLayer(0, 0) {
                        alpha = rootTabLeaveAlpha(tabVisibility[leavingIndex].value)
                    }
                }
                placeable.placeRelativeWithLayer(0, 0) {
                    val visibility = tabVisibility[selectedIndex].value
                    alpha = rootTabEnterAlpha(visibility)
                    translationY = (1f - RootTabEnterEasing.transform(visibility)) * enterOffsetPx
                }
            }
        }
    }
}

// Desktop tab switch: the old tab fades out over the first part, then the new one fades in and
// settles up from slightly below, so the two screens never show through each other.
private const val RootTabSwitchMillis = 420
private val RootTabEnterOffset = 16.dp
private val RootTabEnterEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

private fun rootTabLeaveAlpha(visibility: Float): Float = ((visibility - 0.6f) / 0.4f).coerceIn(0f, 1f)

private fun rootTabEnterAlpha(visibility: Float): Float {
    val t = ((visibility - 0.25f) / 0.75f).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
private fun RootTabPane(
    tab: AppScreenTab,
    active: Boolean,
    stateHolder: SaveableStateHolder,
    content: @Composable (AppScreenTab) -> Unit,
) {
    val lifecycleOwner = rememberLifecycleOwner(
        maxLifecycle = if (active) Lifecycle.State.RESUMED else Lifecycle.State.CREATED,
    )
    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycleOwner,
        LocalScreenActive provides active,
    ) {
        stateHolder.SaveableStateProvider(tab.name) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer()
                    .focusProperties { canFocus = active }
                    .pointerInput(active) {
                        if (!active) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .then(if (active) Modifier else Modifier.clearAndSetSemantics {}),
            ) {
                content(tab)
            }
        }
    }
}
