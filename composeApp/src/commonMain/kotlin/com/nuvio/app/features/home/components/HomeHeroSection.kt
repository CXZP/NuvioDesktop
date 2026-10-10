package com.nuvio.app.features.home.components

import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.nuvioCardDepth
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.AsyncImagePainter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.app.isDesktop
import com.nuvio.app.core.ui.FullscreenActionButton
import com.nuvio.app.core.ui.NuvioDesktopImageScaling
import com.nuvio.app.core.ui.NuvioAsyncImage as AsyncImage
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.isFullscreenActionSupported
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.heroStretchHeight
import com.nuvio.app.core.ui.ScreenActivityEffect
import com.nuvio.app.core.ui.heroStretchZoom
import com.nuvio.app.core.ui.imageBitmapFromArgb
import com.nuvio.app.core.ui.ultrawideViewportProgress
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.tmdb.originalTmdbImageUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.ceil
import kotlin.math.exp

private const val HERO_BACKGROUND_PARALLAX = 0.055f
private const val HERO_BACKGROUND_SCALE = 1.14f
private const val HERO_DESKTOP_BACKDROP_ASPECT_RATIO = 16f / 9f
private const val HERO_DESKTOP_BACKDROP_MIN_WIDTH_FRACTION = 0.64f
private const val HERO_DESKTOP_SPILL_FRACTION = 0.6f
// Columns of averaged colour the spread is reduced to: a smooth gradient, not stretched streaks.
private const val HERO_DESKTOP_SPREAD_GRADIENT_COLUMNS = 8
// Width the spread is averaged down to before it is smoothed.
private const val HERO_DESKTOP_SPREAD_SAMPLE_WIDTH = 64
// Width the shaded, dithered spread is built at; the GPU stretches it the rest of the way.
private const val HERO_DESKTOP_SPREAD_OUTPUT_WIDTH = 960
// Share of the sharp picture's width that fades in from the left, and of its height that fades out
// at the bottom.
private const val HERO_DESKTOP_PICTURE_LEFT_FADE_FRACTION = 0.22f
private const val HERO_DESKTOP_PICTURE_BOTTOM_FADE_FRACTION = 0.20f
private const val HERO_DESKTOP_MASK_BLEED_PX = 4f
// How strongly the backdrop zooms when the hero is pulled down past the top.
private const val HERO_DESKTOP_STRETCH_ZOOM = 0.5f
// Desktop item change, the way Kodi skins such as Arctic Fuse do it: a short crossfade of the
// picture while the text fades out and the next text fades in. A long dissolve left the two
// pictures showing through each other on screen.
private val HeroDesktopPageChangeSpec = tween<Float>(
    durationMillis = 450,
    easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f),
)
// The old text is gone by this share of the change, and the new text starts after the second one,
// so titles and descriptions never show on top of each other.
private const val HERO_DESKTOP_TEXT_OUT_END = 0.3f
private const val HERO_DESKTOP_TEXT_IN_START = 0.4f
// The logo and description slide this far: the old ones out to one side, the new ones in from the
// other, in the direction of the change, so the change reads as moving to the next item.
private val HERO_DESKTOP_TEXT_SLIDE = 72.dp
// How long an item has to stay in the hero before its details are loaded in the background.
private const val HERO_DESKTOP_DETAILS_PREFETCH_DELAY_MS = 1_200L
// Longest wait for the next item's picture before the change starts anyway.
private const val HERO_DESKTOP_PRELOAD_TIMEOUT_MS = 3_000L
// Background-coloured shade over the spread, (position, alpha), adapted from Arctic Fuse's
// combined_flixart.png overlay: darkest towards the left and below the picture.
private val HERO_DESKTOP_LEFT_SHADE = arrayOf(0.00f to 0.80f, 0.25f to 0.50f, 0.50f to 0f)
private val HERO_DESKTOP_BOTTOM_SHADE =
    arrayOf(0.00f to 0f, 0.60f to 0f, 0.70f to 0.45f, 0.80f to 0.85f, 0.92f to 1f)
// Width of the first, sharp render of the spread before it is reduced into the blur.
private const val HERO_DESKTOP_SPREAD_RENDER_WIDTH = 1024
private val HERO_DESKTOP_EDGE_STRIP_WIDTH = 24.dp
private const val HERO_CONTENT_PARALLAX = 0.18f
private const val HERO_SCROLL_PARALLAX = 0.3f
private const val HERO_SCROLL_DOWN_SCALE_MULTIPLIER = 0.0001f
private const val HERO_SCROLL_UP_SCALE_MULTIPLIER = 0.002f
private const val HERO_SCROLL_MAX_SCALE = 1.3f
private const val HERO_SWIPE_THRESHOLD_FRACTION = 0.16f
private const val HERO_SWIPE_VELOCITY_THRESHOLD = 300f
private const val HERO_AUTO_SCROLL_INTERVAL_MS = 8_000L
private const val MOBILE_HERO_VIEWPORT_RATIO = 0.82f
private const val MOBILE_HERO_MIN_HEIGHT_DP = 360f
private const val MOBILE_HERO_MAX_HEIGHT_DP = 760f
private const val ULTRAWIDE_HERO_VIEWPORT_HEIGHT_RATIO = 1f
private const val DESKTOP_HERO_ULTRAWIDE_HORIZONTAL_PADDING_DP = 120f
private const val DESKTOP_HERO_ULTRAWIDE_BOTTOM_PADDING_DP = 192f
private const val DESKTOP_HERO_TOP_FADE_HEIGHT_DP = 160f
private const val DESKTOP_HERO_BOTTOM_FADE_HEIGHT_DP = 300f

internal data class HomeHeroLayout(
    val isTablet: Boolean,
    val heroHeight: Dp,
    val contentMaxWidth: Dp,
    val contentContainerMaxWidth: Dp,
    val contentWidthFraction: Float,
    val contentHorizontalPadding: Dp,
    val contentVerticalPadding: Dp,
    val topFadeHeight: Dp,
    val bottomFadeHeight: Dp,
    val logoWidthFraction: Float,
    val backgroundMotionStrength: Float,
)

