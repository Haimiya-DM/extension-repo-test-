package eu.kanade.tachiyomi.animeextension.en.miruro

import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Primary Anime Extension: Miruro.to
 * 
 * Compatible with Mihon, Aniyomi, and ShonenX (via dartotsu_extension_bridge).
 * Extends [AnimeHttpSource] and implements [ConfigurableAnimeSource].
 */
class Miruro : AnimeHttpSource(), ConfigurableAnimeSource {

    override val name = "Miruro"

    override val baseUrl = "https://miruro.to"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

    // ============================== Popular Anime ==============================

    override fun popularAnimeRequest(page: Int): Request {
        // TODO: Confirm the exact popular anime endpoint on miruro.to
        // E.g.: "$baseUrl/anime/popular?page=$page" or "$baseUrl/api/anime/trending?page=$page"
        val endpoint = "$baseUrl/anime/popular?page=$page"
        return GET(endpoint, headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        // TODO: Replace with the actual CSS/HTML selector for anime cards on the popular page
        val animeElements = document.select("div.anime-card, div.film-item, article.anime-item")

        val animeList = animeElements.mapNotNull { element ->
            runCatching {
                popularAnimeFromElement(element)
            }.getOrNull()
        }

        // TODO: Replace with the actual next page indicator selector
        val hasNextPage = document.select("ul.pagination li.active + li, a[rel=next]").isNotEmpty()

        return AnimesPage(animeList, hasNextPage)
    }

    private fun popularAnimeFromElement(element: Element): SAnime {
        return SAnime.create().apply {
            // TODO: Verify selector for title text
            title = element.selectFirst("h3.title, a.title, .film-name")?.text()?.trim() ?: "Unknown Title"

            // TODO: Verify selector for anime detail URL link
            val href = element.selectFirst("a[href]")?.attr("href") ?: ""
            setUrlWithoutDomain(href)

            // TODO: Verify selector for poster/thumbnail image
            thumbnail_url = element.selectFirst("img[data-src], img[src]")?.let { img ->
                img.attr("abs:data-src").ifEmpty { img.attr("abs:src") }
            }
        }
    }

    // ============================== Latest Updates ==============================

    override fun latestUpdatesRequest(page: Int): Request {
        // TODO: Confirm latest updates endpoint path on miruro.to
        // E.g.: "$baseUrl/anime/recently-updated?page=$page" or "$baseUrl/latest?page=$page"
        val endpoint = "$baseUrl/anime/latest?page=$page"
        return GET(endpoint, headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage {
        // Miruro latest parsing logic
        val document = response.asJsoup()

        // TODO: Confirm selector for latest updated items
        val animeElements = document.select("div.anime-card, div.film-item, div.latest-item")

        val animeList = animeElements.mapNotNull { element ->
            runCatching {
                popularAnimeFromElement(element)
            }.getOrNull()
        }

        // TODO: Confirm pagination selector for latest updates
        val hasNextPage = document.select("ul.pagination li.active + li, a[rel=next]").isNotEmpty()

        return AnimesPage(animeList, hasNextPage)
    }

    // ============================== Search Anime ==============================

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        // TODO: Confirm search query parameter formatting (query param name, page param)
        // E.g.: "$baseUrl/search?keyword=$query&page=$page" or "$baseUrl/anime/search?q=$query"
        val url = "$baseUrl/search?keyword=$query&page=$page"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        // TODO: Confirm selector for search result cards
        val searchResults = document.select("div.anime-card, div.film-item, div.search-result-item")

        val animeList = searchResults.mapNotNull { element ->
            runCatching {
                popularAnimeFromElement(element)
            }.getOrNull()
        }

        val hasNextPage = document.select("ul.pagination li.active + li, a[rel=next]").isNotEmpty()
        return AnimesPage(animeList, hasNextPage)
    }

    // ============================== Anime Details ==============================

    override fun animeDetailsParse(response: Response): SAnime {
        val document = response.asJsoup()

        return SAnime.create().apply {
            // TODO: Confirm anime title selector on details page
            title = document.selectFirst("h1.anime-title, h1.title, .film-title")?.text()?.trim() ?: "Unknown Title"

            // TODO: Confirm thumbnail / cover image selector
            thumbnail_url = document.selectFirst(".poster img, .anime-poster img")?.let {
                it.attr("abs:data-src").ifEmpty { it.attr("abs:src") }
            }

            // TODO: Confirm synopsis / description selector
            description = document.selectFirst(".synopsis, .description, .film-description")?.text()?.trim()

            // TODO: Confirm genre badges selector
            genre = document.select(".genres a, .genre-item, a[href*='/genre/']").joinToString(", ") { it.text().trim() }

            // TODO: Confirm status parsing selector
            val statusText = document.selectFirst(".status, .anime-status")?.text() ?: ""
            status = parseStatus(statusText)

            // TODO: Confirm studio / author selector
            author = document.selectFirst(".studio, .anime-studio, a[href*='/producer/']")?.text()?.trim()
            artist = author
        }
    }

    private fun parseStatus(statusString: String): Int {
        return when {
            statusString.contains("Completed", ignoreCase = true) -> SAnime.COMPLETED
            statusString.contains("Ongoing", ignoreCase = true) || statusString.contains("Currently Airing", ignoreCase = true) -> SAnime.ONGOING
            else -> SAnime.UNKNOWN
        }
    }

    // ============================== Episode List ==============================

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()

        // TODO: Confirm whether episode list is in the initial HTML or loaded via a separate API call
        // If API-based: query "$baseUrl/api/anime/{id}/episodes" and parse JSON
        // If HTML-based: parse episode elements from the DOM
        val episodeElements = document.select("div.episode-item, ul.episodes-list li a, .ep-item")

        return episodeElements.mapIndexedNotNull { index, element ->
            runCatching {
                SEpisode.create().apply {
                    // TODO: Confirm episode link selector and URL
                    val href = element.attr("href").ifEmpty { element.selectFirst("a")?.attr("href") ?: "" }
                    setUrlWithoutDomain(href)

                    // TODO: Confirm episode name and number extraction
                    val epTitle = element.selectFirst(".ep-title, .title")?.text()?.trim()
                    val epNumText = element.selectFirst(".ep-number, .number")?.text()?.trim() 
                        ?: Regex("""\d+""").find(element.text())?.value 
                        ?: (index + 1).toString()

                    episode_number = epNumText.toFloatOrNull() ?: (index + 1).toFloat()
                    name = if (!epTitle.isNullOrBlank()) "Episode $epNumText: $epTitle" else "Episode $epNumText"

                    // TODO: Date upload parsing if available on miruro.to
                    date_upload = parseEpisodeDate(element.selectFirst(".ep-date, .date")?.text())
                }
            }.getOrNull()
        }.reversed() // Most extensions sort oldest first (ep 1 at bottom, ep N at top)
    }

    private fun parseEpisodeDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dateStr)?.time ?: 0L
        }.getOrDefault(0L)
    }

    // ============================== Video / Stream Extraction ==============================

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val videoList = mutableListOf<Video>()

        // TODO: Confirm video stream extraction method for miruro.to
        // Pattern 1: Direct iframe embed player:
        val iframeUrl = document.selectFirst("iframe[src*='embed'], iframe#player-iframe")?.attr("abs:src")
        
        // TODO: If iframe embed player exists, inspect player embed (e.g. MegaCloud, Vidstreaming, RapidCloud)
        // val embedDoc = client.newCall(GET(iframeUrl, headers)).execute().asJsoup()
        // val streamScript = embedDoc.selectFirst("script:containsData(sources)")?.data()

        // Placeholder fallback stream entry for development & confirmation
        if (!iframeUrl.isNullOrBlank()) {
            videoList.add(
                Video(
                    url = iframeUrl,
                    quality = "Default (Embed: $iframeUrl)",
                    videoUrl = iframeUrl,
                    headers = headers
                )
            )
        }

        // TODO: Provide real extraction for HLS m3u8 playlists:
        // PlaylistUtils(client, headers).extractFromHls(masterUrl, videoNameGen = { "Miruro - $it" })

        return videoList
    }

    // ============================== Preferences ==============================

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val qualityPref = ListPreference(screen.context).apply {
            key = "pref_preferred_quality"
            title = "Preferred Quality"
            entries = arrayOf("1080p", "720p", "480p", "360p")
            entryValues = arrayOf("1080", "720", "480", "360")
            setDefaultValue("1080")
            summary = "%s"
        }
        screen.addPreference(qualityPref)
    }
}
