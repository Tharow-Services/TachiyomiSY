package eu.kanade.tachiyomi.source.online.english

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
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
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Calendar
import java.util.Locale

class GrrlPower(
    val context: Context,
    override val id: Long,
    override val name: String = "Grrl Power Comic",
    override val supportsLatest: Boolean = false,
    override val lang: String = "en",
    override val baseUrl: String = "https://grrlpowercomic.com",
    override var url: String ="/archive",
    override var title: String = "Grrl Power Comic",
    override var thumbnail_url: String? = "https://static.tvtropes.org/pmwiki/pub/images/rsz_grrl_power.pngh",
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
    override var initialized: Boolean = true
) :
    HttpSource(), ConfigurableSource, Source, UnmeteredSource, SManga {
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)

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
    ): SMangaUpdate = SMangaUpdate(manga, chapters = (2010..currentYear).flatMap { year ->
        if (fetchChapters) {
            client.newCall(GET("$baseUrl/archive/?archive_year=$year")).execute().use { response ->
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
            }
        } else chapters
    }.sortedByDescending { it.date_upload })

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

        return pages
    }

    override fun setupPreferenceScreen(screen: eu.kanade.tachiyomi.source.PreferenceScreen) {}
}