@Composable
fun HomeHeroSection(
    items: List<MetaPreview>,
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
    sectionPadding: Dp? = null,
    listState: LazyListState? = null,
    stretchPx: () -> Float = { 0f },
    onItemClick: ((MetaPreview) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val pagerState = key(items.size) {
        rememberPagerState(
            initialPage = if (items.size > 1) {
                val middle = Int.MAX_VALUE / 2
                middle - middle % items.size
            } else {
                0
            },
            pageCount = { if (items.size > 1) Int.MAX_VALUE else items.size },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var pagerDragActive by remember { mutableStateOf(false) }
    val crossfade = remember(pagerState) { HeroCrossfade() }
    val autoScrollPage = pagerState.settledPage

    LaunchedEffect(pagerState) {
        pagerState.scrollToPage(pagerState.currentPage)
    }

    ScreenActivityEffect(pagerState) { active ->
        if (!active) {
            pagerState.stopScroll(MutatePriority.PreventUserInput)
            pagerState.scrollToPage(pagerState.currentPage)
        }
    }

    // Desktop: load the shown item's details in the background, so View Details opens a filled page
    // instead of a blank one while the addon and TMDB answer.
    ScreenActivityEffect(autoScrollPage, items.size) { active ->
        if (!active || !isDesktop) return@ScreenActivityEffect
        delay(HERO_DESKTOP_DETAILS_PREFETCH_DELAY_MS)
        val item = items[autoScrollPage % items.size]
        com.nuvio.app.features.details.MetaDetailsRepository.prefetch(item.type, item.id)
    }

    ScreenActivityEffect(autoScrollPage, items.size) { active ->
        if (!active || items.size <= 1) return@ScreenActivityEffect
        delay(HERO_AUTO_SCROLL_INTERVAL_MS)
        while (pagerState.isScrollInProgress || crossfade.active) {
            delay(100L)
        }

        val nextPage = pagerState.currentPage + 1
        if (isDesktop) {
            // Launched outside this effect: when the effect restarted mid-change, the change was
            // cut short, snapped back and started over, which showed as a flicker.
            coroutineScope.launch { crossfade.animateTo(pagerState, nextPage) }
        } else {
            pagerState.animateHeroToPage(nextPage)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .homeHeroPagerGesture(
                pagerState = pagerState,
                itemCount = items.size,
                coroutineScope = coroutineScope,
                onDragActiveChange = { pagerDragActive = it },
            )
            .then(
                if (isDesktop) {
                    // Not clipped: the backdrop's colour spreads below the hero, behind the first rows.
                    Modifier
                } else {
                    Modifier.clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                },
            ),
    ) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
            preferDesktopLayout = isDesktop,
        )
        val heroWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heroHeightPx = with(LocalDensity.current) { layout.heroHeight.toPx() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heroStretchHeight(layout.heroHeight, stretchPx),
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.01f },
            ) {
                Box(modifier = Modifier.fillMaxSize())
            }

            if (isDesktop) {
                DesktopHomeHeroFrame(
                    items = items,
                    pagerState = pagerState,
                    listState = listState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    heroHeightPx = heroHeightPx,
                    stretchPx = stretchPx,
                    includePagerNeighbors = pagerDragActive,
                    contentHorizontalPadding = maxOf(
                        sectionPadding ?: layout.contentHorizontalPadding,
                        layout.contentHorizontalPadding,
                    ),
                    coroutineScope = coroutineScope,
                    crossfade = crossfade,
                    onItemClick = onItemClick,
                )
            } else {
                DefaultHomeHeroFrame(
                    items = items,
                    pagerState = pagerState,
                    listState = listState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    heroHeightPx = heroHeightPx,
                    stretchPx = stretchPx,
                    includePagerNeighbors = pagerDragActive,
                    coroutineScope = coroutineScope,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun HeroBackgroundLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    desktopFrame: Boolean = false,
    crossfade: HeroCrossfade? = null,
) {
    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    if (desktopFrame && crossfade != null) {
        val host = LocalHomeHeroBackdropHost.current
        val backdrop: @Composable () -> Unit = {
            DesktopHeroBackdrops(
                items = items,
                // Worked out here, not captured from the hero: the hero updates this lambda a frame
                // late, and a stale page list dropped the new picture for a frame after each change.
                pages = crossfade.withPages(
                    rememberHeroLayerPages(
                        pagerState = pagerState,
                        itemCount = items.size,
                        includePagerNeighbors = includePagerNeighbors,
                    ),
                ),
                pagerState = pagerState,
                crossfade = crossfade,
                listState = listState,
                heroWidthPx = heroWidthPx,
                heroHeightPx = heroHeightPx,
                heroHeight = layout.heroHeight,
                stretchPx = stretchPx,
                modifier = if (host != null) {
                    // Follows the hero as the list scrolls; placement only, no layer.
                    Modifier.offset {
                        IntOffset(0, -heroScrollOffsetPx(listState, heroHeightPx).roundToInt())
                    }
                } else {
                    Modifier
                },
            )
        }
        if (host == null) {
            backdrop()
        } else {
            // Not cleared on dispose: the list drops the hero item when it scrolls away and may
            // bring it back without recomposing, which left the backdrop empty. While the hero is
            // off screen the backdrop is faded out anyway.
            SideEffect { host.content = backdrop }
        }
        return
    }
    layerPages.forEach { page ->
        val item = items[page % items.size]
        AsyncImage(
            model = item.banner ?: item.poster,
            contentDescription = item.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.heroHeight)
                .heroStretchZoom(stretchPx)
                .graphicsLayer {
                    val pageOffset = heroPageOffset(pagerState, page)
                    val scrollOffsetPx = heroScrollOffsetPx(listState, heroHeightPx)
                    val scrollScale = heroBackgroundScrollScale(scrollOffsetPx)

                    alpha = heroPageVisibility(pageOffset)
                    translationX = -pageOffset * heroWidthPx * HERO_BACKGROUND_PARALLAX
                    translationY = heroBackgroundScrollTranslationY(scrollOffsetPx)
                    scaleX = HERO_BACKGROUND_SCALE * scrollScale
                    scaleY = HERO_BACKGROUND_SCALE * scrollScale
                },
            alignment = if (layout.isTablet) Alignment.TopCenter else Alignment.Center,
            contentScale = ContentScale.Crop,
            desktopImageScaling = NuvioDesktopImageScaling.Disabled,
        )
    }
}

/**
 * Draws the desktop hero backdrop behind the whole Home list instead of inside the hero item. The
 * backdrop reaches below the hero, behind the first rows, and on desktop anything drawn outside
 * the hero item's layers was cut off in some frames while scrolling.
 */
internal class HomeHeroBackdropHost {
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
}

internal val LocalHomeHeroBackdropHost = staticCompositionLocalOf<HomeHeroBackdropHost?> { null }

/**
 * Desktop hero backdrop, composed like Kodi's Arctic Fuse "flixart" background:
 * - a soft gradient continuing the colours of the picture's left edge covers the hero and spills
 *   below it, behind the first rows, so there is no edge anywhere;
 * - the sharp 16:9 picture sits at the top right, reaching a little past the middle, with soft
 *   left and bottom edges;
 * - a background-coloured shade, darkest towards the left and below the picture, keeps the title
 *   and the rows readable and turns into the plain background where the spill ends.
 * Stretching a 16:9 backdrop across the much wider desktop hero used to crop away most of the
 * picture (often the faces), leaving only a band from the top.
 */
@Composable
private fun DesktopHeroBackdrops(
    items: List<MetaPreview>,
    pages: List<Int>,
    pagerState: PagerState,
    crossfade: HeroCrossfade,
    listState: LazyListState?,
    heroWidthPx: Float,
    heroHeightPx: Float,
    heroHeight: Dp,
    stretchPx: () -> Float,
    modifier: Modifier = Modifier,
) {
    val shadeColor = MaterialTheme.colorScheme.background
    // Used on the picture only; the spread behind it has the same shade baked in, dithered.
    val leftShade = Brush.horizontalGradient(colorStops = shadeColorStops(HeroDesktopLeftShadeCurve, shadeColor))
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val backdropWidth = maxOf(
            heroHeight * HERO_DESKTOP_BACKDROP_ASPECT_RATIO,
            maxWidth * HERO_DESKTOP_BACKDROP_MIN_WIDTH_FRACTION,
        ).coerceAtMost(maxWidth)
        val backdropHeight = backdropWidth / HERO_DESKTOP_BACKDROP_ASPECT_RATIO
        // Height of the whole composition: the hero plus the part that reaches behind the first rows.
        val spreadHeight = maxOf(backdropHeight, heroHeight) + heroHeight * HERO_DESKTOP_SPILL_FRACTION
        val density = LocalDensity.current
        val backdropWidthPx = with(density) { backdropWidth.toPx() }
        val backdropHeightPx = with(density) { backdropHeight.toPx() }
        val edgeStripPx = with(density) { HERO_DESKTOP_EDGE_STRIP_WIDTH.toPx() }
        val spreadSizePx = with(density) { Size(maxWidth.toPx(), spreadHeight.toPx()) }
        // Destination-in masks for the sharp picture: it fades in from the left (Arctic Fuse's
        // flixart.png fades over about a quarter of the width) and fades out over a short band at
        // its own bottom, so the whole picture stays visible.
        val leftEdgeMask = Brush.horizontalGradient(
            colorStops = smoothFadeStops(fadeEnd = 1f, fromAlpha = 0f, toAlpha = 1f),
            startX = 0f,
            endX = backdropWidthPx * HERO_DESKTOP_PICTURE_LEFT_FADE_FRACTION,
        )
        val bottomEdgeMask = Brush.verticalGradient(
            colorStops = smoothFadeStops(fadeEnd = 1f, fromAlpha = 1f, toAlpha = 0f),
            startY = backdropHeightPx * (1f - HERO_DESKTOP_PICTURE_BOTTOM_FADE_FRACTION),
            endY = backdropHeightPx,
        )

        pages.forEach { page ->
            key(page) {
                val item = items[page % items.size]
                val imageUrl = originalTmdbImageUrl(item.banner ?: item.poster)
                val incoming by remember(crossfade, pagerState, page) {
                    derivedStateOf { crossfade.isIncoming(pagerState, page) }
                }
                Box(
                    modifier = Modifier
                        // The picture fading in is drawn over the one it replaces.
                        .zIndex(if (incoming) 1f else 0f)
                        .fillMaxWidth()
                        .height(spreadHeight)
                        // Scrolls with the page and fades as the hero leaves, like Arctic Fuse; no
                        // scroll parallax. Pulling down past the top zooms in around the middle of the
                        // picture's top third, where faces and titles usually are.
                        .graphicsLayer {
                            val pageOffset = crossfade.offset(pagerState, page)
                            val scrollOffsetPx = heroScrollOffsetPx(listState, heroHeightPx)
                            val scrollFade =
                                1f - (scrollOffsetPx / heroHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                            // The picture being replaced stays opaque underneath while the new one
                            // fades in over it. With both half transparent, the dark background showed
                            // through in the middle of every change.
                            val visibility = smoothStep(heroPageVisibility(pageOffset))
                            alpha = when {
                                incoming -> visibility
                                visibility > 0f -> 1f
                                else -> 0f
                            } * scrollFade
                            val stretchZoom = 1f + stretchPx().coerceAtLeast(0f) *
                                HERO_DESKTOP_STRETCH_ZOOM / heroHeightPx.coerceAtLeast(1f)
                            transformOrigin = TransformOrigin(
                                1f - backdropWidthPx / 2f / size.width.coerceAtLeast(1f),
                                backdropHeightPx / 6f / size.height.coerceAtLeast(1f),
                            )
                            scaleX = stretchZoom
                            scaleY = stretchZoom
                            // No sideways parallax: the dissolve is in place. Shifting the layers
                            // uncovered their edges as dark and bright strips.
                        },
                ) {
                    // One load feeds both layers. No crossfade: the picture and its spread show up
                    // together once the spread is ready.
                    val platformContext = LocalPlatformContext.current
                    val request = remember(imageUrl, backdropWidthPx, backdropHeightPx) {
                        ImageRequest.Builder(platformContext)
                            .data(imageUrl)
                            .size(backdropWidthPx.roundToInt(), backdropHeightPx.roundToInt())
                            .crossfade(false)
                            .build()
                    }
                    val picture = rememberAsyncImagePainter(model = request)
                    val pictureState by picture.state.collectAsState()
                    val loadedResult = pictureState as? AsyncImagePainter.State.Success
                    val loadedPicture = loadedResult?.painter
                    // Blurred once per picture into a small bitmap. A blur effect on the full-size
                    // layer was redone every frame while the hero moved or faded, and fell behind.
                    // Kept across compositions: the hero is dropped when scrolled out of view, and
                    // rebuilding the spread as it came back made the scroll stutter.
                    // Keyed by the image actually loaded, not the page's current URL: while a page
                    // switches items, the painter can still hold the previous picture, and its
                    // spread was cached under the new item (a red poster tinted another title).
                    val spreadKey = HeroSpreadKey(
                        loadedResult?.result?.request?.data?.toString(),
                        spreadSizePx,
                        backdropWidthPx,
                        shadeColor,
                    )
                    // Rendered off the UI thread. Done during composition it held up the frames of the
                    // item change, and the new spread arrived only after the old one was gone, leaving
                    // the left side black for a moment.
                    var spread by remember(loadedPicture, spreadKey) {
                        mutableStateOf(loadedPicture?.let { HeroSpreadCache.get(spreadKey) })
                    }
                    LaunchedEffect(loadedPicture, spreadKey) {
                        val painter = loadedPicture ?: return@LaunchedEffect
                        if (spread != null) return@LaunchedEffect
                        val rendered = withContext(Dispatchers.Default) {
                            renderBlurredSpread(
                                painter = painter,
                                spreadSize = spreadSizePx,
                                pictureWidth = backdropWidthPx,
                                pictureHeight = backdropHeightPx,
                                strip = edgeStripPx,
                                gradientColumns = HERO_DESKTOP_SPREAD_GRADIENT_COLUMNS,
                                shadeColor = shadeColor,
                                density = density,
                            )
                        }
                        HeroSpreadCache.put(spreadKey, rendered)
                        spread = rendered
                    }
                    // The last picture and spread shown for this item, kept while a new size is
                    // loaded and rendered (going fullscreen), so the hero doesn't go black meanwhile.
                    var shownPicture by remember(imageUrl) { mutableStateOf<Painter?>(null) }
                    var shownSpread by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
                    if (loadedPicture != null && spread != null) {
                        shownPicture = loadedPicture
                        shownSpread = spread
                    }
                    val ready = shownSpread != null
                    DisposableEffect(crossfade, page, ready) {
                        if (ready) crossfade.readyPages += page
                        onDispose { crossfade.readyPages -= page }
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        shownSpread?.let {
                            drawImage(
                                image = it,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                                // Plain bilinear: a sharpening filter would bring the dither grain out.
                                filterQuality = FilterQuality.Low,
                            )
                        }
                    }
                    if (ready) Image(
                        painter = shownPicture ?: picture,
                        contentDescription = item.name,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .width(backdropWidth)
                            .height(backdropHeight)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                // Masks reach past the edges: when the hero is zoomed, the edge falls
                                // between pixels, and a mask ending exactly there left the unmasked
                                // edge column showing as a bright line.
                                val bleed = HERO_DESKTOP_MASK_BLEED_PX
                                val maskTopLeft = Offset(-bleed, -bleed)
                                val maskSize = Size(size.width + bleed * 2, size.height + bleed * 2)
                                drawRect(leftEdgeMask, maskTopLeft, maskSize, blendMode = BlendMode.DstIn)
                                drawRect(bottomEdgeMask, maskTopLeft, maskSize, blendMode = BlendMode.DstIn)
                                // The left shade, laid out over the whole spread, kept to the picture's
                                // own pixels (SrcAtop) so the spread is not shaded twice. The bottom
                                // shade starts below the picture, so it is left off here.
                                translate(left = size.width - spreadSizePx.width) {
                                    drawRect(brush = leftShade, size = spreadSizePx, blendMode = BlendMode.SrcAtop)
                                }
                            },
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

private data class HeroSpreadKey(
    val imageUrl: String?,
    val spreadSize: Size,
    val pictureWidth: Float,
    val shadeColor: Color,
)

/** The most recently used spreads (about 2.5 MB each), so a returning hero shows at once. */
private object HeroSpreadCache {
    private const val MAX_ENTRIES = 8
    private val entries = LinkedHashMap<HeroSpreadKey, ImageBitmap>()

    fun get(key: HeroSpreadKey): ImageBitmap? =
        entries.remove(key)?.also { entries[key] = it }

    fun put(key: HeroSpreadKey, bitmap: ImageBitmap) {
        entries.remove(key)
        entries[key] = bitmap
        while (entries.size > MAX_ENTRIES) entries.remove(entries.keys.first())
    }
}

/**
 * Renders [drawEdgeSpread] into a small bitmap, averages it down and blurs it to about
 * [gradientColumns] bands of colour across, then shades it; drawn back at full size, the averaged
 * colours become one soft gradient, like Arctic Fuse's background.
 */
private fun renderBlurredSpread(
    painter: Painter,
    spreadSize: Size,
    pictureWidth: Float,
    pictureHeight: Float,
    strip: Float,
    gradientColumns: Int,
    shadeColor: Color,
    density: Density,
): ImageBitmap {
    val scale = HERO_DESKTOP_SPREAD_RENDER_WIDTH / spreadSize.width
    var bitmap = drawIntoBitmap(
        width = HERO_DESKTOP_SPREAD_RENDER_WIDTH,
        height = (spreadSize.height * scale).roundToInt(),
        density = density,
    ) {
        drawEdgeSpread(painter, pictureWidth * scale, pictureHeight * scale, strip * scale)
    }
    while (bitmap.width / 2 >= HERO_DESKTOP_SPREAD_SAMPLE_WIDTH) {
        val source = bitmap
        // Exact halving with bilinear sampling averages each 2x2 block.
        bitmap = drawIntoBitmap(source.width / 2, source.height / 2, density) {
            drawImage(image = source, dstSize = IntSize(source.width / 2, source.height / 2))
        }
    }
    // Smoothed at this size rather than reduced further: a handful of pixels stretched across the
    // screen showed as large blocks.
    val planes = bitmap.gaussianBlurredPlanes(sigma = bitmap.width / (gradientColumns * 1.5f))
    return composeShadedSpread(planes, bitmap.width, bitmap.height, spreadSize, shadeColor)
}

/**
 * Scales the blurred planes up to [HERO_DESKTOP_SPREAD_OUTPUT_WIDTH], applies the shades and
 * dithers before rounding to 8 bits. Smooth dark gradients rounded directly showed visible bands;
 * the GPU only stretches this result a few times further.
 */
private fun composeShadedSpread(
    planes: Array<FloatArray>,
    width: Int,
    height: Int,
    spreadSize: Size,
    shadeColor: Color,
): ImageBitmap {
    val outWidth = HERO_DESKTOP_SPREAD_OUTPUT_WIDTH
    val outHeight = (outWidth * spreadSize.height / spreadSize.width).roundToInt().coerceAtLeast(1)
    val shade = floatArrayOf(shadeColor.red * 255f, shadeColor.green * 255f, shadeColor.blue * 255f)
    val leftAlpha = FloatArray(outWidth) { x -> HeroDesktopLeftShadeCurve.at((x + 0.5f) / outWidth) }
    val pixels = IntArray(outWidth * outHeight)
    for (y in 0 until outHeight) {
        val bottomAlpha = HeroDesktopBottomShadeCurve.at((y + 0.5f) / outHeight)
        val sy = ((y + 0.5f) * height / outHeight - 0.5f).coerceIn(0f, (height - 1).toFloat())
        val y0 = sy.toInt()
        val y1 = minOf(y0 + 1, height - 1)
        val fy = sy - y0
        for (x in 0 until outWidth) {
            val sx = ((x + 0.5f) * width / outWidth - 0.5f).coerceIn(0f, (width - 1).toFloat())
            val x0 = sx.toInt()
            val x1 = minOf(x0 + 1, width - 1)
            val fx = sx - x0
            var argb = 0xFF shl 24
            for (channel in 0 until 3) {
                val plane = planes[channel]
                val top = plane[y0 * width + x0] * (1f - fx) + plane[y0 * width + x1] * fx
                val bottom = plane[y1 * width + x0] * (1f - fx) + plane[y1 * width + x1] * fx
                var value = top * (1f - fy) + bottom * fy
                value += (shade[channel] - value) * leftAlpha[x]
                value += (shade[channel] - value) * bottomAlpha
                // Triangular noise of up to one step, so the rounding error is spread out as grain.
                value += ditherNoise(x, y, channel) + ditherNoise(x, y, channel + 3) - 1f
                argb = argb or ((value + 0.5f).toInt().coerceIn(0, 255) shl (16 - channel * 8))
            }
            pixels[y * outWidth + x] = argb
        }
    }
    return imageBitmapFromArgb(pixels, outWidth, outHeight)
}

/** A repeatable value in [0, 1) for each pixel and channel. */
private fun ditherNoise(x: Int, y: Int, salt: Int): Float {
    var hash = x * 374761393 + y * 668265263 + salt * 1274126177
    hash = (hash xor (hash ushr 13)) * 1103515245
    hash = hash xor (hash ushr 16)
    return (hash and 0xFFFF) / 65536f
}

/**
 * Black stops easing from [fromAlpha] to [toAlpha] between 0 and [fadeEnd] (smoothstep). A fade
 * that starts or stops at an angle shows a line there; this one starts and ends flat.
 */
private fun smoothFadeStops(fadeEnd: Float, fromAlpha: Float, toAlpha: Float): Array<Pair<Float, Color>> {
    val steps = 16
    return Array(steps + 1) { i ->
        val t = i / steps.toFloat()
        val eased = t * t * (3f - 2f * t)
        fadeEnd * t to Color.Black.copy(alpha = fromAlpha + (toAlpha - fromAlpha) * eased)
    }
}

/**
 * A shade profile sampled evenly with its corners rounded off. Where the slope of a fade changes
 * sharply, the eye sees a light or dark line, so the straight segments between stops are smoothed.
 */
private class ShadeCurve(stops: Array<Pair<Float, Float>>) {
    val samples: FloatArray

    init {
        val count = 65
        val raw = FloatArray(count) { i -> shadeAlphaAt(stops, i / (count - 1f)) }
        val sigma = count * 0.06f
        val radius = ceil(sigma * 3f).toInt()
        samples = FloatArray(count) { i ->
            var sum = 0f
            var weights = 0f
            for (offset in -radius..radius) {
                val weight = exp(-(offset * offset) / (2f * sigma * sigma))
                sum += raw[(i + offset).coerceIn(0, count - 1)] * weight
                weights += weight
            }
            sum / weights
        }
    }

    fun at(position: Float): Float {
        val scaled = position.coerceIn(0f, 1f) * (samples.size - 1)
        val index = scaled.toInt().coerceAtMost(samples.size - 2)
        val fraction = scaled - index
        return samples[index] + (samples[index + 1] - samples[index]) * fraction
    }
}

private val HeroDesktopLeftShadeCurve = ShadeCurve(HERO_DESKTOP_LEFT_SHADE)
private val HeroDesktopBottomShadeCurve = ShadeCurve(HERO_DESKTOP_BOTTOM_SHADE)

private fun shadeColorStops(curve: ShadeCurve, color: Color): Array<Pair<Float, Color>> =
    Array(curve.samples.size) { i -> i / (curve.samples.size - 1f) to color.copy(alpha = curve.samples[i]) }

private fun shadeAlphaAt(stops: Array<Pair<Float, Float>>, position: Float): Float {
    if (position <= stops.first().first) return stops.first().second
    for (i in 1 until stops.size) {
        val (end, endAlpha) = stops[i]
        if (position <= end) {
            val (start, startAlpha) = stops[i - 1]
            return startAlpha + (endAlpha - startAlpha) * (position - start) / (end - start)
        }
    }
    return stops.last().second
}

private fun ImageBitmap.gaussianBlurredPlanes(sigma: Float): Array<FloatArray> {
    val w = width
    val h = height
    val pixels = IntArray(w * h)
    readPixels(pixels)
    val radius = ceil(sigma * 3f).toInt()
    val kernel = FloatArray(radius * 2 + 1) { i ->
        val x = (i - radius).toFloat()
        exp(-x * x / (2f * sigma * sigma))
    }
    val kernelSum = kernel.sum()
    for (i in kernel.indices) kernel[i] /= kernelSum

    // Red, green and blue planes; the spread is opaque everywhere.
    var planes = Array(3) { channel ->
        val shift = 16 - channel * 8
        FloatArray(w * h) { i -> ((pixels[i] ushr shift) and 0xFF).toFloat() }
    }
    fun pass(horizontal: Boolean) {
        planes = Array(3) { channel ->
            val source = planes[channel]
            FloatArray(w * h) { i ->
                val x = i % w
                val y = i / w
                var value = 0f
                for (k in kernel.indices) {
                    val offset = k - radius
                    val sx = if (horizontal) (x + offset).coerceIn(0, w - 1) else x
                    val sy = if (horizontal) y else (y + offset).coerceIn(0, h - 1)
                    value += source[sy * w + sx] * kernel[k]
                }
                value
            }
        }
    }
    pass(horizontal = true)
    pass(horizontal = false)
    return planes
}

private fun drawIntoBitmap(
    width: Int,
    height: Int,
    density: Density,
    block: DrawScope.() -> Unit,
): ImageBitmap {
    val bitmap = ImageBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1))
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = androidx.compose.ui.graphics.Canvas(bitmap),
        size = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        block = block,
    )
    return bitmap
}

/**
 * Stretches the picture's leftmost columns across the whole width, and the colour of its
 * bottom-left corner down below it, like Arctic Fuse's background: each row continues the colour
 * the picture has at its left edge. The rest of the picture is left out, so dark details near its
 * bottom (hair, clothes) no longer show up as blots once blurred.
 */
private fun DrawScope.drawEdgeSpread(
    painter: Painter,
    pictureWidth: Float,
    pictureHeight: Float,
    strip: Float,
) {
    val left = size.width - pictureWidth
    fun drawPicture() = translate(left = left) {
        with(painter) { draw(Size(pictureWidth, pictureHeight)) }
    }

    withTransform({
        scale(scaleX = size.width / strip, scaleY = 1f, pivot = Offset.Zero)
        translate(left = -left)
        clipRect(left, 0f, left + strip, pictureHeight)
    }) { drawPicture() }
    if (size.height > pictureHeight) {
        withTransform({
            translate(top = pictureHeight)
            scale(scaleX = size.width / strip, scaleY = (size.height - pictureHeight) / strip, pivot = Offset.Zero)
            translate(left = -left, top = -(pictureHeight - strip))
            clipRect(left, pictureHeight - strip, left + strip, pictureHeight)
        }) { drawPicture() }
    }
}

@Composable
private fun HeroContentLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    onItemClick: ((MetaPreview) -> Unit)?,
    includePagerNeighbors: Boolean,
) {
    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    layerPages.forEach { page ->
        Box(
            modifier = Modifier.graphicsLayer {
                val pageOffset = heroPageOffset(pagerState, page)

                alpha = heroPageVisibility(pageOffset)
                translationX = -pageOffset * heroWidthPx * HERO_CONTENT_PARALLAX
            },
        ) {
            HeroContentBlock(
                item = items[page % items.size],
                layout = layout,
                onItemClick = onItemClick,
            )
        }
    }
}

@Composable
private fun rememberHeroLayerPages(
    pagerState: PagerState,
    itemCount: Int,
    includePagerNeighbors: Boolean,
): List<Int> {
    if (itemCount <= 0) return emptyList()

    val currentPage = pagerState.currentPage
    val includeNeighbors = includePagerNeighbors || pagerState.isScrollInProgress
    return remember(currentPage, includeNeighbors, itemCount) {
        heroLayerPages(
            currentPage = currentPage,
            pageCount = pagerState.pageCount,
            includeNeighbors = includeNeighbors,
        )
    }
}

private fun heroLayerPages(
    currentPage: Int,
    pageCount: Int,
    includeNeighbors: Boolean,
): List<Int> {
    if (!includeNeighbors || pageCount == 1) return listOf(currentPage)

    val neighbors = listOf(currentPage - 1, currentPage + 1)
        .map { page -> page.coerceIn(0, pageCount - 1) }
        .filter { page -> page != currentPage }
        .distinct()
    return neighbors + currentPage
}

@Composable
private fun HeroDesktopContentLayers(
    items: List<MetaPreview>,
    pagerState: PagerState,
    crossfade: HeroCrossfade,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    onItemClick: ((MetaPreview) -> Unit)?,
    includePagerNeighbors: Boolean,
) {
    val layerPages = rememberHeroLayerPages(
        pagerState = pagerState,
        itemCount = items.size,
        includePagerNeighbors = includePagerNeighbors,
    )

    // The text slides and fades per item; the View Details button stays put below it, the same for
    // every item, instead of moving with the text and jumping with each description's length.
    Column(horizontalAlignment = Alignment.Start) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
            crossfade.withPages(layerPages).forEach { page ->
                val incoming by remember(crossfade, pagerState, page) {
                    derivedStateOf { crossfade.isIncoming(pagerState, page) }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val pageOffset = crossfade.offset(pagerState, page)
                            val visibility = heroPageVisibility(pageOffset)
                            translationX = -pageOffset * HERO_DESKTOP_TEXT_SLIDE.toPx()
                            if (incoming) {
                                val shown = smoothStep(
                                    (visibility - HERO_DESKTOP_TEXT_IN_START) / (1f - HERO_DESKTOP_TEXT_IN_START),
                                )
                                alpha = shown
                            } else {
                                alpha = smoothStep(
                                    (visibility - (1f - HERO_DESKTOP_TEXT_OUT_END)) / HERO_DESKTOP_TEXT_OUT_END,
                                )
                            }
                        },
                ) {
                    DesktopHeroContentBlock(
                        item = items[page % items.size],
                        layout = layout,
                        onItemClick = onItemClick,
                        showButton = false,
                    )
                }
            }
        }
        if (onItemClick != null) {
            Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))
            DesktopHeroViewDetailsButton {
                val page = crossfade.to ?: pagerState.currentPage
                onItemClick(items[page % items.size])
            }
        }
    }
}

