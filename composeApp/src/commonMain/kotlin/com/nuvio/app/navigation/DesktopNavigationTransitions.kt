package com.nuvio.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.nuvio.app.isDesktop
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene

// Navigation 3 has no page transition on desktop, so pages cut in, often straight to a blank
// loading screen. Here a page opens like an iOS sheet: it slides up from the bottom while the page
// underneath recedes and dims; going back, it slides down and the page underneath comes forward.
private val DesktopPageEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val DesktopPageMillis = 760
private const val DesktopPageRecededScale = 0.94f
private const val DesktopPageRecededAlpha = 0.35f

internal fun <T : Any> desktopPageOpenTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    slideInVertically(tween(DesktopPageMillis, easing = DesktopPageEasing)) { height -> height } togetherWith
        (
            scaleOut(tween(DesktopPageMillis, easing = DesktopPageEasing), targetScale = DesktopPageRecededScale) +
                fadeOut(tween(DesktopPageMillis, easing = DesktopPageEasing), targetAlpha = DesktopPageRecededAlpha)
            )
}

internal fun <T : Any> desktopPageBackTransition(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
    (
        (
            scaleIn(tween(DesktopPageMillis, easing = DesktopPageEasing), initialScale = DesktopPageRecededScale) +
                fadeIn(tween(DesktopPageMillis, easing = DesktopPageEasing), initialAlpha = DesktopPageRecededAlpha)
            ) togetherWith
            // The open curve front-loads its motion; run backwards it reads as a snap, so the\r
            // dismissal eases in and out over the same time instead.\r
            slideOutVertically(tween(DesktopPageMillis, easing = DesktopPageBackEasing)) { height -> height }
        ).apply { targetContentZIndex = -1f }
}

/**
 * Desktop: shows a page's content only once its open transition has finished, then fades it in,
 * like iPadOS. Composing a page as heavy as the details page while it slid in stalled the slide for
 * about 100 ms.
 */
@Composable
internal fun DesktopDeferredPageContent(
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!isDesktop || !enabled) {
        content()
        return
    }
    val transition = LocalNavAnimatedContentScope.current.transition
    val settled = !transition.isRunning && transition.currentState == transition.targetState
    // Saved with the page, so coming back to it (closing a page above) shows it at once.
    var shown by rememberSaveable { mutableStateOf(settled) }
    val alpha = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(settled) {
        if (settled && !shown) shown = true
    }
    LaunchedEffect(shown) {
        if (shown) alpha.animateTo(1f, tween(DesktopPageRevealMillis, easing = DesktopPageEasing))
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (shown) {
            Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value }) { content() }
        }
    }
}

private const val DesktopPageRevealMillis = 400

private val DesktopPageBackEasing = androidx.compose.animation.core.CubicBezierEasing(0.45f, 0f, 0.2f, 1f)
