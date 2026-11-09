package com.imdadburki.aurelius.core.network

import com.imdadburki.aurelius.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class YtSearchResponse(
    val items: List<YtSearchItem> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class YtSearchItem(
    val id: YtSearchId,
    val snippet: YtSnippet
)

@Serializable
data class YtSearchId(val videoId: String? = null)

@Serializable
data class YtSnippet(
    val title: String = "",
    @SerialName("channelTitle") val channelTitle: String = "",
    val thumbnails: YtThumbnails = YtThumbnails()
)

@Serializable
data class YtThumbnails(
    /** 120x90, ~1.4 KB — the list-row size. v2.0.3: the app used to prefer
     * `high` (480x360, ~7.6 KB) which never arrived on Imdad's 1.5 KB/s
     * connection, so every row showed the logo placeholder forever. */
    val default: YtThumb? = null,
    val medium: YtThumb? = null,
    val high: YtThumb? = null
)

@Serializable
data class YtThumb(val url: String = "")

@Serializable
data class YtVideosResponse(val items: List<YtVideoItem> = emptyList())

@Serializable
data class YtVideoItem(
    val id: String = "",
    @SerialName("contentDetails") val contentDetails: YtContentDetails = YtContentDetails(),
    val status: YtStatus = YtStatus()
)

@Serializable
data class YtStatus(val embeddable: Boolean = true)

@Serializable
data class YtContentDetails(val duration: String = "")

data class YouTubeResult(
    val videoId: String,
    val title: String,
    val channel: String,
    /** Small thumbnail (default 120x90) for list rows — loads in ~1s even on
     * a 1.5 KB/s connection. */
    val thumbnailUrl: String,
    /** Larger thumbnail (medium 320x180) for wide cards like Trending. */
    val thumbnailUrlLarge: String = "",
    val durationMs: Long,
    /** False when the uploader disabled embedding — the video can only play
     * in the YouTube app. Defaults to true when the API does not say. */
    val embeddable: Boolean = true
)

/** Picks (small, large) thumbnail URLs from a search result. Small prefers
 * `default` (120x90, ~1.4 KB); large prefers `medium` (320x180). */
fun pickThumbnailUrls(thumbs: YtThumbnails): Pair<String, String> {
    val small = thumbs.default?.url ?: thumbs.medium?.url ?: thumbs.high?.url ?: ""
    val large = thumbs.medium?.url ?: thumbs.high?.url ?: thumbs.default?.url ?: ""
    return small to large
}

/** Unescapes the HTML entities the YouTube Data API returns in titles
 * (unit-tested, v2.0.6). The API returns e.g. `&quot;Big Buck Bunny&quot;`
 * for a title with real quotation marks — shown literally on the
 * player title bar on Appetize (2026-11-08) until this was added.
 * `&amp;` is replaced LAST so `&amp;quot;` decodes to `&quot;` (one
 * level only), never to a bare quote. Numeric entities (&#34; &#x27;)
 * are decoded too. */
fun unescapeHtmlEntities(text: String): String {
    var out = text
    out = out.replace("&quot;", "\"")
        .replace("&#34;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&#x27;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
    out = Regex("&#(\\d+);").replace(out) { m ->
        val code = m.groupValues[1].toIntOrNull() ?: return@replace m.value
        runCatching { code.toChar().toString() }.getOrDefault(m.value)
    }
    out = Regex("&#x([0-9a-fA-F]+);").replace(out) { m ->
        val code = m.groupValues[1].toIntOrNull(16) ?: return@replace m.value
        runCatching { code.toChar().toString() }.getOrDefault(m.value)
    }
    out = out.replace("&amp;", "&")
    return out
}

/** Parses a search.list response (used by unit tests for thumbnails). */
fun parseYtSearchResponse(raw: String): YtSearchResponse {
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString(raw)
}

/** Parses ISO-8601 durations returned by the YouTube API, e.g. PT4M28S -> ms. */
fun parseIso8601Duration(iso: String): Long {
    val regex = Regex("""PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?""")
    val m = regex.matchEntire(iso) ?: return 0L
    val h = m.groupValues[1].toLongOrNull() ?: 0L
    val min = m.groupValues[2].toLongOrNull() ?: 0L
    val s = m.groupValues[3].toLongOrNull() ?: 0L
    return ((h * 3600) + (min * 60) + s) * 1000L
}

/** Parses a videos.list response (used by unit tests for the embeddable flag). */
fun parseYtVideosResponse(raw: String): YtVideosResponse {
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString(raw)
}

interface YouTubeService {
    @GET("search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 25,
        @Query("order") order: String = "relevance",
        @Query("q") q: String,
        @Query("pageToken") pageToken: String? = null,
        @Query("key") key: String
    ): YtSearchResponse

    @GET("videos")
    suspend fun videos(
        @Query("part") part: String = "contentDetails,status",
        @Query("id") id: String,
        @Query("key") key: String
    ): YtVideosResponse
}

/**
 * YouTube Data API v3 search. The API key is the owner's own restricted key,
 * built into the app (BuildConfig) — there is no key field, dialog or option
 * anywhere in the UI. Search results open in the in-app IFrame player;
 * nothing is downloaded or extracted.
 */
class YouTubeApi {
    private val json = Json { ignoreUnknownKeys = true }
    private val service: YouTubeService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(
                OkHttpClient.Builder()
                    // Slow mobile connections (seen at 0.6 KB/s): the 10s
                    // defaults timed out before the response arrived.
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YouTubeService::class.java)
    }

    /** Returns results plus the next-page token, or throws with a user-facing message. */
    suspend fun search(
        query: String,
        pageToken: String? = null,
        order: String = "relevance"
    ): Pair<List<YouTubeResult>, String?> {
        val key = BuildConfig.YOUTUBE_API_KEY
        if (key.isBlank()) throw IllegalStateException("YouTube search is unavailable right now.")
        val resp = try {
            service.search(q = query, pageToken = pageToken, key = key, order = order)
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                400 -> throw IllegalStateException("YouTube rejected the search request. Try again.")
                403 -> throw IllegalStateException("YouTube quota exceeded. Try again later.")
                else -> throw IllegalStateException("YouTube search failed (HTTP ${e.code()}).")
            }
        } catch (e: java.io.IOException) {
            throw IllegalStateException("No network connection. Check your internet and retry.")
        }
        val items = resp.items.mapNotNull { item ->
            val vid = item.id.videoId ?: return@mapNotNull null
            val (small, large) = pickThumbnailUrls(item.snippet.thumbnails)
            YouTubeResult(
                vid,
                unescapeHtmlEntities(item.snippet.title),
                unescapeHtmlEntities(item.snippet.channelTitle),
                small, large, 0L
            )
        }
        if (items.isEmpty()) return Pair(emptyList(), resp.nextPageToken)
        // Fill durations + embeddability with one batched videos call.
        val meta = try {
            service.videos(id = items.joinToString(",") { it.videoId }, key = key).items
        } catch (e: Exception) {
            emptyList()
        }
        val durations = meta.associate { it.id to parseIso8601Duration(it.contentDetails.duration) }
        val embeddable = meta.associate { it.id to it.status.embeddable }
        return Pair(
            items.map {
                it.copy(
                    durationMs = durations[it.videoId] ?: 0L,
                    embeddable = embeddable[it.videoId] ?: true
                )
            },
            resp.nextPageToken
        )
    }
}


