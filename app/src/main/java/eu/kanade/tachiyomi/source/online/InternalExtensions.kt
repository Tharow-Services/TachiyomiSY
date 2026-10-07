package eu.kanade.tachiyomi.source.online

import android.content.Context
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.all.MangaDexFactory
import eu.kanade.tachiyomi.source.online.all.comicfury.ComicFury
import eu.kanade.tachiyomi.source.online.all.lanraragi.Lanraragi
import eu.kanade.tachiyomi.source.online.all.webtoon.Webtoon
import eu.kanade.tachiyomi.source.online.english.GrrlPower
import eu.kanade.tachiyomi.source.online.english.QuestionableContent
import exh.source.GRRL_POWER_COMIC_SOURCE_ID
import exh.source.LANRARAGI_ONE_SOURCE_ID
import exh.source.LANRARAGI_THREE_SOURCE_ID
import exh.source.LANRARAGI_TWO_SOURCE_ID
import exh.source.QUESTIONABLE_CONTENT_SOURCE_ID
import mihon.domain.extension.model.ExtensionStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

val INTERNAL_EXTENSIONS: List<InternalSourceFactory> =
    listOf(
        InternalExtension,
        MangaDexFactory,
        DebugSource,
        ComicFury,
        Webtoon
    )

interface InternalSourceFactory: SourceFactory {
    override fun createSources(): List<Source> {
        return createSources(Injekt.get<Context>())
    }
    val name: String
    val lang: String
    val isNsfw: Boolean
    val icon: Int?
    val pkgFactory: String
        get() = ""
    val pkgName: String
        get() = ""

    fun createSources(context: Context): List<Source>
    fun asInstalledExtension(context: Context)=Extension.Installed(
        name = name,
        pkgName = pkgName,
        versionName = BuildConfig.VERSION_NAME,
        versionCode = BuildConfig.VERSION_CODE.toLong(),
        libVersion = Double.NaN,
        lang = lang,
        isNsfw = isNsfw,
        pkgFactory = pkgFactory,
        sources = createSources(context),
        icon = ContextCompat.getDrawable(context, icon?:R.mipmap.ic_launcher),
        hasUpdate = false,
        isObsolete = false,
        isShared = false,
        isRedundant = false,
        store = ExtensionStore(
            indexUrl = "",
            name = "Internal Extension Store",
            badgeLabel = "",
            signingKey = "",
            contact = ExtensionStore.Contact(
                website = "",
                discord = null
            ),
            isLegacy = true,
            extensionListUrl = null
        ),
    )
}
interface DrawableSource {
    val id: Long
    val iconRes: Int
}

object InternalExtension: InternalSourceFactory{
    override val name: String = "Internal Extensions"
    override val lang: String = "all"
    override val isNsfw: Boolean = false
    override val icon: Int? = null
    override val pkgFactory: String =""
    override val pkgName: String = "Built-in Extensions for use with TachiyomiSY"

     override fun createSources(context: Context): List<Source> = listOf(
        Lanraragi(context, LANRARAGI_ONE_SOURCE_ID),
        Lanraragi(context, LANRARAGI_TWO_SOURCE_ID),
        Lanraragi(context, LANRARAGI_THREE_SOURCE_ID),
        GrrlPower(context, GRRL_POWER_COMIC_SOURCE_ID),
        QuestionableContent(context, QUESTIONABLE_CONTENT_SOURCE_ID),
    )
}

class DebugSource: Source {
    override val id: Long = 1L
    override val name: String = "Debug Source"
    override val supportsLatest: Boolean = false
    override val lang: String = "all"

    override suspend fun getPopularManga(page: Int) = MangasPage(listOf(), false)
    override suspend fun getLatestUpdates(page: Int)=getPopularManga(page)
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList, ) = getPopularManga(page)
    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ) = SMangaUpdate(manga, chapters)
    override suspend fun getPageList(chapter: SChapter): List<Page> = listOf()
    companion object: InternalSourceFactory {
        override val name: String = "Debug Extension"
        override val lang: String = "all"
        override val isNsfw: Boolean = false
        override val icon: Int? = null
        override val pkgFactory: String = ""
        override fun createSources(context: Context) = listOf(DebugSource())

    }
}


