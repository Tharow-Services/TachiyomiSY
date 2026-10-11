@file:OptIn(InternalVoyagerApi::class)

package eu.kanade.tachiyomi.source.online.all.webtoon

import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.EditTextPreference
import androidx.preference.SwitchPreferenceCompat
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.PreferenceScreen
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.model.Filter
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
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ImageSecureTokenHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.LoginById
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.PopularTrendingTitleList
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.RSAKey
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.SearchV2Result
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceInfo
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceResponse
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleEpisodeInfoHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleEpisodeList
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleEpisodeStub
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleHome
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleItemStub
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ViewerEpisode
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okio.IOException
import java.lang.Math.ceilDiv
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher

class Webtoon(val context: Context, override val lang: String) : HttpSource(), UnmeteredSource, LoginSource, DrawableSource, ConfigurableSource {
    override val baseUrl: String = BASE_URL
    override val name: String = "Webtoon"
    val locale: String = lang
    override val iconRes: Int = R.mipmap.ic_webtoon_source
    override val supportsLatest: Boolean = true
    override val requiresLogin: Boolean = false
    override val twoFactorAuth: LoginSource.AuthSupport = LoginSource.AuthSupport.NOT_SUPPORTED
    @get:JvmName("getSP")
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("source_$id", 0x0000)
    }

    fun getPreferences(): SharedPreferences {
        return preferences;
    }
    val useInternalServiceInfo
        get() = preferences.getBoolean(PERF_USE_INTERNAL_SERVICE_INFO, true)
    val imageQualitySetting: ImageQualitySetting
        get() = ImageQualitySetting.entries[preferences.getInt(PREF_IMAGE_QUALITY_SETTINGS, 0)]
    val wtu: String by lazy { preferences.getString(PREF_WTU_ANDROID_ID, SecureRandom.getInstanceStrong().nextLong().toHexString())!! }

    override fun headersBuilder() = Headers.Builder()
        .add("User-Agent", preferences.getString("userAgent", USER_AGENT)!!)


    private val cookieJar: CookieStorage = CookieStorage(this)

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(PlatformInterceptor(this))
        .addInterceptor(HMACManager(API_KEY))
        .cookieJar(cookieJar)
        .build()

    override val id: Long = super.id
    override val versionId: Int = super.versionId
    val json: Json = Json {
        if (!BuildConfig.DEBUG) {
            ignoreUnknownKeys = true
        }
    }
    private var nullableServiceInfo: ServiceInfo? = null
    val serviceInfo: ServiceInfo
        get() {
            if (nullableServiceInfo==null) {
                nullableServiceInfo = if (useInternalServiceInfo) {
                    json.decodeFromStream(context.resources.openRawResource(R.raw.service_info))
                } else {
                    WebtoonAPI.ServiceInfo().toResponse(this).serviceInfo
                }
            }
            return nullableServiceInfo!!
        }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        loginFromDisk()
        val manga = if (fetchDetails.or(!manga.initialized)) get<TitleHome>(manga.url).toSManga(manga.url) else manga
        return SMangaUpdate(
            manga,
            if (fetchChapters) { manga.getChapters() } else chapters
        )
    }


    private fun SManga.getChapters(): List<SChapter> {
        if (!this.initialized) return listOf()
        val titleHolder = json.decodeFromJsonElement(TitleHome.serializer(), this.memo["challengeTitleHolder"] ?: throw Exception("Memo Object not present"))
        val titleNo = titleHolder.title.titleNo
        val count =  titleHolder.episodeMeta.totalEpisodeCount
        val offsetCounts = ceilDiv(count, 30L)
        val api = baseUrl + if (titleHolder.title.downloadable!=null) "/titleHomeEpisodeListV3" else "/challengeTitleHomeEpisodeListV2"
        val result = ArrayList<SChapter>(count.toInt())
        for (i in (0..offsetCounts)) {
            get<TitleEpisodeList>("$api?titleNo=$titleNo&offset=${i*30}&pageSize=30&ordering=LATEST").episodeList
                .forEach { result.add(it.toSChapter(titleNo)) }
        }
        return result
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        return if (chapter.url.contains("json")) {
            isLogged()
            val episode = get<TitleEpisodeInfoHolder>(chapter.url).episodeInfo

            val eop = episode.imageInfo.map {
                Page(index = it.sortOrder.toInt(), imageUrl = (if(episode.product) serviceInfo.productImageDomain else serviceInfo.imageDomain )+it.url)
            }
            if (episode.product) eop.drop(1) else eop
        } else {
            get<ViewerEpisode>(chapter.url).challengeEpisode.imageList.map {
                Page(index = it.sortOrder.toInt(), imageUrl = serviceInfo.imageDomain+it.url)
            }

        }}

    override fun getFilterList() = FilterList(
        object:Filter.CheckBox(name = "Originals", state = true){},
        object:Filter.CheckBox(name = "Challenges", state = true){}
    )
    override suspend fun getSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): MangasPage {
        val result: SearchV2Result = get("$baseUrl/searchAllV2?query=$query&startIndex=$page&pageSize=20")
        val mangas: MutableList<SManga> = ArrayList()
        var hasMore = false
        filters.forEach { filter ->
            when (filter.name) {
                "Originals" -> {
                    mangas += result.webtoonSearch.titleList.map {it.toSManga(true)}
                    if (result.webtoonSearch.hasMore) hasMore = true
                }
                "Challenges" -> {
                    mangas += result.challengeSearch.titleList.map { it.toSManga(false) }
                    if (result.challengeSearch.hasMore) hasMore = true
                }
            }
        }
        return MangasPage(mangas, hasMore)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        return MangasPage(get<PopularTrendingTitleList>("$baseUrl/trendingChartTitles.json?size=10").titleList.map { it.toSManga() }, false)
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        return MangasPage(get<PopularTrendingTitleList>("$baseUrl/getPopularRankingTitleList?count=10").titleList.map { it.toSManga(true) }, false)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        screen.addPreference(SwitchPreferenceCompat(screen.context).apply {
            key = PERF_USE_INTERNAL_SERVICE_INFO
            title = "Load ServiceInfo from Disk"
            summary = "Will Load Api Service Info Data from disk instead of from api " +
                "change if image domain url has changed"
            setDefaultValue(true)
        })
        screen.addPreference(SwitchPreferenceCompat(screen.context).apply {
            key = PREF_TRUST_COOKIES
            title = "Should we trust set-cookie header"
            summary = "False by default linewebtoon app doesn't update NEO_SES value from server or from any other cookie sent back"
            setDefaultValue(false)
        })
        screen.addPreference(screen.editTextPreference(
            key = PREF_WTU_ANDROID_ID,
            title = "WTU API tracking code",
            default = wtu,
            summary = "wtu is a required tracking code based on android SSAID value click to set custom one",
            isPassword = false,
            refreshSummary = false
        ))
        screen.addPreference(screen.editTextPreference(
            key = PREF_USERNAME,
            title = "username",
            default = getUsername(),
            summary = "Account Username",
            isPassword = false,
            refreshSummary = false
        ))
        screen.addPreference(screen.editTextPreference(
            key = PREF_PASSWORD,
            title = "Password",
            default = getPassword(),
            summary = "Password",
            isPassword = true,
            refreshSummary = false
        ))
    }


    // ---------- Start of Account ------ //
    var userAccount: LoginById? = null

    override fun isLogged(): Boolean {
        val result = preferences.getBoolean(PREF_IS_LOGGED, false)
        if (result) loginFromDisk()
        return result
    }

    override fun getUsername(): String = preferences.getString(PREF_USERNAME,"4anzrgez@t-0.cc")!!

    override fun getPassword(): String = preferences.getString(PREF_PASSWORD, "ZmR6L82Q2zMU%u")!!

    private fun debugLoginSetup() {
        preferences.edit(true) {
            putString(PREF_USERNAME, "dev.linewebtoon@gmail.com")
            putString(PREF_PASSWORD, "dnpqxns12!@")
        }}


    private fun loginFromDisk() {
        userAccount = LoginById(preferences)
        val cookieBuilder = Cookie.Builder()
        cookieBuilder.name("NEO_SES").domain(API_DOMAIN)
        cookieBuilder.value(userAccount!!.ses)
        cookieJar.saveCookie(API_DOMAIN, cookieBuilder.build(), true)
    }

    override suspend fun login(
        username: String,
        password: String,
        twoFactorCode: String?,
    ): Boolean {
        val rsaKey = get<RSAKey>("$baseUrl/getRsaKey.json?v=2")
        val builder = FormBody.Builder()
            .add("serviceZone", "GLOBAL")
            .add("loginType", "EMAIL")
            .add("v", "3")
            .add("language", "en")
            .add("encnm", rsaKey.keyName)
            .add("encpw", getRsaEncryptedPassword(username, password, rsaKey))
        userAccount = post("$baseUrl/loginById.json", builder.build())
        userAccount!!.saveTo(preferences)
        val cookieBuilder = Cookie.Builder()
        cookieBuilder.name("NEO_SES").domain(API_DOMAIN)
        cookieBuilder.value(userAccount!!.ses)
        cookieJar.saveCookie(API_DOMAIN, cookieBuilder.build(), true)
        return true
    }

    class CookieStorage(val self: Webtoon): CookieJar {
        private val storage: HashMap<String, List<Cookie>> = HashMap()
        val trustRemoteCookies: Boolean by lazy {
            self.getPreferences().getBoolean(PREF_TRUST_COOKIES, false)
        }
        var currentSecureImageTokenExpirationTime: Long = 0L

        fun saveCookie(host: String, cookie: Cookie, force: Boolean = false) {
            if (force) {
                storage[host] = listOf(cookie)
                return
            }
            if (storage.containsKey(host)) {
                val currentCookies = storage[host]!!.toMutableList()
                currentCookies.add(cookie)
                storage[host] = currentCookies
            }
        }

        private fun saveNewSecureImageCookie() {
            val secureToken = self.get<ImageSecureTokenHolder>("$BASE_URL/getImageSecureToken.json")
            currentSecureImageTokenExpirationTime = secureToken.getExpirationTime()
            saveCookie("prowebtoon-phinf.pstatic.net", secureToken.toCookie("prowebtoon-phinf.pstatic.net"), true)
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (trustRemoteCookies) storage[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            if (url.host=="prowebtoon-phinf.pstatic.net"&&currentSecureImageTokenExpirationTime<= System.currentTimeMillis() &&self.isLogged()) {
                saveNewSecureImageCookie()
            }
            return storage[url.host] ?:listOf()
        }
    }

    override suspend fun logout(): Boolean {
        return false
    }

    // ---------- End of Account --------- //


    companion object: InternalSourceFactory {
        private const val USER_AGENT = "nApps (Android 14; Android Emulator; linewebtoon; 3.9.12)"
        const val API_DOMAIN = "global.apis.naver.com"
        const val BASE_URL = "https://$API_DOMAIN/lineWebtoon/webtoon"
        const val API_KEY = "gUtPzJFZch4ZyAGviiyH94P99lQ3pFdRTwpJWDlSGFfwgpr6ses5ALOxWHOIT7R1"
        const val PERF_USE_INTERNAL_SERVICE_INFO = "use_internal_service_info"
        const val PREF_TRUST_COOKIES = "cookie_trust"
        const val PREF_IMAGE_QUALITY_SETTINGS = "image_quality_setting"
        const val PREF_WTU_ANDROID_ID = "wtu_android_id"
        const val PREF_USERNAME = "username"
        const val PREF_IS_LOGGED = "is_logged"
        const val PREF_PASSWORD = "password"


        override fun createSources(context: Context) =
            listOf(Webtoon(context, "en"))

        override val name: String = "Webtoons Internal Extension"
        override val lang: String = "all"
        override val isNsfw: Boolean = false
        override val icon: Int = R.mipmap.ic_webtoon_source
    }
    private inline fun <reified T> get(url: String): T {
        try {
            return with (json)  {client.newCall(GET(url, headers)).execute().parseAs<ServiceResponse<T>>().getResult()}
        } catch (e: JsonDecodingException) {
            throw IOException("Failed to decode: $url", e)
        }

    }

    private inline fun <reified T> post(url: String, body: RequestBody): T {
        return with (json)  {client.newCall(POST(url, headers, body)).execute().parseAs<ServiceResponse<T>>().getResult()}
    }

    private fun TitleHome.toSManga(
        urlOverride: String = baseUrl + (if (this@toSManga.title.downloadable!=null) "/titleHomeMainV3?titleNo=" else "/challengeTitleHomeMainV2?titleNo=") +this@toSManga.title.titleNo) = SManga.create().apply {
        url = urlOverride
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
    private fun TitleItemStub.toSManga(urlSectionOverride: Boolean = this@toSManga.contentType=="WEBTOON"): SManga {
        return SManga.create().apply {
            this@apply.url = if (urlSectionOverride) "$baseUrl/titleHomeMainV3?titleNo=$titleNo" else "$baseUrl/challengeTitleHomeMainV2?titleNo=$titleNo"
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
            url = getChapterUrl(titleNo)
            name = episodeTitle
            chapter_number = episodeNo.toFloat()
            date_upload = exposureYmdt?:0L
            scanlator = exposureType
        }
    }
    private fun TitleEpisodeStub.getChapterUrl(titleNo: Long): String {
        if (!exposureType.isNullOrEmpty()&&productId.isNullOrEmpty()) return WebtoonAPI.ChallengeViewerEpisodeV1(titleNo, episodeNo).getFullUrl()
        if (isLogged()) return WebtoonAPI.TitleEpisodeWithLogin(titleNo, episodeNo).getFullUrl()
        if (productId.isNullOrEmpty()) return WebtoonAPI.TitleEpisode(titleNo, episodeNo).getFullUrl()


        return ""
    }

    fun getRsaEncryptedPassword(userId: String, pwd: String, key: RSAKey): String {
        val rsa = KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(key.nValue.toBigInteger(16),key.eValue.toBigInteger(16)))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        val toEnc = key.sessionKey.length.toChar() + key.sessionKey +
            "${userId.length.toChar()}$userId" + "${pwd.length.toChar()}$pwd"

        cipher.init(Cipher.ENCRYPT_MODE, rsa)
        return cipher.doFinal(toEnc.toByteArray()).toHexString()
    }

    private fun androidx.preference.PreferenceScreen.editTextPreference(key: String, title: String, default: String, summary: String, isPassword: Boolean = false, refreshSummary: Boolean = false): EditTextPreference = EditTextPreference(context).apply {
        this.key = key
        this.title = title
        this.summary = summary
        this.setDefaultValue(default)
        setOnBindEditTextListener { it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI }

        if (isPassword) {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }



        setOnPreferenceChangeListener { _, newValue ->
            try {
                val newString = newValue.toString()
                val res = preferences.edit().putString(this.key, newString).commit()

                if (refreshSummary) {
                    this.apply {
                        this.summary = newValue as String
                    }
                }
                if (isPassword) {
                    CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName(key)).launch {
                        Looper.prepare()
                        try {
                            if (login(getUsername(), newValue.toString(), null)) {
                                Toast.makeText(context, "Login Success", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e("Webtoon", "Login Error", e)
                            Toast.makeText(context, "Login Error.$e", Toast.LENGTH_LONG).show()
                        }

                    }
                } else {
                    Toast.makeText(context, "Restart app to apply new setting.", Toast.LENGTH_LONG).show()
                }

                res
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
