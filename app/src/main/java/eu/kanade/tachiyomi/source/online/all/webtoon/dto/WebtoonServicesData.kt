package eu.kanade.tachiyomi.source.online.all.webtoon.dto

import android.content.SharedPreferences
import androidx.core.content.edit
import eu.kanade.tachiyomi.source.online.all.webtoon.Webtoon
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import okhttp3.Cookie
import kotlin.uuid.Uuid

@Serializable
data class ServiceInfoHolder(
    val serviceInfo: ServiceInfo,
    val commentTickets: List<String>
)

@Serializable
data class ServiceInfo(
    val filteredMccMap: Map<String, String>,
    val filteredCountryMap: Map<String, String>,
    val apiVersion: String,
    val imageDomain: String,
    val imageUrl: String,
    val productImageDomain: String,
    val creatorFeedUrl: String,
    val creatorHomeUrl: String,
    val joinBackgroundUrlMap: Map<String, String>,
    val hideAd: Boolean,
    val ageGateTime: Long,
    val ageGateTimeMap: Map<String, Long>,
    val remindPushSchedules: Map<String, String>,
)


@Serializable @ApiLocation("getRsaKey.json?v=2")
data class RSAKey(
    val keyName: String,
    @SerialName("nvalue")
    val nValue: String,
    @SerialName("evalue")
    val eValue: String,
    val sessionKey: String,
)

@Serializable @ApiLocation("loginById.json") @JsonIgnoreUnknownKeys //duplicate values with different names.
data class LoginById( //TODO create enum's for login info
    val id: String,
    val idNo: Uuid,
    val statCd: String,
    val loginStatus: Int,
    val svcCd: String,
    val ses: String,
    val hashedNeoId: String,
    val nickname: String,
    val isMember: Boolean,
    val updatedRevisit: Boolean,
    ) {

    constructor(pref: SharedPreferences) : this(
        pref.getString(PREFIX+"id", "")!!,
        Uuid.parse(pref.getString(PREFIX+"id_no", "")!!),
        pref.getString(PREFIX+"stat_cd", "")!!,
        pref.getInt(PREFIX+"login_status", 0),
        pref.getString(PREFIX+"svc_cd", "")!!,
        pref.getString(PREFIX+"ses", "")!!,
        pref.getString(PREFIX+"hashed_neo_id", "")!!,
        pref.getString(PREFIX+"nickname", "")!!,
        pref.getBoolean(PREFIX+"is_member", false),
        pref.getBoolean(PREFIX+"updated_revisit", false),

    )

    fun saveTo(pref: SharedPreferences) {
        pref.edit(true) {
            putBoolean(Webtoon.PREF_IS_LOGGED, true)
            putString(PREFIX+"id", id)
            putString(PREFIX+"id_no", idNo.toString())
            putString(PREFIX+"stat_cd", statCd)
            putInt(PREFIX+"login_status", loginStatus)
            putString(PREFIX+"svc_cd", svcCd)
            putString(PREFIX+"ses", ses)
            putString(PREFIX+"hashed_neo_id", hashedNeoId)
            putString(PREFIX+"nickname", nickname)
            putBoolean(PREFIX+"is_member", isMember)
            putBoolean(PREFIX+"updated_revisit", updatedRevisit)
        }
    }

    companion object { const val PREFIX: String = "login_by_id__" }
}

@Serializable
data class ImageSecureToken(
     val expirationTime: Long = 3600L,
    val cookieName: String,
    val cookieValue: String,
)

@Serializable
data class ImageSecureTokenHolder(
    val secureToken: ImageSecureToken
) {
    fun getExpirationTime(): Long {
        return secureToken.cookieValue.substringAfterLast("exp=").substringBefore("~").toLong()
    }
    fun toCookie(domain: String): Cookie {
        return Cookie.Builder().name(secureToken.cookieName).domain(domain).value(secureToken.cookieValue).expiresAt(getExpirationTime()).build()
    }
}


