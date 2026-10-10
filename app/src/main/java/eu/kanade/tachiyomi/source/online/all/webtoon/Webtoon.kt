package eu.kanade.tachiyomi.source.online.all.webtoon

import android.content.Context
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import eu.kanade.tachiyomi.source.online.DrawableSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.InternalSourceFactory
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.PopularTrendingTitleList
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.SearchV2Result
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceInfo
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceInfoHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceResponse
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleEpisodeList
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleEpisodeStub
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleHome
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleItemStub
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ViewerEpisode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.lang.Math.ceilDiv

class Webtoon(val context: Context, override val lang: String) : HttpSource(), UnmeteredSource, LoginSource, DrawableSource {
    override val baseUrl: String = "https://global.apis.naver.com/lineWebtoon/webtoon"
    override val name: String = "Webtoon"
    val locale: String = lang
    override val iconRes: Int = R.mipmap.ic_webtoon_source
    override val supportsLatest: Boolean = true
    override val requiresLogin: Boolean = false
    override val twoFactorAuth: LoginSource.AuthSupport = LoginSource.AuthSupport.NOT_SUPPORTED
    var authenticationCode: String? = null
    val useInternalServiceInfo: Boolean = true
    var imageQualitySetting: ImageQualitySetting = ImageQualitySetting.NORMAL
    enum class ImageQualitySetting(val type: String?){ NORMAL("q70"), HIGH("opti"), REMOVE(null) }

