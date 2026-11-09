package com.imdadburki.aurelius.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class LrcLibResponse(
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

data class LyricLine(val timeMs: Long, val text: String)

/** Parses LRC timestamped lyrics, e.g. "[01:23.45] line text" -> LyricLine(83450, ...). */
fun parseLrc(lrc: String): List<LyricLine> {
    val lineRegex = Regex("""\[(\d+):(\d+)(?:\.(\d+))?\](.*)""")
    return lrc.lines().mapNotNull { raw ->
        val m = lineRegex.matchEntire(raw.trim()) ?: return@mapNotNull null
        val min = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        val sec = m.groupValues[2].toLongOrNull() ?: return@mapNotNull null
        val frac = m.groupValues[3]
        val ms = when (frac.length) {
            1 -> frac.toLong() * 100
            2 -> frac.toLong() * 10
            3 -> frac.toLong()
            else -> 0L
        }
        val text = m.groupValues[4].trim()
        if (text.isEmpty()) null else LyricLine((min * 60 + sec) * 1000 + ms, text)
    }.sortedBy { it.timeMs }
}

interface LrcLibService {
    @GET("api/get")
    suspend fun get(
        @Query("artist_name") artist: String,
        @Query("track_name") track: String
    ): LrcLibResponse
}

/**
 * Lyrics via LRCLIB's free public API (permitted interface, no key, no scraping).
 * Falls back to plain lyrics, then to local .lrc files (handled by the caller).
 * Never bundles copyrighted lyrics.
 */
class LyricsApi {
    private val json = Json { ignoreUnknownKeys = true }
    private val service: LrcLibService by lazy {
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(LrcLibService::class.java)
    }

    /** Returns synced lines, or plain lines with no timestamps, or null when unavailable. */
    suspend fun fetchLyrics(artist: String, title: String): List<LyricLine>? {
        return try {
            val resp = service.get(artist, title)
            val synced = resp.syncedLyrics?.takeIf { it.isNotBlank() }?.let { parseLrc(it) }
            if (!synced.isNullOrEmpty()) return synced
            val plain = resp.plainLyrics?.takeIf { it.isNotBlank() }
            plain?.lines()?.filter { it.isNotBlank() }?.map { LyricLine(-1, it.trim()) }
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) null else throw IllegalStateException("Lyrics lookup failed (HTTP ${e.code()}).")
        } catch (e: java.io.IOException) {
            throw IllegalStateException("No network connection.")
        }
    }
}


