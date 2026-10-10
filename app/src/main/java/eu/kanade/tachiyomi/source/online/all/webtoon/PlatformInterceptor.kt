package eu.kanade.tachiyomi.source.online.all.webtoon

import eu.kanade.tachiyomi.source.online.all.webtoon.Webtoon.ImageQualitySetting.HIGH
import eu.kanade.tachiyomi.source.online.all.webtoon.Webtoon.ImageQualitySetting.NORMAL
import eu.kanade.tachiyomi.source.online.all.webtoon.Webtoon.ImageQualitySetting.REMOVE
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class PlatformInterceptor(val self: Webtoon): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder: Request.Builder = chain.request().newBuilder()
        val urlBuilder: HttpUrl.Builder = chain.request().url.newBuilder()
        when (chain.request().url.host) {
            "global.apis.naver.com" -> {
                urlBuilder.addQueryParameter("serviceZone", SERVICE_ZONE)
                .addQueryParameter("platform", PLATFORM)
                .addQueryParameter("language", self.lang)
                .addQueryParameter("locale", self.locale)
                if (!chain.request().url.queryParameterNames.contains("v")) { // v is standard as 1 except for rare cases check if already set
                    urlBuilder.addQueryParameter("v", "1")
                }
            }
            "webtoon-phinf.pstatic.net" -> {
                requestBuilder.header("referer", REFERER)
                requestBuilder.removeHeader("wtu")
                when (self.imageQualitySetting) {
                    NORMAL -> urlBuilder.setQueryParameter("type", NORMAL.type)
                    HIGH -> urlBuilder.setQueryParameter("type", HIGH.type)
                    REMOVE -> urlBuilder.removeAllQueryParameters("type")
                }
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
