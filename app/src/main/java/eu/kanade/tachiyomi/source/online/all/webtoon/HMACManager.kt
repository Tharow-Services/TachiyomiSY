package eu.kanade.tachiyomi.source.online.all.webtoon

import okhttp3.HttpUrl
import okhttp3.Request
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HMACManager(
    val apiKey: String = API_KEY,
    val mac: Mac = getMACForKey(apiKey)
) {
    fun getMACEncode(builder: HttpUrl.Builder): HttpUrl.Builder {
        val time = System.currentTimeMillis().toString()

        //builder.addQueryParameter("msgpad", time)

        //    builder.addQueryParameter("md", URLEncoder.encode(synchronized(mac) {
    //        HMACSubstitution.substitution(mac.doFinal(this.toString().subString255(time).toByteArray()))
    //    }, "utf-8"))
        return builder
    }



    companion object {
        fun String.subString255(input: String) = (this.substring(0, input.length.coerceAtMost(255)) + input)
        fun getMACForKey(key: String = API_KEY): Mac {
            return WebtoonSecretKey.getMACForKey(key)
        }
        public const val API_KEY = "gUtPzJFZch4ZyAGviiyH94P99lQ3pFdRTwpJWDlSGFfwgpr6ses5ALOxWHOIT7R1"
    }
}
