package eu.kanade.tachiyomi.source.online.all.webtoon.dto

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.Serializable

@Serializable
data class ChallengeTitleHolder(
    val point: PointMessage? = null,
    val challengeTitle: ChallengeTitle,
    val tag: ChallengeTag,
    val superLike: SuperLike? = null,
    val challengeEpisodeMeta: ChallengeEpisodeMeta,
    val rewardAd: ChallengeRewardAd,
)

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
data class ChallengeEpisodeHolder(
    val regionalContentRatingMap: Map<String, String>,
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
    val firstEpisodeViewerPcWebUrl: String
)
@Serializable
data class ChallengeTitle(
    val titleNo: Long,
    val sourceLanguage: String, //TODO Create lang enum
    val readingLanguage: String,
    val source: Boolean,
    val readingTitle: String,
    val sourceSynopsis: String,
    val readingSynopsis: String,
    val restTerminationStatus: String, //TODO read rest Status
    val representGenre: RepresentGenre,
    val thumbnailUrl: String,
    val author: ChallengeAuthor,
    val previewAvailable: Boolean,
    val lastEpisodeRegisterYmdt: Long,
    val likeitCount: Long,
    val challengeAuthorPatreon: ChallengeAuthorPatreon,
    val regionalContentRatingMap: Map<String, String> = mapOf(),
) {


    fun asSManga(thumbnailSource: String): SManga {
        return SManga.create().apply {
            url = "challengeTitleHomeMainV2?titleNo=$titleNo"
            title = readingTitle
            thumbnail_url = thumbnailSource+thumbnailUrl
            author = this@ChallengeTitle.author.authorName
            status = SManga.UNKNOWN
            description = sourceSynopsis
            genre = representGenre.displayName
            update_strategy = UpdateStrategy.ALWAYS_UPDATE
            initialized = true
        }
    }
}

@Serializable
data class ChallengeRewardAd(
    val rewardAdTitle: Boolean
)

@Serializable
data class ChallengeEpisodeMeta(
    val titleHomePcWebUrl: String,
    val totalEpisodeCount: Long,
    val firstEpisodeNo: Long,
    val firstEpisodeViewerPcWebUrl: String,
)

@Serializable
data class ChallengeTag(
    val tagList: List<String>
)

@Serializable
data class RepresentGenre(
    val code: String, //TODO create genre enum
    val displayName: String
)
@Serializable
data class ChallengeAuthor(
    val authorName: String,
    val communityId: String
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
    val imageList: List<ChallengeEpisodeImageListItem>,
    val creatorNote: String,
    val nextEpisode: ChallengeNextPrevEpisode? = null,
    val prevEpisode: ChallengeNextPrevEpisode? = null,
    val exposureType: String,
    val writingAuthorName: String,
    val communityAuthorList: List<ChallengeEpisodeAuthorItem>,
    val catchUpInfo: ChallengeEpisodeCatchUpInfo
) {
    fun getPages(imageDomain: String): List<Page> {
        return imageList.map { Page(index = it.sortOrder.toInt(), imageUrl = imageDomain+it.url) }
    }
}

@Serializable
data class ChallengeEpisodeCatchUpInfo(
    val showCatchUpNotification: Boolean,
    val remainingEpisodeCount: Long,
)

@Serializable
data class ChallengeEpisodeAuthorItem(
    val id: String,
    val profileImageUrl: String,
    val authorType: String, //TODO create author Type enum
    val follow: Boolean = false,
)

@Serializable
data class ChallengeAuthorPatreon(
    val patreonUrl: String,
    val usePatronage: Boolean
)

@Serializable
data class ChallengeNextPrevEpisode(
    val episodeNo: Long,
    val readingEpisodeTitle: String,
    val thumbnailImageUrl: String,
    val exposureType: String
)

@Serializable
data class ChallengeEpisodeImageListItem(
    val sortOrder: Long,
    val url: String,
    val width: Long,
    val height: Long,
)

@Serializable
data class ChallengeEpisodeListHolder(
    val challengeEpisodeList: List<ChallengeEpisodeItem>
)

@Serializable
data class ChallengeEpisodeItem(
    val episodeNo: Long,
    val readingEpisodeTitle: String,
    val episodeThumbnail: String,
    val exposureYmdt: Long,
    val displayUp: Boolean,
    val exposureType: String
) {
    fun asChapter(chapterUrl: String): SChapter {
        return SChapter.create().apply {
            url = chapterUrl + episodeNo.toString()
            name = readingEpisodeTitle
            chapter_number = episodeNo.toFloat()
            date_upload = exposureYmdt
            scanlator = exposureType
        }
    }
}