@Composable
private fun DefaultHomeHeroFrame(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    coroutineScope: CoroutineScope,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        HeroBackgroundLayers(
            items = items,
            pagerState = pagerState,
            listState = listState,
            layout = layout,
            heroWidthPx = heroWidthPx,
            heroHeightPx = heroHeightPx,
            stretchPx = stretchPx,
            includePagerNeighbors = includePagerNeighbors,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.02f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.34f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                        ),
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.bottomFadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    horizontal = layout.contentHorizontalPadding,
                    vertical = layout.contentVerticalPadding,
                ),
            horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(layout.contentWidthFraction)
                    .widthIn(max = layout.contentMaxWidth),
                contentAlignment = if (layout.isTablet) Alignment.CenterStart else Alignment.BottomStart,
            ) {
                HeroContentLayers(
                    items = items,
                    pagerState = pagerState,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    onItemClick = onItemClick,
                    includePagerNeighbors = includePagerNeighbors,
                )
            }

            if (!layout.isTablet) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .clickable(enabled = onItemClick != null) {
                            onItemClick?.invoke(currentHeroItem(items, pagerState))
                        },
                    color = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(40.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.home_view_details),
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            HeroPageIndicatorRow(
                itemCount = items.size,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                modifier = Modifier.padding(top = if (layout.isTablet) 14.dp else 12.dp),
            )
        }
    }
}

