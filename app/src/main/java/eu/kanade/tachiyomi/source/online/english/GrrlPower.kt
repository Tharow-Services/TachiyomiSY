package eu.kanade.tachiyomi.source.online.english

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.AssetInterceptor
import eu.kanade.tachiyomi.source.online.DrawableSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.TextInterceptor
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale

class GrrlPower(
    val context: Context,
    override val id: Long,
    override val name: String = "Grrl Power",
    override val supportsLatest: Boolean = false,
    override val lang: String = "en",
    override val baseUrl: String = "https://grrlpowercomic.com",
    override var url: String ="/archive",
    override var title: String = name,
    override var thumbnail_url: String? = AssetInterceptor.createUrl("thumbnails/grrl_power_comic.jpeg"),
    override var artist: String? = "David Barrack",
    override var author: String? = artist,
    override var status: Int = SManga.ONGOING,
    override var description: String? = "Grrl Power is a comic about a crazy nerdette that becomes a superheroine. Humor, action, cheesecake, beefcake, 'explosions, and maybe some drama. Possibly ninjas.",
    override var genre: String? = "superhero, humor, action",
    override var update_strategy: UpdateStrategy= UpdateStrategy.ALWAYS_UPDATE,
    override var memo: JsonObject = JsonObject(mapOf()),
    override val originalTitle: String = title,
    override val originalAuthor: String? = author,
    override val originalArtist: String? = artist,
    override val originalThumbnailUrl: String? = thumbnail_url,
    override val originalDescription: String? = description,
    override val originalGenre: String? = genre,
    override val originalStatus: Int = status,
    override var initialized: Boolean = true,
    override val iconRes: Int = R.mipmap.ic_grrlpower_source
) :
    HttpSource(), ConfigurableSource, Source, UnmeteredSource, SManga, DrawableSource {
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(TextInterceptor())
        .addInterceptor(AssetInterceptor())
        .build()
        private val dateFormat by lazy {
        SimpleDateFormat("MMM dd yyyy", Locale.US)
    }

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("source_$id", 0x0000)
    }
    override suspend fun getPopularManga(page: Int)=MangasPage(listOf(this), false)
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList) = getPopularManga(page)

    // ============================== Details ==============================
    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean
    ): SMangaUpdate = SMangaUpdate(
        manga = this,
        chapters = if (fetchChapters) {
            (2010..currentYear).flatMap { year -> client.newCall(GET("$baseUrl/archive/?archive_year=$year")).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP error ${response.code}")
                }
                response.asJsoup().getElementsByClass("archive-date").map {
                    val dateStr = "${it.text()} $year"
                    val link = it.nextElementSibling()!!.child(0)
                    SChapter.create().apply {
                        name = link.text()
                        url = (link.absUrl("href"))
                        date_upload = (dateFormat.parse(dateStr) ?: Date.from(Instant.EPOCH)).time
                    }
                }
            } }.sortedBy { it.date_upload }.mapIndexed { index, chapter -> chapter.apply { this.chapter_number = index.toFloat() } }
        } else chapters)

    // =============================== Pages ===============================

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val response = client.newCall(GET(chapter.url)).execute()
        val soup = response.asJsoup()
        val pages = mutableListOf<Page>()

        val comicImg = soup.selectFirst("div#comic img")
        if (comicImg != null) {
            pages.add(
                Page(
                    index = 0,
                    url = chapter.url,
                    imageUrl = comicImg.absUrl("src"),
                ),
            )
        }

        val text = soup.getElementsByClass("entry").html()
        if (text.isNotEmpty() && showAuthorsNotesPref()) {
            pages.add(
                Page(
                    index = pages.size,
                    imageUrl = TextInterceptor.createUrl("Author's Notes from $author", text),
                ),
            )
        }

        return pages
    }

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
        private const val SHOW_AUTHORS_NOTES_KEY = "showAuthorsNotes"
    }
}
