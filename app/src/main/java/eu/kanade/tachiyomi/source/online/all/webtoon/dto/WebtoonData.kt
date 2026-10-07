package eu.kanade.tachiyomi.source.online.all.webtoon.dto

import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
data class ServiceResponse<T> (
    val message: ServiceMessage<T>
) {
    fun getResult(): T {
        if (message.result==null) {
            throw IOException(message.message)
        }
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

@Serializable
data class SearchV2Result(
    val webtoonSearch: WebtoonSearch,
    val challengeSearch: ChallengeSearch,
    val shortDramaSearch: WebtoonSearch,
    val keywords: List<KeywordsSearch>

)

@Serializable
data class ChallengeSearch(
    val query: String,
    val start: Int,
    val display: Int,
    val hasMore: Boolean,
    val challengeTitleList: List<ChallengeTitleListStub>
)

@Serializable
data class ChallengeTitleListStub(
    val titleNo: Long,
    val thumbnailImageUrl: String,
    val readingTitle: String,
    val readCount: Long,
    val writingAuthorName: String,
)

@Serializable
data class WebtoonSearch(
    val query: String,
    val start: Int,
    val display: Int,
    val hasMore: Boolean,
    val titleList: List<WebtoonTitleStub>
)

@Serializable
data class WebtoonTitleStub(
    val titleNo: Long,
    val thumbnailUrl: String,
    val readCount: Long,
    val mature: Boolean,
    val blockUnsuitableForChildren: Boolean,
    val contentType: String
)

@Serializable
data class KeywordsSearch(
    val keywordNo: Long,
    val keyword: String,
    val titleList: List<WebtoonTitleStub>
)
