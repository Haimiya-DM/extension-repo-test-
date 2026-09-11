package eu.kanade.tachiyomi.animeextension.en.anikoto

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
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Fallback Anime Extension: Anikototv.to (Anikoto)
 *
 * Compatible with Mihon, Aniyomi, and ShonenX (via dartotsu_extension_bridge).
 * Extends [AnimeHttpSource] and implements [ConfigurableAnimeSource].
 */
class Anikoto : AnimeHttpSource(), ConfigurableAnimeSource {

    override val name = "Anikoto"

    override val baseUrl = "https://anikototv.to"

    override val lang = "en"

    override val supportsLatest = true

    // Alternate mirror domains used by Anikoto network
    val domainEntries = listOf(
        "anikototv.to",
        "anikoto.bz",
        "anikoto.cz",
        "anikoto.me",
        "anikoto.net"
    )

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

    // ============================== Popular Anime ==============================

    override fun popularAnimeRequest(page: Int): Request {
        // TODO: Confirm the popular/trending endpoint path on anikototv.to
        // E.g.: "$baseUrl/most-popular?page=$page" or "$baseUrl/filter?sort=views&page=$page"
        val endpoint = "$baseUrl/most-popular?page=$page"
        return GET(endpoint, headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        // TODO: Confirm CSS selector for anime list items on anikototv.to
        val animeElements = document.select("div.film_list-wrap div.flw-item, div.film-item, article.anime-card")

        val animeList = animeElements.mapNotNull { element ->
            runCatching {
                popularAnimeFromElement(element)
            }.getOrNull()
        }

        // TODO: Confirm next page pagination selector
        val hasNextPage = document.select("ul.pagination li.active + li, a.next, a[rel=next]").isNotEmpty()

        return AnimesPage(animeList, hasNextPage)
    }

    private fun popularAnimeFromElement(element: Element): SAnime {
        return SAnime.create().apply {
            // TODO: Confirm anime title selector
            title = element.selectFirst("h3.film-name a, .film-name, h3.title a")?.text()?.trim() ?: "Unknown Title"

            // TODO: Confirm link href attribute
            val href = element.selectFirst("a.film-poster-ahref, a[href*='/anime/'], a[href*='/watch/']")?.attr("href") ?: ""
            setUrlWithoutDomain(href)

            // TODO: Confirm poster image attribute (data-src vs src)
            thumbnail_url = element.selectFirst("img.film-poster-img, img[data-src], img[src]")?.let { img ->
                img.attr("abs:data-src").ifEmpty { img.attr("abs:src") }
            }
        }
    }

    // ============================== Latest Updates ==============================

    override fun latestUpdatesRequest(page: Int): Request {
        // TODO: Confirm latest released/updated anime endpoint path on anikototv.to
        // E.g.: "$baseUrl/recently-updated?page=$page" or "$baseUrl/filter?sort=recently_updated&page=$page"
        val endpoint = "$baseUrl/recently-updated?page=$page"
        return GET(endpoint, headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        // TODO: Confirm latest updates card selector
        val animeElements = document.select("div.film_list-wrap div.flw-item, div.film-item")

        val animeList = animeElements.mapNotNull { element ->
            runCatching {
                popularAnimeFromElement(element)
            }.getOrNull()
        }

        val hasNextPage = document.select("ul.pagination li.active + li, a[rel=next]").isNotEmpty()

        return AnimesPage(animeList, hasNextPage)
    }

    // ============================== Search Anime ==============================

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        // TODO: Confirm search endpoint and query parameters on anikototv.to
        // E.g.: "$baseUrl/search?keyword=$query&page=$page"
        val url = "$baseUrl/search?keyword=$query&page=$page"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        val document = response.asJsoup()

        // TODO: Confirm search result card selector
        val searchResults = document.select("div.film_list-wrap div.flw-item, div.film-item")

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
            // TODO: Confirm details page title selector
            title = document.selectFirst("h2.film-name, h1.anime-title, .detail-title")?.text()?.trim() ?: "Unknown Title"

            // TODO: Confirm poster image selector on details page
            thumbnail_url = document.selectFirst("img.film-poster-img, .detail-poster img")?.let {
                it.attr("abs:data-src").ifEmpty { it.attr("abs:src") }
            }

            // TODO: Confirm synopsis / plot description selector
            description = document.selectFirst("div.film-description, .anime-description, .text-synopsis")?.text()?.trim()

            // TODO: Confirm genres selector
            genre = document.select(".item-list a[href*='/genre/'], .genres a").joinToString(", ") { it.text().trim() }

            // TODO: Confirm status field
            val statusSpan = document.selectFirst(".item-title:contains(Status) + span, .status-text")?.text() ?: ""
            status = when {
                statusSpan.contains("Completed", ignoreCase = true) -> SAnime.COMPLETED
                statusSpan.contains("Currently Airing", ignoreCase = true) || statusSpan.contains("Ongoing", ignoreCase = true) -> SAnime.ONGOING
                else -> SAnime.UNKNOWN
            }

            // TODO: Confirm studio info
            author = document.selectFirst(".item-title:contains(Studios) + span a, a[href*='/producer/']")?.text()?.trim()
            artist = author
        }
    }

    // ============================== Episode List ==============================

    override fun episodeListParse(response: Response): List<SEpisode> {
        val document = response.asJsoup()

        // TODO: Check if episodes are loaded directly in page or via AJAX (e.g. /ajax/v2/episode/list/$id)
        val episodeElements = document.select("div.ss-list a.ssl-item, div.episodes-list a, .episode-item")

        return episodeElements.mapIndexedNotNull { index, element ->
            runCatching {
                SEpisode.create().apply {
                    // TODO: Confirm episode link selector
                    val href = element.attr("href").ifEmpty { element.selectFirst("a")?.attr("href") ?: "" }
                    setUrlWithoutDomain(href)

                    // TODO: Confirm episode title and number attributes
                    val epTitle = element.attr("title").ifEmpty { element.selectFirst(".ep-name")?.text()?.trim() ?: "" }
                    val epNum = element.attr("data-number").ifEmpty {
                        Regex("""\d+""").find(epTitle)?.value ?: (index + 1).toString()
                    }

                    episode_number = epNum.toFloatOrNull() ?: (index + 1).toFloat()
                    name = if (epTitle.isNotBlank() && !epTitle.equals("Episode $epNum", ignoreCase = true)) {
                        "Episode $epNum: $epTitle"
                    } else {
                        "Episode $epNum"
                    }

                    date_upload = 0L
                }
            }.getOrNull()
        }.reversed()
    }

    // ============================== Video / Stream Extraction ==============================

    override fun videoListParse(response: Response): List<Video> {
        val document = response.asJsoup()
        val videoList = mutableListOf<Video>()

        // TODO: Confirm Anikoto streaming embed servers (e.g. HD-1, Vidstream-2, VidCloud-1, Kiwi-Stream)
        val serverElements = document.select("div.ps__-list div.server-item, .servers-list a")

        // Example: If embed iframe is in page
        val iframeUrl = document.selectFirst("iframe#iframe-to-embed, iframe[src*='embed']")?.attr("abs:src")
        if (!iframeUrl.isNullOrBlank()) {
            videoList.add(
                Video(
                    url = iframeUrl,
                    quality = "Anikoto Default ($iframeUrl)",
                    videoUrl = iframeUrl,
                    headers = headers
                )
            )
        }

        // TODO: Extract HLS m3u8 streams using PlaylistUtils if direct streams are decrypted
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
