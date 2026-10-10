package eu.kanade.tachiyomi.source.online.all.webtoon.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import java.io.IOException

annotation class ApiLocation(val url: String)

@Serializable
data class ServiceResponse<T> (
    val message: ServiceMessage<T>? = null,
    val errorMessage: String? = null,
    val errorCode: Int? = null,
) {
    fun getResult(): T {
        if (message==null) throw IOException(errorMessage)
        if (message.result==null) throw IOException(message.message)
        return message.result
    }
}

@Serializable
data class ServiceMessage<T>(
    val type: String,
    val service: String,
    val version: String,
    val code: Long? = null,
    val message: String? = null,
    val result: T? = null
)

@Serializable @ApiLocation($$"/searchAllV2?query=$query&startIndex=$page&pageSize=20")
data class SearchV2Result(
    val webtoonSearch: TitleSearch,
    val challengeSearch: TitleSearch,
    val shortDramaSearch: TitleSearch,
    val keywords: List<KeywordsSearch>
) {
    @Serializable
    data class KeywordsSearch(val keywordNo: Long, val keyword: String, val titleList: List<TitleItemStub>)
}


@Serializable
data class TitleSearch(
    val query: String,
    val start: Int,
    val display: Int,
    val hasMore: Boolean,
    @JsonNames("challengeTitleList")
    val titleList: List<TitleItemStub>
)

@Serializable
data class TitleItemStub(
    val titleNo: Long,
    @JsonNames("readingTitle")
    val title: String? = null,
    @JsonNames("thumbnail", "thumbnailUrl")
    val thumbnailImageUrl: String,
    val thumbnailVertical: String? = null,
    val posterThumbnail: String? = null,
    @SerialName("likeitCount")
    val likeItCount: Long? = null,
    val webtoonType: String? = null, //TODO create webtoonType Enum
    val starScoreAverage: Double? = null,
    val ageGradeNotice: Boolean? = null,
    val readCount: Long? = null,
    val unsuitableForChildren: Boolean? = null,
    val genre: String? = null,
    val genreDisplayName: String? = null,
    val synopsis: String? = null,
    val writingAuthorName: String? = null,
    val mature: Boolean? = null,
    val blockUnsuitableForChildren: Boolean? = null,
    val contentType: String? = null,
    val rankInfo: RankInfo? = null
)

@Serializable
data class RankInfo(
    val currentRank: Short,
    val changeAmount: Short,
    val status: String
)

@Serializable @ApiLocation("getPopularRankingTitleList?count=10 and trendingChartTitles.json?count=10")
data class PopularTrendingTitleList(
    val modelVersion: String? = null,
    val titleList: List<TitleItemStub>
)