@Composable
private fun DesktopHomeHeroFrame(
    items: List<MetaPreview>,
    pagerState: PagerState,
    listState: LazyListState?,
    layout: HomeHeroLayout,
    heroWidthPx: Float,
    heroHeightPx: Float,
    stretchPx: () -> Float,
    includePagerNeighbors: Boolean,
    contentHorizontalPadding: Dp,
    coroutineScope: CoroutineScope,
    crossfade: HeroCrossfade,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    val colorScheme = MaterialTheme.colorScheme
    val opacity = NuvioTokens.Opacity
    val space = NuvioTokens.Space
    val backgroundColor = colorScheme.background

    // No background of its own: the backdrop is drawn behind the Home list (HomeHeroBackdropHost),
    // on the screen's background.
    Box(modifier = Modifier.fillMaxSize()) {
        HeroBackgroundLayers(
            items = items,
            pagerState = pagerState,
            listState = listState,
            layout = layout,
            heroWidthPx = heroWidthPx,
            heroHeightPx = heroHeightPx,
            stretchPx = stretchPx,
            includePagerNeighbors = includePagerNeighbors,
            desktopFrame = true,
            crossfade = crossfade,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.topFadeHeight)
                .align(Alignment.TopCenter)
                .background(
                    // Eased out to zero: a straight fade ending abruptly over the bright backdrop
                    // showed as a light line where it stopped.
                    Brush.verticalGradient(
                        colorStops = Array(9) { i ->
                            val t = i / 8f
                            t to backgroundColor.copy(alpha = opacity.overlayHeavy * (1f - t) * (1f - t))
                        },
                    ),
                ),
        )

        // The text scrim and the bottom fade are drawn with the backdrop spread (DesktopHeroBackdrops),
        // which reaches below the hero, so they end where the spread ends instead of at the hero edge.

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = layout.contentContainerMaxWidth)
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = contentHorizontalPadding,
                        end = space.s32,
                        bottom = layout.contentVerticalPadding,
                    )
                    .fillMaxWidth(layout.contentWidthFraction)
                    .widthIn(max = layout.contentMaxWidth),
                contentAlignment = Alignment.BottomStart,
            ) {
                HeroDesktopContentLayers(
                    items = items,
                    pagerState = pagerState,
                    crossfade = crossfade,
                    layout = layout,
                    heroWidthPx = heroWidthPx,
                    onItemClick = onItemClick,
                    includePagerNeighbors = includePagerNeighbors,
                )
            }

            if (isFullscreenActionSupported) {
                FullscreenActionButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(
                            top = space.s32,
                            end = contentHorizontalPadding,
                        ),
                    buttonSize = 48.dp,
                    iconSize = 24.dp,
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.82f),
                    contentColor = colorScheme.onSurface,
                )
            }

            HeroPageIndicatorRow(
                itemCount = items.size,
                pagerState = pagerState,
                coroutineScope = coroutineScope,
                crossfade = crossfade,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = contentHorizontalPadding,
                        bottom = space.s40,
                    )
                    // A dark capsule like the navigation bar's, so the white dots stay visible
                    // over a bright picture.
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFF1C1C1E).copy(alpha = 0.55f))
                    .padding(horizontal = space.s12, vertical = space.s8),
            )
        }
    }
}

