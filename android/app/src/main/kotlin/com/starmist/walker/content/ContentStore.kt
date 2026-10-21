package com.starmist.walker.content

import android.content.Context
import com.starmist.core.route.ContentPack
import com.starmist.core.route.Routes
import com.starmist.walker.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * The text content (card lines, "did you know" facts, township features) as a pack that can be
 * replaced without a new app: a copy is bundled in the app, and a newer one is fetched from a
 * static address, checked, and kept in the app's private folder. Anything wrong with a download
 * (no network, bad hash, a file that breaks the content rules) leaves what was there untouched,
 * so the app always has text. The fetch only reads public files; nothing is sent.
 */
class ContentStore(private val context: Context, private val baseUrl: String = BuildConfig.CONTENT_URL) {
    private val root = File(context.filesDir, "content")
    private val current = File(root, "current")
    private val meta = File(root, "published")

    /** When the stored pack was published (seconds), or 0 when there is none that is newer than this build. */
    val publishedAt: Long
        get() = runCatching {
            meta.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull()?.takeIf { it > BuildConfig.BUILD_TIME && current.isDirectory }
        }.getOrNull() ?: 0L

    /** The lines of a pack file: the stored pack when it is newer than this build, otherwise the bundled copy. */
    fun lines(name: String): List<String> {
        if (publishedAt > 0) {
            val f = File(current, name)
            if (f.isFile) runCatching { return f.readLines(Charsets.UTF_8) }
        }
        return bundled(name)
    }

    private fun bundled(name: String): List<String> {
        val id = context.resources.getIdentifier(name.substringBeforeLast('.'), "raw", context.packageName)
        if (id == 0) return emptyList()
        return runCatching { context.resources.openRawResource(id).bufferedReader(Charsets.UTF_8).readLines() }.getOrDefault(emptyList())
    }

    /** Puts the card names and lines of the active pack onto the routes. */
    fun applyToRoutes() {
        Routes.applyTexts(ContentPack.parseCards(lines(ContentPack.CARDS)))
    }

    sealed interface Result {
        data object UpToDate : Result
        data class Updated(val publishedAt: Long) : Result
        data class Failed(val reason: String) : Result
    }

    /** Looks for a newer pack and stores it when every file checks out. Safe to call at any time. */
    suspend fun refresh(): Result = withContext(Dispatchers.IO) {
        try {
            val manifest = JSONObject(fetch("manifest.json", 64 * 1024).toString(Charsets.UTF_8))
            if (manifest.optInt("schema") != ContentPack.SCHEMA) return@withContext Result.UpToDate
            val published = manifest.getLong("published")
            if (published <= maxOf(publishedAt, BuildConfig.BUILD_TIME)) return@withContext Result.UpToDate

            val files = manifest.getJSONObject("files")
            val staging = File(root, "staging").apply { deleteRecursively(); mkdirs() }
            val ids = Routes.byId("taiwan_loop")!!.cards.map { it.first.id }.toSet()
            for (name in ContentPack.FILES) {
                val info = files.getJSONObject(name)
                val bytes = fetch(name, 2 * 1024 * 1024)
                if (bytes.size.toLong() != info.getLong("size")) return@withContext Result.Failed("$name: wrong size")
                if (sha256(bytes) != info.getString("sha256")) return@withContext Result.Failed("$name: wrong hash")
                val errors = ContentPack.validate(name, bytes.toString(Charsets.UTF_8).lines(), ids)
                if (errors.isNotEmpty()) return@withContext Result.Failed("$name: ${errors.first()}")
                File(staging, name).writeBytes(bytes)
            }
            // Swap in only after everything arrived and passed.
            current.deleteRecursively()
            if (!staging.renameTo(current)) return@withContext Result.Failed("could not store the pack")
            meta.writeText(published.toString())
            applyToRoutes()
            Result.Updated(published)
        } catch (e: Exception) {
            Result.Failed(e.javaClass.simpleName + (e.message?.let { ": $it" } ?: ""))
        }
    }

    private fun fetch(name: String, maxBytes: Int): ByteArray {
        val c = URL(baseUrl + name).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        c.useCaches = false
        try {
            if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
            c.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > maxBytes) throw IllegalStateException("$name is too large")
                }
                return out.toByteArray()
            }
        } finally {
            c.disconnect()
        }
    }

    private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }
}
