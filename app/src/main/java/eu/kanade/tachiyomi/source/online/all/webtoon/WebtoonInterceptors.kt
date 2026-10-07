package eu.kanade.tachiyomi.source.online.all.webtoon

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.IOException

object HMACInterceptor: Interceptor {
    private val macManager = HMACManager()
    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.host!="global.apis.naver.com") return chain.proceed(chain.request())
        val builder = chain.request().newBuilder()
        try {
            builder.url(WebtoonSecretKey.getMACEncoded(macManager.mac, chain.request().url.toString(), System.currentTimeMillis().toString()))
        } catch (ex : Exception) {
            throw IOException(ex)
        }
        return chain.proceed(builder.build())
    }
}

class PlatformInterceptor(val language: String, val locale: String, val stripSizeHeader: Boolean = false): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder: Request.Builder = chain.request().newBuilder()
        val urlBuilder: HttpUrl.Builder = chain.request().url.newBuilder()
        when (chain.request().url.host) {
            "global.apis.naver.com" -> {
                urlBuilder.addQueryParameter("serviceZone", SERVICE_ZONE)
                .addQueryParameter("platform", PLATFORM)
                .addQueryParameter("language", language)
                .addQueryParameter("locale", locale)
                if (!chain.request().url.queryParameterNames.contains("v")) { // v is standard as 1 except for rare cases check if already set
                    urlBuilder.addQueryParameter("v", "1")
                }
            }
            "webtoon-phinf.pstatic.net" -> {
                requestBuilder.header("referer", REFERER)
                if (stripSizeHeader) {urlBuilder.removeAllQueryParameters("type")}
            }
        }


        return chain.proceed(requestBuilder.url(urlBuilder.build()).build())
    }

    companion object {
        const val REFERER: String = "https://m.webtoons.com"
        const val SERVICE_ZONE: String = "GLOBAL"
        const val PLATFORM: String = "APP_ANDROID"
    }
}