    override fun headersBuilder() = Headers.Builder().add("User-Agent", USER_AGENT)
        //.add("cookie", "NEO_SES=\"JV3YIC3NTtZJUwKRpFqi3qqxWfwO7TgICX2OI6Qvp0TEu/P1nD8vsmqypul+q5jf6WghsycGImHHP2JYzRqSxRYScFBeABuDeOoHyW5aS257/jUjxFs1qc6ZFE21hieWdY3PVq97x1ldevFZKJAfUziiaA3OzoGNDgluyHIPR5HnJhMWvqFywN86Z0/58xelmngouIXNZFeBTIzbZ61y0CUqJsubXeU2hwHdr22XzL7eBzEqwiKL7gsHNB3NRTwzxbkwaPeJOFOPXm2HXtoGSYxJzx5Jde9tvuZ9JqMmx3hkNLJ8IUhm3jXHG5onQL25eCG74zr0Q+4bbdiLgdYq+GP/rRsxaRM7dTBHwKVxON0t5zn7z1lkAi4hrHt2GDnBNl/ZUHTlkhbI439Cs9Z2i/d4pFl/EzdTPBJg9sb+PIc=\"; NEO_CHK=PZTO+AJBfWvbsLj1CVg9skPq9g4O0mU6Jpqtyfv36LRtqOYAAKpUF9YpT0zPPWJGVInLDdwaKYqNjSzpv2YQ0+t/tG3XgCRym5OX1tBe7Om8pDHGqSp0unm+zMCOc5Cfkljr5wY2UoBYskJD9D+pgQ==")
        .add("wtu", HMACManager.getWTUHeader("9e8cc925d93016eb"))


    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(PlatformInterceptor(this))
        .addInterceptor(HMACManager())
        .build()
    override val id: Long = super.id
    override val versionId: Int = super.versionId
    val json: Json = Json {
        if (!BuildConfig.DEBUG) {
            ignoreUnknownKeys = true
        }
    }
    private var nullableServiceInfo: ServiceInfo? = null
    private val serviceInfo: ServiceInfo
        get() {
            if (nullableServiceInfo==null) {
                nullableServiceInfo = if (useInternalServiceInfo) {
                    json.decodeFromStream(context.resources.openRawResource(R.raw.service_info))
                } else {
                    makeReqGetRes<ServiceInfoHolder>("$baseUrl/service_info.json").serviceInfo
                }
            }
            return nullableServiceInfo!!
        }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ) = SMangaUpdate(
            if (fetchDetails.or(!manga.initialized)) makeReqGetRes<TitleHome>(manga.url).toSManga() else manga,
            if (fetchChapters) { manga.getChapters() } else chapters
        )


    private fun SManga.getChapters(): List<SChapter> {
        if (!this.initialized) return listOf()
        val titleHolder = json.decodeFromJsonElement(TitleHome.serializer(), this.memo["challengeTitleHolder"] ?: throw Exception("Memo Object not present"))
        val titleNo = titleHolder.title.titleNo
        val count =  titleHolder.episodeMeta.totalEpisodeCount
        val offsetCounts = ceilDiv(count, 30L)
        val api = baseUrl + if (titleHolder.title.downloadable!=null) "/titleHomeEpisodeListV3" else "/challengeTitleHomeEpisodeListV2"
        val result = ArrayList<SChapter>(count.toInt())
        for (i in (0..offsetCounts)) {
            makeReqGetRes<TitleEpisodeList>("$api?titleNo=$titleNo&offset=${i*30}&pageSize=30&ordering=LATEST").episodeList
                .forEach { result.add(it.toSChapter(titleNo)) }
        }
        return result
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return makeReqGetRes<ViewerEpisode>(chapter.url).challengeEpisode.imageList.map {
            Page(index = it.sortOrder.toInt(), imageUrl = serviceInfo.imageDomain+it.url)
    }}

    override suspend fun getSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        val result: SearchV2Result = makeReqGetRes("$baseUrl/searchAllV2?query=$query&startIndex=$page&pageSize=20")
        val mangas = result.challengeSearch.titleList.map {it.toSManga()} + result.webtoonSearch.titleList.map {it.toSManga()}
        return MangasPage(mangas, result.webtoonSearch.hasMore.or(result.challengeSearch.hasMore))
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return MangasPage(makeReqGetRes<PopularTrendingTitleList>("$baseUrl/trendingChartTitles.json?size=10").titleList.map { it.toSManga() }, false)
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        return MangasPage(makeReqGetRes<PopularTrendingTitleList>("$baseUrl/getPopularRankingTitleList?count=10").titleList.map { it.toSManga() }, false)
    }

    override fun getFilterList(): FilterList {
        return super<HttpSource>.getFilterList()
    }

    override fun isLogged() = !authenticationCode.isNullOrEmpty()

    override fun getUsername(): String = "4anzrgez@t-0.cc"

    override fun getPassword(): String = "ZmR6L82Q2zMU%u"

    override suspend fun login(
        username: String,
        password: String,
        twoFactorCode: String?,
    ): Boolean {





        return false
    }

    override suspend fun logout(): Boolean {
        return false
    }

    companion object: InternalSourceFactory {
        private const val USER_AGENT = "nApps (Android 14; motorola edge 30 pro; linewebtoon; 3.9.12)"

        override fun createSources(context: Context) =
            listOf(Webtoon(context, "en"))

        override val name: String = "Webtoons Internal Extension"
        override val lang: String = "all"
        override val isNsfw: Boolean = false
        override val icon: Int = R.mipmap.ic_webtoon_source
    }

    private inline fun <reified T> makeReqGetRes(url: String): T {
        return with (json)  {client.newCall(GET(url, headers)).execute().parseAs<ServiceResponse<T>>().getResult()}
    }

    private fun TitleHome.toSManga() = SManga.create().apply {
        url = baseUrl + (if (this@toSManga.title.downloadable!=null) "/titleHomeMainV3?titleNo=" else "/challengeTitleHomeMainV2?titleNo=") +this@toSManga.title.titleNo
        title = this@toSManga.title.title
        author = this@toSManga.title.author?.authorName?:this@toSManga.title.authorList?.first()?.authorName?:"Unknown"
        artist = null
        description = this@toSManga.title.sourceSynopsis
        genre = this@toSManga.title.representGenre.displayName
        status = SManga.ONGOING
        thumbnail_url = serviceInfo.imageDomain + this@toSManga.title.thumbnailUrl
        update_strategy = UpdateStrategy.ALWAYS_UPDATE
        initialized = true
        memo = JsonObject(mapOf(Pair("challengeTitleHolder",json.encodeToJsonElement(serializer = serializer(), this@toSManga))))
    }

    private fun TitleItemStub.toSManga(): SManga {
        return SManga.create().apply {
            this@apply.url = if (this@toSManga.contentType=="WEBTOON") "$baseUrl/titleHomeMainV3?titleNo=$titleNo" else "$baseUrl/challengeTitleHomeMainV2?titleNo=$titleNo"
            this@apply.title = this@toSManga.title?: ""
            this@apply.thumbnail_url = serviceInfo.imageDomain+thumbnailImageUrl
            this@apply.author = writingAuthorName
            this@apply.status = SManga.UNKNOWN
            this@apply.description = synopsis
            this@apply.genre = genreDisplayName
            this@apply.update_strategy = UpdateStrategy.ALWAYS_UPDATE
            this@apply.initialized = false
        }
    }

    private fun TitleEpisodeStub.toSChapter(titleNo: Long): SChapter {
        return SChapter.create().apply {
            val internalUrl = baseUrl + if (this@toSChapter.rewardAdEpisode==null) "/challengeViewerEpisodeV1?readingRequestLanguage=ENGLISH" else "/episodeInfo.json?v=4"
            url = "$internalUrl&titleNo=$titleNo&episodeNo=$episodeNo"
            name = episodeTitle
            chapter_number = episodeNo.toFloat()
            date_upload = exposureYmdt?:0L
            scanlator = exposureType
        }
    }
}

