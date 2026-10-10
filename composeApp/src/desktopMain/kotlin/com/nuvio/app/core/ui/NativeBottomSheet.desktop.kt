package com.nuvio.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// Desktop shows the app's bottom sheets as a centred panel, like the dialogs elsewhere in Settings
// (and macOS/iPadOS form sheets): a sheet rising from the bottom edge of a wide window read as a
// phone layout, and pickers came up three different ways.
internal actual val usesNativeNuvioBottomSheet: Boolean = true

private val SheetEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val SheetEnterMillis = 420
internal const val DesktopSheetExitMillis = 220L
private val SheetShape = RoundedCornerShape(24.dp)

// Bumped by dismissNativeNuvioBottomSheet; the open panel animates out when it changes.
private var closeRequests by mutableIntStateOf(0)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal actual fun NuvioNativeModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    showDragHandle: Boolean,
    fullHeight: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val progress = remember { Animatable(0f) }
    val openedAt = remember { closeRequests }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SheetEnterMillis, easing = SheetEasing))
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var dismissing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // A click outside or Esc closes with the same animation as a picked option.
    val animatedDismiss: () -> Unit = {
        if (!dismissing) {
            dismissing = true
            scope.launch {
                progress.animateTo(0f, tween(DesktopSheetExitMillis.toInt(), easing = SheetEasing))
                onDismissRequest()
            }
        }
    }
    LaunchedEffect(closeRequests) {
        if (closeRequests != openedAt) {
            progress.animateTo(0f, tween(DesktopSheetExitMillis.toInt(), easing = SheetEasing))
        }
    }
    Dialog(
        onDismissRequest = animatedDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            scrimColor = Color.Transparent,
            animateTransition = false,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                .background(tokens.colors.overlayScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = animatedDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            val maxPanelHeight = maxHeight * if (fullHeight) 0.86f else 0.8f
            Column(
                modifier = modifier
                    .padding(24.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxPanelHeight)
                    .graphicsLayer {
                        val scale = 0.94f + 0.06f * progress.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(SheetShape)
                    .background(containerColor)
                    .nuvioCardDepth(SheetShape, NuvioCardDepthSurface.Controls, fallbackBorderAlpha = 0.10f)
                    // Clicks inside the panel must not reach the scrim.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(top = 20.dp),
            ) {
                CompositionLocalProvider(LocalBottomInsetsConsumed provides true) {
                    content()
                }
            }
        }
    }
}

internal actual fun dismissNativeNuvioBottomSheet() {
    closeRequests += 1
}
