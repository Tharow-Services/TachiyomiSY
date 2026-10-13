package eu.kanade.tachiyomi.source.online.all.webtoon.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import kotlinx.serialization.json.JsonNames

@Serializable
data class TitleHome(
    val point: PointMessage? = null,
    @JsonNames("challengeTitle")
    val title: Title,
    val tag: TitleTags,
    val superLike: SuperLike? = null,
    @JsonNames("challengeEpisodeMeta")
    val episodeMeta: TitleEpisodeMeta,
    @JsonNames("challengeAuthorPatreon")
    val authorPatreon: TitleAuthor? = null,
    val highlight: Highlight? = null,
    val product: TitleHomeProduct? = null,
    val rewardAd: TitleRewardAd? = null,
    val notice: TitleNotice? = null
)

@Serializable
data class TitleNotice(
    val type: String,
    val text: String,
    val linkUrl: String,
) {

}

@Serializable
data class PointMessage(
    val pointMessage: String
)

@Serializable
data class SuperLike(
    val show: Boolean,
    val totalSuperLikeCount: Long
)

@Serializable
data class ViewerEpisode(
    val regionalContentRatingMap: Map<String, String>? = null,
    val challengeEpisode: ChallengeEpisode,
    val superLikeEnabled: Boolean,
    val adExposure: AdExposure,
    val rewardAdExposureDays: Long,
    val rewardAdTitle: Boolean,
    val sourceTitle: String,
    val readingTitle: String,
    val titleThumbnailUrl: String,
    val representGenreCode: String, //TODO create enum for genre code
    val restTerminationStatus: String, //TODO create enum for rest termination status
    val titleHomePcWebUrl: String,
    val firstEpisodeViewerPcWebUrl: String,
    @JsonNames("challengeAuthorPatreon")
    val authorPatreon: TitleAuthor? = null
)
@Serializable
data class Title(
    val titleNo: Long,
    @JsonNames("readingTitle")
    val title: String,
    val language: String? = null,
    val sourceLanguage: String? = language, //TODO Create lang enum
    val readingLanguage: String? = language,
    val source: Boolean? = null,
    val viewer: String? = null, //TODO create viewer enum
    val synopsis: String? = null,
    val sourceSynopsis: String? = synopsis,
    val readingSynopsis: String? = synopsis,
    val restTerminationStatus: String, //TODO read rest Status
    val representGenre: RepresentGenre,
    @JsonNames("posterThumbnailUrl")
    val thumbnailUrl: String,
    val instagramShareImageUrl: String? = null,
    val blockUnsuitableForChildren: Boolean? = null,
    val author: TitleAuthor? = null,
    val contentRating: String? = null,
    val authorList: List<TitleAuthor>? = null,
    val previewAvailable: Boolean,
    val mature: Boolean? = null,
    val downloadable: Boolean? = null, //lol it is now
    val lastEpisodeRegisterYmdt: Long? = null,
    val likeitCount: Long? = null,
    val regionalContentRatingMap: Map<String, String>? = null,
    val adExposure: AdExposure? = null,
)
@Serializable
data class TitleRewardAd(
    val rewardAdTitle: Boolean
)

@Serializable
data class TitleEpisodeMeta(
    val pcWebLinkUrl: String? = null,
    val titleHomePcWebUrl: String,
    val totalEpisodeCount: Long,
    val firstEpisodeNo: Long,
    val firstEpisodeViewerPcWebUrl: String,
    val firstEpisodeVideoAvailable: Boolean? = null
)

@Serializable @JsonIgnoreUnknownKeys
data class TitleTags(
    val unknown: String? = null
)

@Serializable
data class TagListItem(
    val text: String,
    val type: String,
    val keywordNo: Int? = null
)

@Serializable
data class Highlight(
    val highlightList: List<HighlightItem>
)
@Serializable
data class HighlightItem(
    val mediaType: String,
    val coverImageUrl: String,
    val imageUrlList: List<String>? = null,
    val videoId: String?  = null,
    val scaleType: String,
)

@Serializable
data class RepresentGenre(
    val code: String, //TODO create genre enum
    val displayName: String
)


@Serializable
data class AdExposure(
    val status: String, //TODO ad exposure enum
    val exposure: Boolean
)

@Serializable
data class ChallengeEpisode(
    val sourceLanguage: String,
    val readingLanguage: String,
    val supportedReadingLanguages: List<String>,
    val episodeSeq: Long,
    val sourceEpisodeTitle: String,
    val readingEpisodeTitle: String,
    val thumbnailImageUrl: String,
    val linkUrl: String,
    val imageList: List<TitleEpisodeImageInfo>,
    val creatorNote: String? = null,
    val nextEpisode: ChallengeNextPrevEpisode? = null,
    @JsonNames("previousEpisode")
    val prevEpisode: ChallengeNextPrevEpisode? = null,
    val exposureType: String,
    val writingAuthorName: String,
    val communityAuthorList: List<TitleAuthor>,
    val catchUpInfo: TitleEpisodeCatchUpInfo,
)
@Serializable
data class TitleEpisodeCatchUpInfo(
    val showCatchUpNotification: Boolean,
    val remainingEpisodeCount: Long,
)

