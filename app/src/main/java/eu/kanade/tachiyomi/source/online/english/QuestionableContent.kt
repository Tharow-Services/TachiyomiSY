package eu.kanade.tachiyomi.source.online.english

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.TextInterceptor
import eu.kanade.tachiyomi.source.online.TextInterceptorHelper
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.util.Date

class QuestionableContent(val context: Context, override val id: Long,
                          override val name: String = "Questionable Content",
                          override val baseUrl: String = "https://www.questionablecontent.net",
                          override val lang: String = "en",
                          override val supportsLatest: Boolean = false,
                          override var url: String = "/archive.php",
                          override var title: String = name,
                          override var thumbnail_url: String? = "https://i.ibb.co/ZVL9ncS/qc-teh.png",
                          override var artist: String? = "Jeph Jacques",
                          override var author: String? = artist,
                          override var status: Int = SManga.ONGOING,
                          override var description: String? = "An internet comic strip about romance and robots",
                          override var genre: String? = "",
                          override var update_strategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
                          override var initialized: Boolean = true,
                          override var memo: JsonObject = JsonObject(mapOf()),
                          override val originalTitle: String = title,
                          override val originalAuthor: String? = author,
                          override val originalArtist: String? = artist,
                          override val originalThumbnailUrl: String? = thumbnail_url,
                          override val originalDescription: String? = description,
                          override val originalGenre: String? = genre,
                          override val originalStatus: Int = status,

                          ) : HttpSource(), ConfigurableSource, SManga {

    override val client: OkHttpClient = network.client.newBuilder()
        .addInterceptor(TextInterceptor())
        .build()

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("source_$id", 0x0000)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return super.getPageList(chapter)
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        return super.getMangaUpdate(manga, chapters, fetchDetails, fetchChapters)
    }

    override suspend fun getSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        return super.getSearchManga(page, query, filters)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return super.getLatestUpdates(page)
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        return super.getPopularManga(page)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()

        val chapters = document.select("""div#container a[href^="view.php?comic="]""")
            .map { element ->
                val chapterUrl = element.attr("href")
                val number = URL_REGEX.find(chapterUrl)!!.groupValues[1]

                SChapter.create().apply {
                    setUrlWithoutDomain("/$chapterUrl")
                    name = element.text()
                    chapter_number = number.toFloat()
                }
            }
            .distinct()

        if (chapters.isNotEmpty()) {
            val firstChapter = chapters.first()
            if (firstChapter.url != preferences.getString(LAST_CHAPTER_URL, null)) {
                val date = Date().time
                firstChapter.date_upload = date
                preferences.edit()
                    .putString(LAST_CHAPTER_URL, firstChapter.url)
                    .putLong(LAST_CHAPTER_DATE, date)
                    .apply()
            } else {
                firstChapter.date_upload = preferences.getLong(LAST_CHAPTER_DATE, 0L)
            }
        }

        return chapters
    }

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val pages = document.select("#strip").mapIndexed { i, element ->
            Page(i, imageUrl = element.attr("abs:src"))
        }.toMutableList()

        if (showAuthorsNotesPref()) {
            val str = document.selectFirst("#newspost")?.html()
            if (!str.isNullOrEmpty()) {
                pages.add(Page(pages.size, imageUrl = TextInterceptorHelper.createUrl("Author's Notes from $author", str)))
            }
        }
        return pages
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    private fun showAuthorsNotesPref() = preferences.getBoolean(SHOW_AUTHORS_NOTES_KEY, false)

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val authorsNotesPref = SwitchPreferenceCompat(screen.context).apply {
            key = SHOW_AUTHORS_NOTES_KEY
            title = "Show author's notes"
            summary = "Enable to see the author's notes at the end of chapters (if they're there)."
            setDefaultValue(false)
        }
        screen.addPreference(authorsNotesPref)
    }

    companion object {
        private const val LAST_CHAPTER_URL = "QC_LAST_CHAPTER_URL"
        private const val LAST_CHAPTER_DATE = "QC_LAST_CHAPTER_DATE"
        private const val SHOW_AUTHORS_NOTES_KEY = "showAuthorsNotes"
        private val URL_REGEX = """view\.php\?comic=(.*)""".toRegex()
    }
}
