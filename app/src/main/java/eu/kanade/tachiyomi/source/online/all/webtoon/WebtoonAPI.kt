package eu.kanade.tachiyomi.source.online.all.webtoon

import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceInfoHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ServiceResponse
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.TitleEpisodeInfoHolder
import eu.kanade.tachiyomi.source.online.all.webtoon.dto.ViewerEpisode
import kotlinx.serialization.json.JsonDecodingException
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

object WebtoonAPI {

    class ServiceInfo(override val endpoint: String = "service_info.json"): ApiEndpoint<ServiceInfoHolder>
    class TitleEpisode(
        val titleNo: Long,
        val episodeNo: Long,
        override val endpoint: String = "episodeInfoWithLogin.json?v=4" +
            "&titleNo=$titleNo&episodeNo=$episodeNo",
    ) : ApiEndpoint<TitleEpisodeInfoHolder>

    class TitleEpisodeWithLogin(
        val titleNo: Long,
        val episodeNo: Long,
        val priorityViewingType: String = "IMAGE",
        override val endpoint: String = "episodeInfoWithLogin.json?v=4" +
            "&titleNo=$titleNo&episodeNo=$episodeNo&priorityViewingType=$priorityViewingType",
    ) : ApiEndpoint<TitleEpisodeInfoHolder>

    class ChallengeViewerEpisodeV1(
        val titleNo: Long,
        val episodeNo: Long,
        val readingRequestLanguage: String = "ENGLISH",
        override val endpoint: String = "challengeViewerEpisodeV1?v=1&titleNo=$titleNo&episodeNo=$episodeNo&readingRequestLanguage=$readingRequestLanguage"
    ) : ApiEndpoint<ViewerEpisode>

}
private sealed interface ApiEndpoint<T> {
    val method: String
        get() = "GET"
    val endpoint: String

    fun getFullUrl() = Webtoon.BASE_URL+"/"+endpoint
    fun getRequestBody(): RequestBody? = null

    fun toResponse(self: Webtoon): T {
        val builder = Request.Builder()
        builder.url(getFullUrl())
        builder.headers(self.headers)
        builder.method(method, getRequestBody())

        try {
            return with (self.json)  {self.client.newCall(builder.build()).execute().parseAs<ServiceResponse<T>>().getResult()}
        } catch (e: JsonDecodingException) {
            throw IOException("Json Encoding Fail At: $endpoint", e)
        }
    }

}
