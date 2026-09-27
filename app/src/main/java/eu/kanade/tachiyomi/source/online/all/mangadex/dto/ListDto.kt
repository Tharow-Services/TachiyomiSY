package eu.kanade.tachiyomi.source.online.all.mangadex.dto

import eu.kanade.tachiyomi.source.online.all.mangadex.MDConstants
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias ListDto = ResponseDto<ListDataDto>

@Serializable
@SerialName(MDConstants.LIST)
data class ListDataDto(override val attributes: ListAttributesDto? = null) : EntityDto()

@Serializable
data class ListAttributesDto(
    val name: String,
    val visibility: String,
    val version: Int,
) : AttributesDto()
