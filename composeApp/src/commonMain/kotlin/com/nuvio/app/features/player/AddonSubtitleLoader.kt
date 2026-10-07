package com.nuvio.app.features.player

import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.AddonResource
import com.nuvio.app.features.addons.buildAddonResourceUrl
import com.nuvio.app.features.addons.enabledAddons
import com.nuvio.app.features.addons.fetchAddonResponseText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_addon_subtitle_display_format
import org.jetbrains.compose.resources.getString

@Serializable
data class SubtitleAddonRequest(
    val url: String,
    val addonId: String,
    val addonName: String,
)

internal fun addonSubtitleRequests(
    type: String,
    videoId: String,
    playingFile: PlayingFileHints? = null,
): List<SubtitleAddonRequest> {
    val requestType = canonicalSubtitleType(type)
    val extra = playingFile?.toSubtitleExtraPathSegment()
    return AddonRepository.uiState.value.addons.enabledAddons().mapNotNull { addon ->
        val manifest = addon.manifest ?: return@mapNotNull null
        if (manifest.resources.none { resource ->
                (resource.name.equals("subtitles", true) || resource.name.equals("subtitle", true)) &&
                    resource.supportsSubtitleType(requestType, videoId)
            }) return@mapNotNull null
        SubtitleAddonRequest(
            url = buildAddonResourceUrl(manifest.transportUrl, "subtitles", requestType, videoId, extra),
            addonId = manifest.id,
            addonName = addon.displayTitle,
        )
    }
}

internal suspend fun loadAddonSubtitles(
    requests: List<SubtitleAddonRequest>,
    onLoaded: (SubtitleAddonRequest, List<AddonSubtitle>) -> Unit = { _, _ -> },
): List<AddonSubtitle> = supervisorScope {
    requests.map { request ->
        async {
            val subtitles = try {
                withTimeoutOrNull(10_000L) {
                    parseAddonSubtitles(fetchAddonResponseText(request.url), request)
                }.orEmpty()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                emptyList()
            }
            currentCoroutineContext().ensureActive()
            onLoaded(request, subtitles)
            subtitles
        }
    }.awaitAll().flatten()
}

private suspend fun parseAddonSubtitles(response: String, request: SubtitleAddonRequest): List<AddonSubtitle> {
    val subtitles = Json.parseToJsonElement(response).jsonObject["subtitles"]?.jsonArray.orEmpty()
    return subtitles.mapIndexedNotNull { index, element ->
        val obj = element as? JsonObject ?: return@mapIndexedNotNull null
        val url = obj.stringValue("url") ?: return@mapIndexedNotNull null
        val languageField = listOf("lang", "language", "languageCode", "locale")
            .firstNotNullOfOrNull(obj::stringValue)
        val label = obj.stringValue("label")
        val language = languageField ?: label ?: "unknown"
        AddonSubtitle(
            id = obj.stringValue("id") ?: "${request.addonId}_$index",
            url = url,
            language = normalizeLanguageCode(language) ?: language,
            display = getString(
                Res.string.player_addon_subtitle_display_format,
                addonSubtitleTitle(
                    languageLabel = getLanguageLabelForCode(language),
                    // When there is no language field the label already stood in for it.
                    trackLabel = label.takeIf { languageField != null },
                ),
                request.addonName,
            ),
            addonName = request.addonName,
            fontUrls = obj.fontUrls(),
        )
    }
}

/** `fonts`: font file URLs sent with ASS subtitles (Jellio++ serves the video's font attachments). */
internal fun JsonObject.fontUrls(): List<String> =
    (this["fonts"] as? JsonArray).orEmpty()
        .mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
        .filter { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
        .distinct()

/**
 * Language name plus the addon's track label (e.g. "English · SDH"), so several tracks in one
 * language can be told apart. The label is dropped when it only repeats the language.
 */
internal fun addonSubtitleTitle(languageLabel: String, trackLabel: String?): String {
    val label = trackLabel?.trim()?.takeIf { it.isNotEmpty() && !it.equals(languageLabel, ignoreCase = true) }
    return if (label == null) languageLabel else "$languageLabel · $label"
}

private fun canonicalSubtitleType(type: String): String =
    if (type.equals("tv", ignoreCase = true)) "series" else type.lowercase()

private fun AddonResource.supportsSubtitleType(type: String, videoId: String): Boolean {
    val canonical = canonicalSubtitleType(type)
    val typeMatches = types.isEmpty() || types.any { canonicalSubtitleType(it).equals(canonical, ignoreCase = true) }
    return typeMatches && (idPrefixes.isEmpty() || idPrefixes.any { videoId.startsWith(it) })
}

private fun JsonObject.stringValue(name: String): String? =
    this[name]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotBlank() }