@Composable
private fun HeroPageIndicatorRow(
    itemCount: Int,
    pagerState: PagerState,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    crossfade: HeroCrossfade? = null,
) {
    if (itemCount <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(itemCount) { index ->
            val page = heroPageForItem(pagerState.currentPage, index, itemCount)
            val activeFraction = heroPageVisibility(
                crossfade?.offset(pagerState, page) ?: heroPageOffset(pagerState, page),
            )
            Box(
                modifier = Modifier
                    .clickable {
                        coroutineScope.launch {
                            val target = heroPageForItem(pagerState.currentPage, index, itemCount)
                            if (crossfade != null) {
                                crossfade.animateTo(pagerState, target)
                            } else {
                                pagerState.animateHeroToPage(target)
                            }
                        }
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .graphicsLayer {
                        alpha = 0.35f + (0.57f * activeFraction)
                    }
                    .width(8.dp + (24.dp * activeFraction))
                    .height(8.dp),
            )
        }
    }
}

internal fun heroPageForItem(currentPage: Int, itemIndex: Int, itemCount: Int): Int {
    val page = currentPage.toLong() - currentPage % itemCount + itemIndex
    return listOf(page - itemCount, page, page + itemCount)
        .filter { it in 0L until Int.MAX_VALUE.toLong() }
        .minBy { abs(it - currentPage) }
        .toInt()
}

private fun heroPageOffset(
    pagerState: PagerState,
    page: Int,
): Float = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

private fun heroPageVisibility(
    pagerState: PagerState,
    page: Int,
): Float = heroPageVisibility(heroPageOffset(pagerState, page))

private fun heroPageVisibility(pageOffset: Float): Float = (1f - abs(pageOffset)).coerceIn(0f, 1f)

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Desktop item changes from the timer and the dots: a dissolve straight from one item to the other.
 * Scrolling the pager there instead ran through every item in between (a dot can be several items
 * away) and jumped where the pager skipped ahead. The pager moves to the new page once the dissolve
 * is done; dragging still moves the pager directly.
 */
@Stable
private class HeroCrossfade {
    var from by mutableStateOf<Int?>(null)
        private set
    var to by mutableStateOf<Int?>(null)
        private set
    private val progress = Animatable(0f)
    private val mutex = MutatorMutex()
    // 1 when moving to a later item, -1 to an earlier one (a dot to the left).
    private var direction by mutableStateOf(1f)

    /** Pages whose picture and spread are ready to show. */
    val readyPages = mutableStateSetOf<Int>()

    val active: Boolean get() = to != null

    /** Like [heroPageOffset]: 0 for the item in place, towards ±1 for the one leaving or arriving. */
    fun offset(pagerState: PagerState, page: Int): Float {
        val target = to ?: return heroPageOffset(pagerState, page)
        return when (page) {
            target -> -(1f - progress.value) * direction
            from -> progress.value * direction
            else -> 1f
        }
    }

    fun isIncoming(pagerState: PagerState, page: Int): Boolean {
        val target = to
        return if (target != null) page == target else heroPageVisibility(pagerState, page) < 0.5f
    }

    fun withPages(pages: List<Int>): List<Int> = (pages + listOfNotNull(from, to)).distinct()

    suspend fun animateTo(pagerState: PagerState, page: Int) = mutex.mutate {
        val start = pagerState.currentPage
        if (page == start) return@mutate
        try {
            direction = if (page > start) 1f else -1f
            from = start
            to = page
            progress.snapTo(0f)
            // The new page is composed (invisible) from here on, so its picture loads and its
            // spread renders before the dissolve starts, instead of popping in half way.
            withTimeoutOrNull(HERO_DESKTOP_PRELOAD_TIMEOUT_MS) {
                snapshotFlow { page in readyPages }.first { it }
            }
            progress.animateTo(1f, HeroDesktopPageChangeSpec)
        } finally {
            // Also when interrupted (another dot, leaving the screen): settle on whichever item
            // was showing more.
            withContext(NonCancellable) {
                pagerState.scrollToPage(if (progress.value >= 0.5f) page else start)
                from = null
                to = null
                progress.snapTo(0f)
            }
        }
    }
}

private suspend fun PagerState.animateHeroToPage(page: Int) {
    if (isDesktop) {
        animateScrollToPage(page, animationSpec = HeroDesktopPageChangeSpec)
    } else {
        animateScrollToPage(page)
    }
}

private fun currentHeroItem(
    items: List<MetaPreview>,
    pagerState: PagerState,
): MetaPreview {
    val currentPage = pagerState.currentPage
    val currentVisiblePages = heroLayerPages(
        currentPage = currentPage,
        pageCount = pagerState.pageCount,
        includeNeighbors = true,
    )
    val selectedPage = currentVisiblePages.maxBy { page ->
        heroPageVisibility(pagerState, page)
    }
    return items[selectedPage % items.size]
}

@Composable
private fun HeroPageIndicatorDot(
    pagerState: PagerState,
    page: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground)
            .graphicsLayer {
                val activeFraction = heroPageVisibility(pagerState, page)
                alpha = 0.35f + (0.57f * activeFraction)
            }
            .heroPageIndicatorSize(pagerState = pagerState, page = page),
    )
}

