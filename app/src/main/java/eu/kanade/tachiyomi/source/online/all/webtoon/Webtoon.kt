package eu.kanade.tachiyomi.source.online.all.webtoon

import android.content.Context
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
import eu.kanade.tachiyomi.source.online.DrawableSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.InternalSourceFactory
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ChallengeEpisodeHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ChallengeEpisodeListHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ChallengeTitleHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.SearchV2Result
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceInfo
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceInfoHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceResponse
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class Webtoon(val context: Context, override val lang: String) : HttpSource(), UnmeteredSource, LoginSource, DrawableSource {
    override val baseUrl: String = "https://global.apis.naver.com/lineWebtoon/webtoon"
    override val name: String = "Webtoon"
    override val iconRes: Int = R.mipmap.ic_webtoon_source
    override val supportsLatest: Boolean = false
    override val requiresLogin: Boolean = false
    override val twoFactorAuth: LoginSource.AuthSupport = LoginSource.AuthSupport.NOT_SUPPORTED
    var authenticationCode: String? = null

    override fun headersBuilder() = Headers.Builder().add("User-Agent", USER_AGENT)
        //.add("cookie", "NEO_SES=\"JV3YIC3NTtZJUwKRpFqi3qqxWfwO7TgICX2OI6Qvp0TEu/P1nD8vsmqypul+q5jf6WghsycGImHHP2JYzRqSxRYScFBeABuDeOoHyW5aS257/jUjxFs1qc6ZFE21hieWdY3PVq97x1ldevFZKJAfUziiaA3OzoGNDgluyHIPR5HnJhMWvqFywN86Z0/58xelmngouIXNZFeBTIzbZ61y0CUqJsubXeU2hwHdr22XzL7eBzEqwiKL7gsHNB3NRTwzxbkwaPeJOFOPXm2HXtoGSYxJzx5Jde9tvuZ9JqMmx3hkNLJ8IUhm3jXHG5onQL25eCG74zr0Q+4bbdiLgdYq+GP/rRsxaRM7dTBHwKVxON0t5zn7z1lkAi4hrHt2GDnBNl/ZUHTlkhbI439Cs9Z2i/d4pFl/EzdTPBJg9sb+PIc=\"; NEO_CHK=PZTO+AJBfWvbsLj1CVg9skPq9g4O0mU6Jpqtyfv36LRtqOYAAKpUF9YpT0zPPWJGVInLDdwaKYqNjSzpv2YQ0+t/tG3XgCRym5OX1tBe7Om8pDHGqSp0unm+zMCOc5Cfkljr5wY2UoBYskJD9D+pgQ==")
        .add("wtu", HMACSubstitution.getWTU("9e8cc925d93016eb"))


    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(PlatformInterceptor(this.lang, this.lang, true))
        .addInterceptor(HMACInterceptor).build()
    override val id: Long = super.id
    override val versionId: Int = super.versionId
    val json: Json = Json
    private var nullableServiceInfo: ServiceInfo? = null
    private val serviceInfo: ServiceInfo
        get() {
            if (nullableServiceInfo==null) {
                nullableServiceInfo = with(json) {
                    client.newCall(GET("$baseUrl/serviceInfo.json", headers)).execute()
                        .parseAs<ServiceResponse<ServiceInfoHolder>>().getResult().serviceInfo
                }
            }
            return nullableServiceInfo!!
        }




    private fun getThumbnailUrl(path: String): String {
        return serviceInfo.imageDomain+path
    }


    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val titleNo = manga.url.toHttpUrl().queryParameter("titleNo")
        val details = if (fetchDetails) {
            with (json) {client.newCall(GET(manga.url, headers)).execute().parseAs<ServiceResponse<ChallengeTitleHolder>>().getResult().challengeTitle.asSManga(serviceInfo.imageDomain)}
        } else manga
        val newChapters = if (fetchChapters) {
            with (json) {client.newCall(GET("$baseUrl/challengeTitleHomeEpisodeListV2?titleNo=$titleNo&offset=0&pageSize=30&ordering=OLDEST")).execute().parseAs<ServiceResponse<ChallengeEpisodeListHolder>>().getResult().challengeEpisodeList}.
                map { it.asChapter("$baseUrl/challengeViewerEpisodeV2?readingRequestLanguage=ENGLISH&titleNo=$titleNo&episodeNo=") }
        } else chapters

        return SMangaUpdate(details, newChapters)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return with (json) {client.newCall(GET(chapter.url, headers)).execute().parseAs<ServiceResponse<ChallengeEpisodeHolder>>().getResult()}.challengeEpisode.getPages(serviceInfo.imageUrl)
    }

    override suspend fun getSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        val response = with (json) {client.newCall(GET("$baseUrl/searchAllV2?query=$query&startIndex=$page&pageSize=20", headers)).execute().parseAs<ServiceResponse<SearchV2Result>>()}
        val webtoonSearch = response.getResult().webtoonSearch
        val mangas = response.getResult().challengeSearch.challengeTitleList.map {
            SManga.create().apply {
                url = "$baseUrl/challengeTitleHomeMainV2?titleNo=${it.titleNo}"
                thumbnail_url = getThumbnailUrl(it.thumbnailImageUrl)
                title = it.readingTitle
            }
        }
        return MangasPage(mangas, webtoonSearch.hasMore)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return super.getLatestUpdates(page)
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        return super.getPopularManga(page)
    }

    override fun isLogged() = !authenticationCode.isNullOrEmpty()

    override fun getUsername(): String = "dev.linewebtoon@gmail.com"

    override fun getPassword(): String = "dnpqxns12!@"

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


}