@Serializable
data class TitleAuthor(
    @JsonNames("userName")
    val authorName: String? = null,
    @JsonNames("communityId", "userId")
    val id: String,
    val profileImageUrl: String? = null,
    val authorType: String? = null, //TODO create author Type enum
    val follow: Boolean = false,
    val pushAlarm: Boolean = false,
    val patronUrl: String? = null,
    val usePatronage: Boolean? = null,
)

@Serializable
data class ChallengeNextPrevEpisode(
    val episodeNo: Long,
    val readingEpisodeTitle: String,
    val thumbnailImageUrl: String,
    val exposureType: String
)

@Serializable
data class TitleEpisodeImageInfo(
    val sortOrder: Long,
    val cutId: Short? = null,
    val url: String,
    val fileSize: Long? = null,
    val width: Long,
    val height: Long,
)

@Serializable
data class TitleEpisodeList(
    @JsonNames("challengeEpisodeList")
    val episodeList: List<TitleEpisodeStub>
)



@Serializable
data class TitleEpisodeStub(
    val episodeNo: Long,
    @JsonNames("readingEpisodeTitle")
    val episodeTitle: String,
    val episodeThumbnail: String,
    val exposureYmdt: Long? = null,
    val displayUp: Boolean,
    val hasBgm: Boolean? = null,
    val videoAvailable: Boolean? = null,
    val rewardAdEpisode: Boolean? = null,
    val timeDealEpisode: Boolean? = null,
    val productId: String? = null,
    val exposureType: String? = productId,
    val basicProductSaleUnit: TitleEpisodeProductSaleUnit? = null,
    val premiumBenefitProductSaleUnitList: List<TitleEpisodeProductSaleUnit>? = null
)
@Serializable
data class TitleEpisodeProductSaleUnit(
    val productSaleUnitType: String,
    val policyPrice: Int,
    val policyCostPrice: Int,
    val discounted: Boolean? = null,
    val freeConversionYmdt: Long? = null
)

@Serializable
data class TitleHomeProduct(
    val titleBMType: String,
    val settlementWuids: List<TitleHomeProductSettlement>
)

@Serializable
data class TitleHomeProductSettlement(
    val wcsSettlementType: String,
    val wuid: String,
)

@Serializable
data class TitleEpisodeInfoHolder(
    val episodeInfo: TitleEpisodeInfo,
    val superLikeEnabled: Boolean
)

@Serializable @JsonIgnoreUnknownKeys
data class TitleEpisodeInfo(
    val episodeSeq: Long,
    val episodeTitle: String,
    val thumbnailImageUrl: String,
    val exposureYmdt: Long,
    val count: Int,
    val imageInfo: List<TitleEpisodeImageInfo>,
    val linkUrl: String,
    val priorityViewingType: String,
    val likeItCount: Long,
    val likeIt: Boolean,
    val bgmPlaySortOrder: Byte,
    val serviceStatus: String,
    val titleServiceStatus: String,
    val writingAuthorName: String,
    val pictureAuthorName: String,
    val communityAuthorList: List<TitleAuthor>,
    val creatorNote: String? = null,
    val weekday: String? = null,
    val adExposure: AdExposure,
    val catchUpInfo: TitleEpisodeCatchUpInfo,
    val product: Boolean,
    val dailyPass: Boolean? = null,
    val rewardAd: Boolean? = null,
    val timeDeal: Boolean? = null,
    val previewProduct: Boolean? = null,
    val completeProduct: Boolean? = null,
    val nextEpisodeProduct: Boolean? = null,
    val nextEpisodePreviewProduct: Boolean? = null,
    val nextEpisodeCompleteProduct: Boolean? = null,
    val nextEpisodeDailyPass: Boolean? = null,
    val nextEpisodePassUseRestrictEpisode: Boolean? = null,
    val nextEpisodeRewardAd: Boolean? = null,
    val nextEpisodeTimeDeal: Boolean? = null,
    val nextEpisodeNo: Long? = null,
    val nextEpisodeTitle: String? = null,
    val nextEpisodeViewingType: String? = null,
    val nextEpisodeThumbnailUrl: String? = null,
    val previousEpisodeProduct: Boolean? = null,
    val previousEpisodePreviewProduct: Boolean? = null,
    val previousEpisodeCompleteProduct: Boolean? = null,
    val previousEpisodeDailyPass: Boolean? = null,
    val previousEpisodePassUseRestrictEpisode: Boolean? = null,
    val previousEpisodeRewardAd: Boolean? = null,
    val previousEpisodeTimeDeal: Boolean? = null,
    val previousEpisodeNo: Long? = null,
    val previousEpisodeTitle: String? = null,
    val previousEpisodeViewingType: String?  = null,
)
