package eu.kanade.tachiyomi.extension.de.novelcool

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class novelcool : ParsedHttpSource() {

    override val id: Long = 10

    override val name = "NovelCool"

    override val baseUrl = "https://de.novelcool.com"

    override val lang = "de"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0")
        .add("Accept-Language", "en-US,en;q=0.5")

    override fun popularMangaSelector() = ".book-item"

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/category/popular.html", headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/category/latest.html", headers)
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        manga.setUrlWithoutDomain(element.select("a").attr("href"))
        manga.title = element.select(".book-pic").attr("title")
        manga.thumbnail_url = element.select("a > img").first().attr("abs:src")

        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun popularMangaNextPageSelector(): String? = null

    override fun latestUpdatesNextPageSelector(): String? = null

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search?name=$query", headers)
    }

    override fun searchMangaSelector() = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        manga.setUrlWithoutDomain(element.select("a").attr("href"))
        manga.title = element.select(".book-pic").attr("title")
        manga.thumbnail_url = element.select("a > img").first().attr("abs:src")

        return manga
    }

    override fun searchMangaNextPageSelector() = ".page-navone > a"

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()

        document.select(".bookinfo-info").let { details ->
            manga.author = details.select(".bookinfo-author > a").attr("title")
            manga.artist = manga.author
            manga.genre = details.select(".bookinfo-category-list a").joinToString { it.text() }
            manga.thumbnail_url = details.select(".bookinfo-pic-img").attr("abs:src")
        }
        manga.status = parseStatus(document.select("div.bookinfo-category-list:nth-child(6) > a:nth-child(2)").attr("href"))
        manga.description = document.select(".bookinfo-summary > span").text()

        return manga
    }
//   TODO: Take the status from the link for multilang
    private fun parseStatus(status: String?) = when {
        status == null -> SManga.UNKNOWN
        status.contains("updated", ignoreCase = true) -> SManga.ONGOING
        status.contains("completed", ignoreCase = true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = ".chapter-item-list > a"

//    TODO:
//    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
//        Observable.just(manga.apply { initialized = true })

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        chapter.setUrlWithoutDomain(element.attr("href"))
        chapter.name = element.attr("title")
//        TODO: Parse Chapter date
//        chapter.date_upload = element.select(".chapter-item-time").first()?.text()?.let { parseChapterDate(it) } ?: 0

        return chapter
    }
//    TODO: fix function for date parsing
    private fun parseChapterDate(date: String): Long {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(date)?.time ?: 0L
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()

        document.select(".sl-page").first().select("option").forEach {
            pages.add(Page(pages.size, it.attr("value")))
        }
        return pages
    }

    override fun imageUrlParse(document: Document): String {
        return document.select("#manga_picid_1").first().attr("abs:src")
    }
}
