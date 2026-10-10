package eu.kanade.tachiyomi.source.online.all.webtoon.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
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
    val nvalue: String,
    val evalue: String,
    val sessionKey: String,
)

@Serializable @ApiLocation("loginById.json") @JsonIgnoreUnknownKeys
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
    )
