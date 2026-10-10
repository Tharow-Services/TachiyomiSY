package eu.kanade.tachiyomi.source.online

import android.content.Context
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
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


