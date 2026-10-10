package com.nuvio.app.features.details

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nuvio.app.isDesktop
import kotlinx.coroutines.delay

// Long enough that sweeping the pointer across a row doesn't load every poster it passes.
private const val DetailsPrefetchHoverDelayMillis = 200L

/** Desktop: starts loading the item's details once the pointer rests on its poster. */
@Composable
internal fun Modifier.prefetchDetailsOnHover(type: String, id: String): Modifier {
    if (!isDesktop) return this
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    LaunchedEffect(hovered, type, id) {
        if (!hovered) return@LaunchedEffect
        delay(DetailsPrefetchHoverDelayMillis)
        MetaDetailsRepository.prefetch(type, id)
    }
    return hoverable(interactionSource)
}
