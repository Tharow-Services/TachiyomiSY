package eu.kanade.tachiyomi.source.online.all.webtoon

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.security.MessageDigest

class PlatformInterceptor(val self: Webtoon): Interceptor {
    val wtu = MessageDigest.getInstance("MD5").digest(self.wtu.toByteArray()).map {
        (((it.toInt() and 255) + 255).toString(16).substring(1))
    }.reduce(String::plus)

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder: Request.Builder = chain.request().newBuilder()
        val urlBuilder: HttpUrl.Builder = chain.request().url.newBuilder()
        if (chain.request().method=="GET") {
            when (chain.request().url.host) {
                "global.apis.naver.com" -> {
                    requestBuilder.addHeader("wtu", wtu)
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
                    urlBuilder.setQueryParameter("type", self.imageQualitySetting.type)
                }
            }
        }
        if (chain.request().method=="POST") {
            requestBuilder.header("User-Agent", "Android/14 Model/motorolaedge30pro com.naver.linewebtoon/3.9.12(3091207,uid:10533) NeoIdSignInMod/0.1.12")
        }

        return chain.proceed(requestBuilder.url(urlBuilder.build()).build())
    }

    companion object {
        const val REFERER: String = "https://m.webtoons.com"
        const val SERVICE_ZONE: String = "GLOBAL"
        const val PLATFORM: String = "APP_ANDROID"
    }
}
