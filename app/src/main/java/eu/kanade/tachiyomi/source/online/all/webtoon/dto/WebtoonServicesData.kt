package eu.kanade.tachiyomi.source.online.all.webtoon.dto

import kotlinx.serialization.Serializable

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