private fun Modifier.heroPageIndicatorSize(
    pagerState: PagerState,
    page: Int,
): Modifier = layout { measurable, constraints ->
    val activeFraction = heroPageVisibility(pagerState, page)
    val widthPx = (8.dp.toPx() + (24.dp.toPx() * activeFraction)).roundToInt()
    val heightPx = 8.dp.roundToPx()
    val constrainedWidth = widthPx.coerceIn(constraints.minWidth, constraints.maxWidth)
    val constrainedHeight = heightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constrainedWidth,
            maxWidth = constrainedWidth,
            minHeight = constrainedHeight,
            maxHeight = constrainedHeight,
        ),
    )

    layout(constrainedWidth, constrainedHeight) {
        placeable.place(0, 0)
    }
}

@Composable
fun HomeHeroReservedSpace(
    modifier: Modifier = Modifier,
    viewportHeight: Dp? = null,
    mobileBelowSectionHeightHint: Dp? = null,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
    ) {
        val layout = homeHeroLayout(
            maxWidthDp = maxWidth.value,
            viewportHeightDp = viewportHeight?.value,
            mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHint?.value,
            preferDesktopLayout = isDesktop,
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(layout.heroHeight),
        )
    }
}

@Composable
private fun HeroContentBlock(
    item: MetaPreview,
    layout: HomeHeroLayout,
    onItemClick: ((MetaPreview) -> Unit)?,
) {
    var logoLoadError by remember(item.type, item.id, item.logo) {
        mutableStateOf(false)
    }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (layout.isTablet) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        if (logoUrl != null && !logoLoadError) {
            AsyncImage(
                model = logoUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth(layout.logoWidthFraction)
                    .aspectRatio(2.6f)
                    .clickable(enabled = onItemClick != null) {
                        onItemClick?.invoke(item)
                    },
                alignment = if (layout.isTablet) Alignment.CenterStart else Alignment.Center,
                contentScale = ContentScale.Fit,
                onError = { logoLoadError = true },
            )
        } else {
            Text(
                text = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onItemClick != null) {
                        onItemClick?.invoke(item)
                    },
                style = if (layout.isTablet) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.displaySmall
                },
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                textAlign = if (layout.isTablet) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (layout.isTablet) {
                Arrangement.spacedBy(8.dp, Alignment.Start)
            } else {
                Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroMetaText(text = item.type.replaceFirstChar(Char::uppercase))
            item.genres.firstOrNull()?.let { genre ->
                HeroMetaDot()
                HeroMetaText(text = genre)
            }
            item.releaseInfo?.takeIf { it.isNotBlank() }?.let { info ->
                HeroMetaDot()
                HeroMetaText(text = formatReleaseDateForDisplay(info))
            }
        }
    }
}

