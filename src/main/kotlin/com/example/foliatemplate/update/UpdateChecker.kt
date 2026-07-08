package com.example.foliatemplate.update

import com.example.foliatemplate.scheduler.Schedulers
import com.google.gson.JsonParser
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Checks a GitHub repository's latest release and logs a note if it's newer
 * than the running version.
 *
 * Runs entirely on the async scheduler — network I/O must never touch a region
 * thread. It only reads and logs; it never downloads or self-updates.
 *
 * Set [githubRepo] to "owner/repo" (e.g. "YourName/your-plugin"). The check
 * uses GitHub's `/releases/latest` endpoint, which already ignores drafts and
 * pre-releases, and compares the release's tag (e.g. "v1.2.0") to your version.
 * If you don't publish GitHub releases, just don't call [checkAsync].
 *
 * Note: unauthenticated GitHub API requests are limited to 60/hour per IP,
 * which is plenty for a startup check.
 */
class UpdateChecker(
    private val plugin: JavaPlugin,
    private val schedulers: Schedulers,
    private val githubRepo: String,
) {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    fun checkAsync() {
        // Small delay so this never competes with startup work.
        schedulers.asyncDelayed(5) {
            try {
                val latest = fetchLatestTag() ?: return@asyncDelayed
                val current = plugin.pluginMeta.version
                if (compareVersions(latest, current) > 0) {
                    plugin.logger.info("An update is available: $latest (you have $current).")
                    plugin.logger.info("Download: https://github.com/$githubRepo/releases/latest")
                } else {
                    plugin.logger.info("You're on the latest version ($current).")
                }
            } catch (e: Exception) {
                plugin.logger.warning("Update check failed: ${e.message}")
            }
        }
    }

    private fun fetchLatestTag(): String? {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/$githubRepo/releases/latest"))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "${plugin.pluginMeta.name}/${plugin.pluginMeta.version} (update-checker)")
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build()

        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        // 404 = no releases yet; anything non-200 we simply skip.
        if (response.statusCode() != 200) return null

        val obj = JsonParser.parseString(response.body()).asJsonObject
        val tag = obj.get("tag_name") ?: return null
        return tag.asString
    }
}

/**
 * Compares two dotted version strings numerically (e.g. "1.10.0" > "1.9.9").
 * Leading 'v' is ignored (so GitHub tags like "v1.2.0" work); non-numeric tags
 * are skipped. Pure and unit-tested.
 *
 * @return >0 if [a] is newer than [b], 0 if equal, <0 if older.
 */
fun compareVersions(a: String, b: String): Int {
    val pa = a.trimStart('v', 'V').split('.', '-', '+', '_').mapNotNull { it.toIntOrNull() }
    val pb = b.trimStart('v', 'V').split('.', '-', '+', '_').mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(pa.size, pb.size)) {
        val x = pa.getOrElse(i) { 0 }
        val y = pb.getOrElse(i) { 0 }
        if (x != y) return x.compareTo(y)
    }
    return 0
}
