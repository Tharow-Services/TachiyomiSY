package eu.kanade.tachiyomi.source.online

import android.content.Context
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.online.all.MangaDexFactory
import eu.kanade.tachiyomi.source.online.all.lanraragi.Lanraragi
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

object InternalExtensions: SourceFactory {
    private fun createSources(context: Context): List<Source> = listOf(
        Lanraragi(context, LANRARAGI_ONE_SOURCE_ID),
        Lanraragi(context, LANRARAGI_TWO_SOURCE_ID),
        Lanraragi(context, LANRARAGI_THREE_SOURCE_ID),
        GrrlPower(context, GRRL_POWER_COMIC_SOURCE_ID),
        QuestionableContent(context, QUESTIONABLE_CONTENT_SOURCE_ID),
    )

    override fun createSources(): List<Source> {
        return createSources(Injekt.get<Context>())
    }
    fun loadResults(context: Context): List<LoadResult> = listOf(
        LoadResult.Success(getInternalExt(context)),
        LoadResult.Success(MangaDexFactory.getExtension(),)
    )

    fun getInternalExt(context: Context): Extension.Installed {
        return Extension.Installed(
            name = "Internal Sources",
            pkgName = "",
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            libVersion = 1.6,
            lang = "all",
            isNsfw = false,
            pkgFactory = "eu.kanade.tachiyomi.source.online.InternalExtensions",
            sources = createSources(context),
            icon = ContextCompat.getDrawable(context, R.mipmap.ic_launcher),
            hasUpdate = false,
            isObsolete = false,
            isShared = false,
            store = INTERNAL_EXTENSION_STORE,
            isRedundant = false
        )
    }

     val INTERNAL_EXTENSION_STORE = ExtensionStore(
        indexUrl = "mihon://extension-store?url=https%3A%2F%2Fgithub.com%2Fkeiyoushi%2Fextensions%2Fraw%2Frepo%2Findex.pb",
        name = "Internal Extension Store",
        badgeLabel = "Badge Label",
        signingKey = "",
        contact = ExtensionStore.Contact(
            website = "",
            discord = null
        ),
        isLegacy = true,
        extensionListUrl = null
    )

}