@Composable
private fun DesktopHeroContentBlock(
    item: MetaPreview,
    layout: HomeHeroLayout,
    onItemClick: ((MetaPreview) -> Unit)?,
    showButton: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme
    var logoLoadError by remember(item.type, item.id, item.logo) {
        mutableStateOf(false)
    }
    val logoUrl = item.logo?.takeIf { it.isNotBlank() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = onItemClick != null,
            ) {
                onItemClick?.invoke(item)
            },
        horizontalAlignment = Alignment.Start,
    ) {
        if (logoUrl != null && !logoLoadError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(desktopHeroLogoSlotHeight(layout)),
                contentAlignment = Alignment.CenterStart,
            ) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth(desktopHeroLogoWidthFraction(layout))
                        .fillMaxHeight(),
                    alignment = Alignment.CenterStart,
                    contentScale = ContentScale.Fit,
                    clipToBounds = false,
                    onError = { logoLoadError = true },
                )
            }
        } else {
            Text(
                text = item.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = NuvioTokens.Type.displayMd,
                    lineHeight = NuvioTokens.LineHeight.displayMd,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = NuvioTokens.LetterSpacing.none,
                ),
                color = colorScheme.onBackground,
                textAlign = TextAlign.Start,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val genreText = desktopHeroGenreText(item)
        if (genreText.isNotBlank()) {
            Spacer(modifier = Modifier.height(NuvioTokens.Space.s12))
            Text(
                text = genreText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = NuvioTokens.Type.bodyMd,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = NuvioTokens.LetterSpacing.none,
                ),
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        item.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(modifier = Modifier.height(NuvioTokens.Space.s16))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = NuvioTokens.Type.titleMd,
                    lineHeight = NuvioTokens.LineHeight.headline,
                    letterSpacing = NuvioTokens.LetterSpacing.none,
                ),
                color = colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (onItemClick != null && showButton) {
            Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))
            DesktopHeroViewDetailsButton { onItemClick(item) }
        }
    }
}

@Composable
private fun DesktopHeroViewDetailsButton(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(NuvioTokens.Space.s12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A glass button over the backdrop: translucent, with the Card Depth edge and sheen.
        val shape = RoundedCornerShape(40.dp)
        Surface(
            modifier = Modifier
                .height(48.dp)
                .nuvioCardDepth(shape, NuvioCardDepthSurface.Controls, fallbackBorderAlpha = 0.22f)
                .clip(shape)
                .clickable(onClick = onClick),
            color = Color.White.copy(alpha = 0.16f),
            contentColor = Color.White,
            shape = shape,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.home_view_details),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun desktopHeroLogoWidthFraction(layout: HomeHeroLayout): Float =
    when {
        layout.contentMaxWidth >= 640.dp -> 0.74f
        layout.contentMaxWidth >= 520.dp -> 0.74f
        else -> 0.8f
    }

private fun desktopHeroLogoSlotHeight(layout: HomeHeroLayout): Dp =
    when {
        layout.contentMaxWidth >= 640.dp -> 180.dp
        layout.contentMaxWidth >= 520.dp -> 168.dp
        else -> 156.dp
    }

private fun desktopHeroGenreText(item: MetaPreview): String =
    item.genres
        .take(3)
        .joinToString(" • ")
        .ifBlank { item.type.replaceFirstChar(Char::uppercase) }

@Composable
private fun HeroMetaText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

internal fun homeHeroLayout(
    maxWidthDp: Float,
    viewportHeightDp: Float? = null,
    mobileBelowSectionHeightHintDp: Float? = null,
    preferDesktopLayout: Boolean = false,
): HomeHeroLayout {
    if (preferDesktopLayout) {
        val heroHeight = desktopHeroHeight(
            maxWidthDp = maxWidthDp,
            viewportHeightDp = viewportHeightDp,
        )
        val ultrawideProgress = ultrawideViewportProgress(
            widthDp = maxWidthDp,
            heightDp = viewportHeightDp,
        )

        val standardHorizontalPadding = homeSectionHorizontalPaddingForWidth(maxWidthDp).value

        return HomeHeroLayout(
            isTablet = true,
            heroHeight = heroHeight,
            contentMaxWidth = 760.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            // Text ends about where the centred navigation bar begins.
            contentWidthFraction = 0.45f,
            contentHorizontalPadding = lerp(
                start = standardHorizontalPadding,
                stop = DESKTOP_HERO_ULTRAWIDE_HORIZONTAL_PADDING_DP,
                fraction = ultrawideProgress,
            ).dp,
            contentVerticalPadding = lerp(
                start = 40f,
                stop = DESKTOP_HERO_ULTRAWIDE_BOTTOM_PADDING_DP,
                fraction = ultrawideProgress,
            ).dp,
            topFadeHeight = DESKTOP_HERO_TOP_FADE_HEIGHT_DP.dp,
            bottomFadeHeight = DESKTOP_HERO_BOTTOM_FADE_HEIGHT_DP.dp,
            logoWidthFraction = 0.74f,
            backgroundMotionStrength = 1f - ultrawideProgress,
        )
    }

    return when {
        maxWidthDp >= 1200f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.42f).dp.coerceIn(360.dp, 440.dp),
            contentMaxWidth = 640.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.56f,
            contentHorizontalPadding = 56.dp,
            contentVerticalPadding = 22.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 190.dp,
            logoWidthFraction = 0.58f,
            backgroundMotionStrength = 1f,
        )
        maxWidthDp >= 840f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.46f).dp.coerceIn(340.dp, 420.dp),
            contentMaxWidth = 560.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.62f,
            contentHorizontalPadding = 40.dp,
            contentVerticalPadding = 20.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 180.dp,
            logoWidthFraction = 0.56f,
            backgroundMotionStrength = 1f,
        )
        maxWidthDp >= 600f -> HomeHeroLayout(
            isTablet = true,
            heroHeight = (maxWidthDp * 0.58f).dp.coerceIn(320.dp, 380.dp),
            contentMaxWidth = 520.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 0.72f,
            contentHorizontalPadding = 32.dp,
            contentVerticalPadding = 18.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 170.dp,
            logoWidthFraction = 0.54f,
            backgroundMotionStrength = 1f,
        )
        else -> HomeHeroLayout(
            isTablet = false,
            heroHeight = mobileHeroHeight(
                maxWidthDp = maxWidthDp,
                viewportHeightDp = viewportHeightDp,
                mobileBelowSectionHeightHintDp = mobileBelowSectionHeightHintDp,
            ),
            contentMaxWidth = 480.dp,
            contentContainerMaxWidth = maxWidthDp.dp,
            contentWidthFraction = 1f,
            contentHorizontalPadding = 24.dp,
            contentVerticalPadding = 16.dp,
            topFadeHeight = 0.dp,
            bottomFadeHeight = 220.dp,
            logoWidthFraction = 0.62f,
            backgroundMotionStrength = 1f,
        )
    }
}

