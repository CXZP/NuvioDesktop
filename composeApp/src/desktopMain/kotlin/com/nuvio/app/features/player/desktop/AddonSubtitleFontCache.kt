package com.nuvio.app.features.player.desktop

import co.touchlab.kermit.Logger
import com.nuvio.app.core.storage.DesktopStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * Font files an addon sends with an ASS subtitle (Jellio++ serves the video's font attachments),
 * kept in one folder per font set so libass can load them through mpv's `sub-fonts-dir`.
 */
internal object AddonSubtitleFontCache {
    private val log = Logger.withTag("AddonSubtitleFonts")

    private const val MAX_FONTS = 64
    private const val MAX_FONT_BYTES = 32 * 1024 * 1024
    private const val MAX_CACHED_SETS = 20

    private val rootDir: Path by lazy { DesktopStorage.cacheDir.resolve("subtitle-fonts") }
    private val downloadSemaphore = Semaphore(4)
    private val httpClient by lazy {
        HttpClient(CIO) {
            followRedirects = true
            engine {
                requestTimeout = 30_000
            }
        }
    }

    /** Folder holding the downloaded fonts, or null when none of them could be fetched. */
    suspend fun prepare(fontUrls: List<String>): Path? = withContext(Dispatchers.IO) {
        val urls = fontUrls.distinct().take(MAX_FONTS)
        if (urls.isEmpty()) return@withContext null
        val directory = rootDir.resolve(fontSetKey(urls))
        Files.createDirectories(directory)
        val fetched = coroutineScope {
            urls.mapIndexed { index, url ->
                async { downloadSemaphore.withPermit { ensureFont(directory, index, url) } }
            }.awaitAll()
        }.count { it }
        log.d { "fonts ready ${fetched}/${urls.size} in ${directory.fileName}" }
        if (fetched == 0) return@withContext null
        runCatching { Files.setLastModifiedTime(directory, FileTime.fromMillis(System.currentTimeMillis())) }
        trimOldSets(keep = directory)
        directory
    }

    /** Fonts are written atomically under their index, so an existing file is a complete one. */
    private suspend fun ensureFont(directory: Path, index: Int, url: String): Boolean {
        val prefix = fontFilePrefix(index)
        val existing = Files.list(directory).use { files -> files.anyMatch { it.fileName.toString().startsWith(prefix) } }
        if (existing) return true
        return try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) {
                log.w { "font $index HTTP ${response.status.value}" }
                return false
            }
            val bytes = response.body<ByteArray>()
            val extension = fontFileExtension(bytes)
            if (extension == null || bytes.size > MAX_FONT_BYTES) {
                log.w { "font $index skipped: not a font file or too large (${bytes.size} B)" }
                return false
            }
            // Written outside the folder first: libass loads every file it finds in there.
            val pending = Files.createTempFile(rootDir, prefix, ".part")
            try {
                Files.write(pending, bytes)
                Files.move(pending, directory.resolve("$prefix.$extension"), StandardCopyOption.REPLACE_EXISTING)
            } finally {
                Files.deleteIfExists(pending)
            }
            true
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            log.w { "font $index failed: ${error::class.simpleName}" }
            false
        }
    }

    private fun trimOldSets(keep: Path) {
        runCatching {
            val sets = Files.list(rootDir).use { paths -> paths.filter(Files::isDirectory).toList() }
            if (sets.size <= MAX_CACHED_SETS) return
            sets.filter { it != keep }
                .sortedBy { runCatching { Files.getLastModifiedTime(it) }.getOrNull() }
                .take(sets.size - MAX_CACHED_SETS)
                .forEach { set ->
                    Files.list(set).use { files -> files.toList() }.forEach { runCatching { Files.deleteIfExists(it) } }
                    runCatching { Files.deleteIfExists(set) }
                }
        }
    }
}

/** Folder name for a font set; query strings (API keys) are left out so a new key reuses the cache. */
internal fun fontSetKey(urls: List<String>): String {
    val digest = MessageDigest.getInstance("SHA-256")
    urls.forEach { url ->
        digest.update(url.substringBefore('?').toByteArray(Charsets.UTF_8))
        digest.update(0)
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }.take(24)
}

internal fun fontFilePrefix(index: Int): String = "font-%03d".format(index)

/** Extension for a font file judged by its signature, or null when the bytes are not a font. */
internal fun fontFileExtension(bytes: ByteArray): String? {
    if (bytes.size < 4) return null
    val signature = bytes.copyOfRange(0, 4)
    return when {
        signature.contentEquals(byteArrayOf(0, 1, 0, 0)) -> "ttf"
        signature.contentEquals("true".toByteArray()) -> "ttf"
        signature.contentEquals("OTTO".toByteArray()) -> "otf"
        signature.contentEquals("ttcf".toByteArray()) -> "ttc"
        signature.contentEquals("wOFF".toByteArray()) -> "woff"
        signature.contentEquals("wOF2".toByteArray()) -> "woff2"
        else -> null
    }
}
