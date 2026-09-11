package eu.kanade.tachiyomi.extension.en.comix

import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
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
 * Fallback Manga Extension: Comix.to
 *
 * Compatible with Mihon, Tachiyomi, and ShonenX (via dartotsu_extension_bridge).
 * Extends [ParsedHttpSource] and implements [ConfigurableSource].
 */
class Comix : ParsedHttpSource(), ConfigurableSource {

    override val name = "Comix"

    override val baseUrl = "https://comix.to"

    override val lang = "en"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")

    // ============================== Popular Manga ==============================

    override fun popularMangaRequest(page: Int): Request {
        // TODO: Confirm the popular/browse endpoint path on comix.to
        // E.g.: "$baseUrl/browse?sort=views&page=$page" or "$baseUrl/popular?page=$page"
        val endpoint = "$baseUrl/popular?page=$page"
        return GET(endpoint, headers)
    }

    // TODO: Confirm CSS selector for manga cards on popular page
    override fun popularMangaSelector(): String = "div.manga-card, div.comic-item, article.book-card"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            // TODO: Confirm title selector
            title = element.selectFirst("h3.title a, .comic-title, h2.title")?.text()?.trim() ?: "Unknown Title"

            // TODO: Confirm link href attribute
            val href = element.selectFirst("a[href*='/manga/'], a[href*='/comic/'], a[href]")?.attr("href") ?: ""
            setUrlWithoutDomain(href)

            // TODO: Confirm cover thumbnail attribute
            thumbnail_url = element.selectFirst("img[data-src], img[src]")?.let { img ->
                img.attr("abs:data-src").ifEmpty { img.attr("abs:src") }
            }
        }
    }

    // TODO: Confirm next page pagination selector
    override fun popularMangaNextPageSelector(): String? = "ul.pagination li.active + li a, a[rel=next]"

    // ============================== Latest Updates ==============================

    override fun latestUpdatesRequest(page: Int): Request {
        // TODO: Confirm latest updates endpoint path on comix.to
        // E.g.: "$baseUrl/latest-updates?page=$page" or "$baseUrl/browse?sort=latest&page=$page"
        val endpoint = "$baseUrl/latest?page=$page"
        return GET(endpoint, headers)
    }

    // TODO: Confirm selector for latest items (defaults to popular selector)
    override fun latestUpdatesSelector(): String = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = popularMangaNextPageSelector()

    // ============================== Search Manga ==============================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        // TODO: Confirm search endpoint and parameters on comix.to
        // E.g.: "$baseUrl/search?q=$query&page=$page" or "$baseUrl/api/search"
        val endpoint = "$baseUrl/search?q=$query&page=$page"
        return GET(endpoint, headers)
    }

    // TODO: Confirm search result item selector
    override fun searchMangaSelector(): String = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector(): String? = popularMangaNextPageSelector()

    // ============================== Manga Details ==============================

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            // TODO: Confirm manga title selector on details page
            title = document.selectFirst("h1.comic-title, h1.manga-name, .series-title")?.text()?.trim() ?: "Unknown Title"

            // TODO: Confirm cover image selector
            thumbnail_url = document.selectFirst(".manga-poster img, .cover img")?.let { img ->
                img.attr("abs:data-src").ifEmpty { img.attr("abs:src") }
            }

            // TODO: Confirm author selector
            author = document.selectFirst(".author, a[href*='/author/'], .creator")?.text()?.trim()
            artist = document.selectFirst(".artist, a[href*='/artist/']")?.text()?.trim() ?: author

            // TODO: Confirm genre tags selector
            genre = document.select(".genres a, .tags a, a[href*='/genre/']").joinToString(", ") { it.text().trim() }

            // TODO: Confirm description / synopsis selector
            description = document.selectFirst(".synopsis, .description, .comic-description")?.text()?.trim()

            // TODO: Confirm publication status selector
            val statusText = document.selectFirst(".status, .publication-status")?.text() ?: ""
            status = when {
                statusText.contains("Ongoing", ignoreCase = true) -> SManga.ONGOING
                statusText.contains("Completed", ignoreCase = true) -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
        }
    }

    // ============================== Chapter List ==============================

    // TODO: Confirm selector for chapter rows on comix.to
    override fun chapterListSelector(): String = "ul.chapter-list li, div.chapters div.chapter-item"

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            // TODO: Confirm chapter link selector
            val link = element.selectFirst("a[href]") ?: element
            setUrlWithoutDomain(link.attr("href"))

            // TODO: Confirm chapter title and number formatting
            val titleText = link.selectFirst(".chapter-title, .title")?.text()?.trim() ?: link.text().trim()
            name = titleText

            val numRegex = Regex("""(?:chapter|ch\.?)\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
            chapter_number = numRegex.find(titleText)?.groupValues?.get(1)?.toFloatOrNull() ?: -1f

            // TODO: Confirm chapter date upload formatting
            val dateText = element.selectFirst(".chapter-date, .date, span.time")?.text()?.trim()
            date_upload = parseChapterDate(dateText)
        }
    }

    private fun parseChapterDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(dateStr)?.time ?: 0L
        }.getOrDefault(0L)
    }

    // ============================== Page List ==============================

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()

        // TODO: Confirm reader page image selectors on comix.to
        // If static images in reader container:
        val imgElements = document.select("div.reader-images img, div#chapter-images img, .page-image")

        if (imgElements.isNotEmpty()) {
            imgElements.forEachIndexed { index, element ->
                val imgUrl = element.attr("abs:data-src").ifEmpty { element.attr("abs:src") }
                if (imgUrl.isNotBlank()) {
                    pages.add(Page(index, "", imgUrl))
                }
            }
        } else {
            // TODO: If images are loaded via JSON/Script payload in the page (e.g. window.__DATA__ = {...})
            // Parse script tag and populate Page objects
        }

        return pages
    }

    override fun imageUrlParse(document: Document): String = ""

    // ============================== Preferences ==============================

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val qualityPref = ListPreference(screen.context).apply {
            key = "pref_image_quality"
            title = "Image Quality"
            entries = arrayOf("Original", "High", "Medium", "Data Saver")
            entryValues = arrayOf("original", "high", "medium", "saver")
            setDefaultValue("original")
            summary = "%s"
        }
        screen.addPreference(qualityPref)
    }
}
