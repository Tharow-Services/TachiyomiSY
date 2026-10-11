package eu.kanade.tachiyomi.source.online.all.webtoon

import okhttp3.Interceptor
import okhttp3.Response
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import javax.crypto.Mac
import kotlin.io.encoding.Base64

class HMACManager (
    key: String,
): Interceptor {
    private val mac: Mac = HmacUtils.getInitializedMac(HmacAlgorithms.HMAC_SHA_1, key.toByteArray())
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        if (url.host != "global.apis.naver.com") return chain.proceed(chain.request())
        val urlBuilder = url.newBuilder()
        val time = System.currentTimeMillis().toString()
        urlBuilder.setQueryParameter("msgpad", time)
        val urlToProcess = url.toString().substring(0, url.toString().length.coerceAtMost(255)) + time
        urlBuilder.setQueryParameter("md", Base64.encode(mac.doFinal(urlToProcess.toByteArray())))

        return chain.proceed(chain.request().newBuilder().url(urlBuilder.build()).build())
    }
}