private fun desktopHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
): Dp {
    val baselineHeight = (maxWidthDp * 0.56f).dp.coerceIn(460.dp, 640.dp)
    val viewportHeight = viewportHeightDp ?: return baselineHeight
    val ultrawideProgress = ultrawideViewportProgress(
        widthDp = maxWidthDp,
        heightDp = viewportHeight,
    )
    if (ultrawideProgress <= 0f) return baselineHeight

    val ultrawideHeight = (viewportHeight * ULTRAWIDE_HERO_VIEWPORT_HEIGHT_RATIO).dp
        .coerceAtLeast(baselineHeight)
    return (
        baselineHeight.value +
            (ultrawideHeight.value - baselineHeight.value) * ultrawideProgress
        ).dp
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

private fun mobileHeroHeight(
    maxWidthDp: Float,
    viewportHeightDp: Float?,
    mobileBelowSectionHeightHintDp: Float?,
): Dp {
    val viewportDrivenHeight = viewportHeightDp?.let { (it * MOBILE_HERO_VIEWPORT_RATIO).dp }
    val widthFallbackHeight = (maxWidthDp * 1.16f).dp
    val baseHeight = if (mobileBelowSectionHeightHintDp == null) {
        viewportDrivenHeight?.coerceAtMost(widthFallbackHeight) ?: widthFallbackHeight
    } else {
        viewportDrivenHeight ?: widthFallbackHeight
    }

    val cappedHeight = if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        val maxAllowedFromViewport = (viewportHeightDp - mobileBelowSectionHeightHintDp).dp
        baseHeight.coerceAtMost(maxAllowedFromViewport)
    } else {
        baseHeight
    }

    return if (viewportHeightDp != null && mobileBelowSectionHeightHintDp != null) {
        cappedHeight.coerceIn(0.dp, MOBILE_HERO_MAX_HEIGHT_DP.dp)
    } else {
        cappedHeight.coerceIn(MOBILE_HERO_MIN_HEIGHT_DP.dp, MOBILE_HERO_MAX_HEIGHT_DP.dp)
    }
}

@Composable
private fun HeroMetaDot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
    )
}

private fun heroScrollOffsetPx(
    listState: LazyListState?,
    heroHeightPx: Float,
): Float = when {
    listState == null -> 0f
    listState.firstVisibleItemIndex > 0 -> heroHeightPx
    else -> listState.firstVisibleItemScrollOffset.toFloat()
}

private fun heroBackgroundScrollScale(scrollOffsetPx: Float): Float {
    val scaleIncrease = if (scrollOffsetPx < 0f) {
        abs(scrollOffsetPx) * HERO_SCROLL_UP_SCALE_MULTIPLIER
    } else {
        scrollOffsetPx * HERO_SCROLL_DOWN_SCALE_MULTIPLIER
    }
    return (1f + scaleIncrease).coerceAtMost(HERO_SCROLL_MAX_SCALE)
}

private fun heroBackgroundScrollTranslationY(scrollOffsetPx: Float): Float {
    return scrollOffsetPx * HERO_SCROLL_PARALLAX
}

private fun Modifier.homeHeroPagerGesture(
    pagerState: PagerState,
    itemCount: Int,
    coroutineScope: CoroutineScope,
    onDragActiveChange: (Boolean) -> Unit,
): Modifier {
    if (itemCount <= 1) return this

    return pointerInput(pagerState, itemCount) {
        awaitEachGesture {
            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            val widthPx = size.width.toFloat().takeIf { it > 0f } ?: return@awaitEachGesture
            val velocityTracker = VelocityTracker().apply {
                addPosition(down.uptimeMillis, down.position)
            }
            val startPage = pagerState.currentPage
            var totalDx = 0f
            var totalDy = 0f
            var dragging = false
            var settleAnimationStarted = false

            try {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    velocityTracker.addPosition(change.uptimeMillis, change.position)

                    if (!change.pressed) {
                        if (dragging) {
                            val targetPage = resolveHeroTargetPage(
                                startPage = startPage,
                                pageCount = pagerState.pageCount,
                                totalDx = totalDx,
                                velocityX = velocityTracker.calculateVelocity().x,
                                widthPx = widthPx,
                            )
                            settleAnimationStarted = true
                            coroutineScope.launch {
                                try {
                                    pagerState.animateHeroToPage(targetPage)
                                } finally {
                                    onDragActiveChange(false)
                                }
                            }
                        }
                        break
                    }

                    val delta = change.position - change.previousPosition
                    totalDx += delta.x
                    totalDy += delta.y

                    if (!dragging) {
                        val horizontalDrag =
                            abs(totalDx) > viewConfiguration.touchSlop && abs(totalDx) > abs(totalDy)
                        val verticalDrag =
                            abs(totalDy) > viewConfiguration.touchSlop && abs(totalDy) > abs(totalDx)

                        when {
                            verticalDrag -> break
                            horizontalDrag -> {
                                dragging = true
                                onDragActiveChange(true)
                            }
                            else -> continue
                        }
                    }

                    pagerState.dispatchRawDelta(-delta.x)
                    change.consume()
                }
            } finally {
                if (dragging && !settleAnimationStarted) {
                    onDragActiveChange(false)
                }
            }
        }
    }
}

private fun resolveHeroTargetPage(
    startPage: Int,
    pageCount: Int,
    totalDx: Float,
    velocityX: Float,
    widthPx: Float,
): Int {
    val thresholdPassed = abs(totalDx) > widthPx * HERO_SWIPE_THRESHOLD_FRACTION ||
        abs(velocityX) > HERO_SWIPE_VELOCITY_THRESHOLD
    if (!thresholdPassed) return startPage

    val currentPage = startPage.coerceIn(0, pageCount - 1)
    return when {
        totalDx > 0f -> (currentPage - 1).coerceAtLeast(0)
        totalDx < 0f -> (currentPage + 1).coerceAtMost(pageCount - 1)
        else -> currentPage
    }
}
